package ru.ytkab0bp.beamklipper

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import androidx.multidex.MultiDexApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.ytkab0bp.beamklipper.db.BeamDB
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager
import ru.ytkab0bp.beamklipper.utils.Prefs
import ru.ytkab0bp.eventbus.EventBus
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class KlipperApp : MultiDexApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            val apkFile = File(applicationInfo.sourceDir)
            val loader = javaClass.classLoader ?: return
            val secondaryBytes = readSecondaryDexBytes(apkFile) ?: return

            val inmemLoader = dalvik.system.InMemoryDexClassLoader(
                java.nio.ByteBuffer.allocateDirect(secondaryBytes.size).put(secondaryBytes).also { it.flip() },
                loader
            )

            var pathListClass: Class<*> = loader::class.java
            while (pathListClass != null && pathListClass.declaredFields.none { it.name == "pathList" }) {
                pathListClass = pathListClass.superclass
            }
            val pathListField = pathListClass!!.getDeclaredField("pathList")
            pathListField.isAccessible = true
            val pathList = pathListField.get(loader)

            var localPathListClass: Class<*> = inmemLoader::class.java
            while (localPathListClass != null && localPathListClass.declaredFields.none { it.name == "pathList" }) {
                localPathListClass = localPathListClass.superclass
            }
            val localPathListField = localPathListClass!!.getDeclaredField("pathList")
            localPathListField.isAccessible = true
            val localPathList = localPathListField.get(inmemLoader)
            val localElementsField = localPathList.javaClass.getDeclaredField("dexElements")
            localElementsField.isAccessible = true
            val localElements = localElementsField.get(localPathList) as Array<*>

            val existingElementsField = pathList.javaClass.getDeclaredField("dexElements")
            existingElementsField.isAccessible = true
            val existingElements = existingElementsField.get(pathList) as Array<*>

            val elementType = existingElements.javaClass.componentType
            val combined = java.lang.reflect.Array.newInstance(elementType, existingElements.size + localElements.size)
            System.arraycopy(existingElements, 0, combined, 0, existingElements.size)
            System.arraycopy(localElements, 0, combined, existingElements.size, localElements.size)
            existingElementsField.set(pathList, combined)
        } catch (e: Exception) {
            android.util.Log.w("KlipperApp", "Failed secondary DEX install", e)
        }
    }

    private fun readSecondaryDexBytes(apk: File): ByteArray? {
        val zipFile = java.util.zip.ZipFile(apk)
        try {
            val entry = zipFile.getEntry("classes2.dex") ?: return null
            return zipFile.getInputStream(entry).use { it.readBytes() }
        } finally {
            zipFile.close()
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        Prefs.init(this)
        DATABASE = BeamDB(this)
        EventBus.registerImpl(this)
        Prefs.applyAppTheme()
        Prefs.applyAppLanguage()

        hasUpdateInfo = try {
            assets.open("update.json").close()
            true
        } catch (_: java.io.IOException) {
            false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(SERVICES_CHANNEL, getString(R.string.ServicesChannel), NotificationManager.IMPORTANCE_LOW))
        }

        val isMainProcess = getProcessNameCompat() == packageName

        bundleInstallJob = appScope.async(Dispatchers.IO) {
            BundleInstaller.init(this@KlipperApp)
            if (isMainProcess) {
                seedChaquopyDirLocked(this@KlipperApp)
            }
        }

        if (!isMainProcess) {
            runBlocking { bundleInstallJob.await() }
            waitForChaquopySeed()
        }

        if (isMainProcess) {
            appScope.launch {
                runBlocking { bundleInstallJob.await() }
                waitForChaquopySeed()
                Log.i("beam_app", "BundleInstaller+seed done, loading instances from DB")
                val instances = withContext(Dispatchers.IO) {
                    DATABASE.getInstances()
                }
                Log.i("beam_app", "DB.getInstances() returned ${instances.size} rows")
                KlipperInstance.onInstancesLoadedFromDB(instances)
            }
            appScope.launch(Dispatchers.IO) {
                UsbSerialManager.init(this@KlipperApp)
            }
        }
    }

    private fun waitForChaquopySeed() {
        val marker = File(filesDir, CHAQUOPY_SEED_MARKER)
        var attempts = 0
        while (!marker.exists() && attempts < 200) {
            try { Thread.sleep(50) } catch (_: InterruptedException) { break }
            attempts++
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getProcessNameCompat(): String {
        if (Build.VERSION.SDK_INT >= 28) return Application.getProcessName()
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val method = activityThread.getDeclaredMethod("currentProcessName")
            method.invoke(null) as String
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private const val CHAQUOPY_SEED_MARKER = ".chaquopy_seed_v1"
        private const val CHAQUOPY_LOCK_NAME = ".chaquopy_lock"

        fun withChaquopyLock(ctx: Context, action: () -> Unit) {
            val lockFile = File(ctx.filesDir, CHAQUOPY_LOCK_NAME)
            var raf: RandomAccessFile? = null
            var lock: FileLock? = null
            try {
                raf = RandomAccessFile(lockFile, "rw")
                lock = raf.channel.lock()
                action()
            } finally {
                try { lock?.release() } catch (_: Throwable) {}
                try { raf?.close() } catch (_: Throwable) {}
            }
        }

        fun seedChaquopyDirLocked(ctx: Context) {
            withChaquopyLock(ctx) {
                val marker = File(ctx.filesDir, CHAQUOPY_SEED_MARKER)
                if (marker.exists()) return@withChaquopyLock
                try {
                    val platform = AndroidPlatform(ctx)
                    platform.path
                    try {
                        if (!Python.isStarted()) {
                            Python.start(platform)
                        }
                    } catch (_: IllegalStateException) {
                    } catch (_: Throwable) {
                        try { File(ctx.filesDir, "chaquopy").deleteRecursively() } catch (_: Throwable) {}
                        try {
                            if (!Python.isStarted()) {
                                Python.start(AndroidPlatform(ctx))
                            }
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
                try {
                    File(ctx.filesDir, "chaquopy").mkdirs()
                    marker.createNewFile()
                } catch (_: Throwable) {}
            }
        }

        @JvmField
        val PERMISSION = BuildConfig.APPLICATION_ID + ".permission.INTERNAL_BROADCASTS"
        @JvmField
        val SERVICES_CHANNEL = "services"

        @JvmField
        val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        lateinit var bundleInstallJob: Deferred<Unit>

        lateinit var INSTANCE: KlipperApp
        lateinit var DATABASE: BeamDB
        @JvmField
        var EVENT_BUS: EventBus = EventBus.newBus("main")
        @JvmField
        var hasUpdateInfo = false
    }
}
