package ru.ytkab0bp.beamklipper

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import ru.ytkab0bp.beamklipper.events.InstanceStateChangedEvent
import ru.ytkab0bp.beamklipper.events.InstancesRefreshedEvent
import ru.ytkab0bp.beamklipper.events.WebStateChangedEvent
import ru.ytkab0bp.beamklipper.service.*
import ru.ytkab0bp.beamklipper.utils.Prefs
import java.io.File

class KlipperInstance {
    @JvmField
    var name: String = ""
    @JvmField
    var id: String? = null
    @JvmField
    var icon: InstanceIcon = InstanceIcon.PRINTER
    @JvmField
    var autostart = false

    private var state: State = State.IDLE
    private var klippyIntent: Intent? = null
    private var klippyConnection: ServiceConnection? = null
    private var klippyConnected = false
    private var moonrakerIntent: Intent? = null
    private var moonrakerConnection: ServiceConnection? = null
    private var moonrakerConnected = false
    private var slot = 0

    fun getState(): State = state

    val directory: File
        get() = File(KlipperApp.INSTANCE.filesDir, "instance${File.separator}$id")

    val publicDirectory: File
        get() = File(directory, "public")

    fun start() {
        if (state != State.IDLE) return
        notifyStateChanged(State.STARTING)

        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Failed to create instance directory ($id)")
            stop()
            return
        }
        if (!publicDirectory.exists() && !publicDirectory.mkdirs()) {
            Log.w(TAG, "Failed to create public instance directory ($id)")
            stop()
            return
        }
        val cfg = File(publicDirectory, "config")
        if (!cfg.exists() && !cfg.mkdirs()) {
            Log.w(TAG, "Failed to create config directory ($id)")
        }
        val gcodes = File(publicDirectory, "gcodes")
        if (!gcodes.exists() && !gcodes.mkdirs()) {
            Log.w(TAG, "Failed to create gcodes directory ($id)")
        }
        val logs = File(publicDirectory, "logs")
        if (!logs.exists() && !logs.mkdirs()) {
            Log.w(TAG, "Failed to create logs directory ($id)")
        }
        val timelapses = File(publicDirectory, "timelapses")
        if (!timelapses.exists() && !timelapses.mkdirs()) {
            Log.w(TAG, "Failed to create timelapses directory ($id)")
        }

        slot = -1
        if (slots.isEmpty()) {
            slot = 0
        } else if (slots.size < SLOTS_COUNT) {
            val cl = slots.values
            for (i in 0 until SLOTS_COUNT) {
                if (!cl.contains(i)) {
                    slot = i
                    break
                }
            }
        } else {
            throw IllegalStateException("Can't start $id: out of slots")
        }
        slots[this] = slot
        val instId = id
        mainHandler.post {
            try {
                val kIntent = Intent(KlipperApp.INSTANCE, Class.forName("ru.ytkab0bp.beamklipper.service.KlippyService_$slot"))
                klippyIntent = kIntent
                kIntent.putExtra(BasePythonService.KEY_INSTANCE, instId)
                val b1 = KlipperApp.INSTANCE.bindService(kIntent, object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        Log.i("beam_service", "klippy connected!"); klippyConnected = true
                        if (moonrakerConnected) {
                            notifyStateChanged(State.RUNNING)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName) {}
                }.also { klippyConnection = it }, Context.BIND_AUTO_CREATE); Log.i("beam_service", "bind klippy: $b1")
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            }
            try {
                val mIntent = Intent(KlipperApp.INSTANCE, Class.forName("ru.ytkab0bp.beamklipper.service.MoonrakerService_$slot"))
                moonrakerIntent = mIntent
                mIntent.putExtra(BasePythonService.KEY_INSTANCE, instId)
                KlipperApp.INSTANCE.bindService(mIntent, object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        Log.i("beam_service", "moonraker connected!"); moonrakerConnected = true
                        if (klippyConnected) {
                            notifyStateChanged(State.RUNNING)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName) {}
                }.also { moonrakerConnection = it }, Context.BIND_AUTO_CREATE)
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }

    fun stop() {
        if (state != State.RUNNING && state != State.STARTING) return
        notifyStateChanged(State.STOPPING)

        mainHandler.post {
            val nm = KlipperApp.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (klippyConnection != null) {
                try { KlipperApp.INSTANCE.unbindService(klippyConnection!!) } catch (_: Throwable) {}
                try { KlipperApp.INSTANCE.stopService(klippyIntent) } catch (_: Throwable) {}
                onKlippyUnbound()
                try { nm.cancel(BaseKlippyService.BASE_ID + slot) } catch (_: Throwable) {}
            }
            if (moonrakerConnection != null) {
                try { KlipperApp.INSTANCE.unbindService(moonrakerConnection!!) } catch (_: Throwable) {}
                try { KlipperApp.INSTANCE.stopService(moonrakerIntent) } catch (_: Throwable) {}
                onMoonrakerUnbound()
                try { nm.cancel(BaseMoonrakerService.BASE_ID + slot) } catch (_: Throwable) {}
            }
        }
    }

    private fun onKlippyUnbound() {
        klippyConnection = null
        klippyConnected = false
        if (!moonrakerConnected) {
            notifyStateChanged(State.IDLE)
        }
    }

    private fun onMoonrakerUnbound() {
        moonrakerConnection = null
        moonrakerConnected = false
        if (!klippyConnected) {
            notifyStateChanged(State.IDLE)
        }
    }

    private fun notifyStateChanged(state: State) {
        this.state = state
        KlipperApp.EVENT_BUS.fireEvent(InstanceStateChangedEvent(requireNotNull(id), state))

        if (state == State.IDLE) {
            slots.remove(this)
            if (slots.isEmpty()) {
                mainHandler.post {
                    if (slots.isNotEmpty()) return@post
                    if (webServerConnection != null) {
                        KlipperApp.EVENT_BUS.fireEvent(WebStateChangedEvent(ru.ytkab0bp.beamklipper.KlipperInstance.State.STOPPING))
                        try { KlipperApp.INSTANCE.unbindService(webServerConnection!!) } catch (_: Throwable) {}
                        try { KlipperApp.INSTANCE.stopService(Intent(KlipperApp.INSTANCE, WebService::class.java)) } catch (_: Throwable) {}
                        KlipperApp.EVENT_BUS.fireEvent(WebStateChangedEvent(ru.ytkab0bp.beamklipper.KlipperInstance.State.IDLE))
                        webServerConnection = null
                    }
                    if (cameraServerConnection != null) {
                        try { KlipperApp.INSTANCE.unbindService(cameraServerConnection!!) } catch (_: Throwable) {}
                        try { KlipperApp.INSTANCE.stopService(Intent(KlipperApp.INSTANCE, CameraService::class.java)) } catch (_: Throwable) {}
                        cameraServerConnection = null
                    }
                }
            }
        } else if (state == State.RUNNING) {
            mainHandler.post {
                if (webServerConnection == null) {
                    KlipperApp.EVENT_BUS.fireEvent(WebStateChangedEvent(ru.ytkab0bp.beamklipper.KlipperInstance.State.STARTING))
                    KlipperApp.INSTANCE.bindService(Intent(KlipperApp.INSTANCE, WebService::class.java), object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            KlipperApp.EVENT_BUS.fireEvent(WebStateChangedEvent(ru.ytkab0bp.beamklipper.KlipperInstance.State.RUNNING))
                        }

                        override fun onServiceDisconnected(name: ComponentName) {}
                    }.also { webServerConnection = it }, Context.BIND_AUTO_CREATE)
                }

                if (Prefs.isCameraEnabled) {
                    if (cameraServerConnection == null) {
                        KlipperApp.INSTANCE.bindService(Intent(KlipperApp.INSTANCE, CameraService::class.java), object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, service: IBinder) {}
                            override fun onServiceDisconnected(name: ComponentName) {}
                        }.also { cameraServerConnection = it }, Context.BIND_AUTO_CREATE)
                    }
                }
            }
        }
    }

    enum class State {
        IDLE, STARTING, RUNNING, STOPPING
    }

    companion object {
        const val SLOTS_COUNT = 4
        private const val TAG = "beam_instance"

        private val mainHandler = Handler(Looper.getMainLooper())
        private val slots = HashMap<KlipperInstance, Int>()
        private var webServerConnection: ServiceConnection? = null
        private var cameraServerConnection: ServiceConnection? = null
        private var instances: List<KlipperInstance> = emptyList()
        private val instanceMap = object : HashMap<String, KlipperInstance>() {
            override fun get(key: String): KlipperInstance? {
                var inst = super.get(key)
                if (inst == null) {
                    for (i in instances) {
                        if (key == i.id) {
                            put(key, i)
                            inst = i
                            break
                        }
                    }
                }
                return inst
            }
        }

        @JvmStatic
        fun onInstancesLoadedFromDB(loaded: List<KlipperInstance>) {
            Log.i("beam_instance", "onInstancesLoadedFromDB: count=${loaded.size}")
            for (inst in loaded) {
                val was = getInstance(inst.id ?: continue)
                if (was != null) {
                    inst.state = was.state
                    inst.klippyConnection = was.klippyConnection
                    inst.klippyConnected = was.klippyConnected
                    inst.klippyIntent = was.klippyIntent
                    inst.moonrakerConnection = was.moonrakerConnection
                    inst.moonrakerConnected = was.moonrakerConnected
                    inst.moonrakerIntent = was.moonrakerIntent
                    inst.slot = was.slot
                    slots.remove(was)
                    slots[inst] = inst.slot
                }
            }
            instances = loaded
            instanceMap.clear()
            KlipperApp.EVENT_BUS.fireEvent(InstancesRefreshedEvent())

            for (inst in instances) {
                Log.i("beam_instance", "instance id=${inst.id} name=${inst.name} autostart=${inst.autostart} state=${inst.getState()}")
                if (inst.autostart && inst.getState() == State.IDLE) {
                    Log.i("beam_instance", "  -> calling start()")
                    inst.start()
                }
            }
        }

        @JvmStatic
        fun getInstance(id: String): KlipperInstance? {
            var inst = instanceMap[id]
            if (inst == null) {
                for (i in instances) {
                    if (id == i.id) {
                        instanceMap[id] = i
                        inst = i
                        break
                    }
                }
            }
            if (inst == null) {
                val db = try { KlipperApp.DATABASE } catch (_: Throwable) { return null }
                val all = try { db.getInstances() } catch (_: Throwable) { return null }
                for (i in all) {
                    if (id == i.id) {
                        inst = i
                        instanceMap[id] = i
                        if (instances.isEmpty()) {
                            instances = all
                        }
                        break
                    }
                }
            }
            return inst
        }

        @JvmStatic
        fun getInstances(): List<KlipperInstance> {
            if (instances.isEmpty()) {
                try { KlipperApp.DATABASE } catch (_: Throwable) { return emptyList() } ?: return emptyList()
                val loaded = try { KlipperApp.DATABASE.getInstances() } catch (_: Throwable) { emptyList() }
                if (loaded.isNotEmpty()) {
                    instances = loaded
                }
            }
            return instances
        }

        @JvmStatic
        fun hasFreeSlots(): Boolean = slots.size < SLOTS_COUNT

        @JvmStatic
        fun isWebServerRunning(): Boolean = webServerConnection != null

        @JvmStatic
        fun onCameraConfigChanged(enable: Boolean) {
            mainHandler.post {
                if (cameraServerConnection == null && slots.isNotEmpty() && enable) {
                    KlipperApp.INSTANCE.bindService(Intent(KlipperApp.INSTANCE, CameraService::class.java), object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {}
                        override fun onServiceDisconnected(name: ComponentName) {}
                    }.also { cameraServerConnection = it }, Context.BIND_AUTO_CREATE)
                } else if (cameraServerConnection != null && !enable) {
                    try { KlipperApp.INSTANCE.unbindService(cameraServerConnection!!) } catch (_: Throwable) {}
                    try { KlipperApp.INSTANCE.stopService(Intent(KlipperApp.INSTANCE, CameraService::class.java)) } catch (_: Throwable) {}
                    cameraServerConnection = null
                }
            }
        }
    }
}
