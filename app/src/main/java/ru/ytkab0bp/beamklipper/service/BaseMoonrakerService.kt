package ru.ytkab0bp.beamklipper.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.system.Os
import android.util.Log
import ru.ytkab0bp.beamklipper.BundleInstaller
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

open class BaseMoonrakerService(private val num: Int) : BasePythonService() {
    companion object {
        const val BASE_ID = 200000
        @JvmField val MOONRAKER_PORT_PATTERN = Pattern.compile("port: (\\d+)")

        @JvmStatic
        @Throws(IOException::class)
        fun readString(file: File): String = file.readText(StandardCharsets.UTF_8)
    }

    override fun onBind(intent: Intent?): IBinder? {
        val b = super.onBind(intent) ?: return null
        val inst = instance
        if (inst != null) {
            val not = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(this, KlipperApp.SERVICES_CHANNEL)
            else
                Notification.Builder(this)
            not.setContentTitle(getString(R.string.MoonrakerTitle, inst.name))
                .setContentText(getString(R.string.MoonrakerDescription))
                .setSmallIcon(R.drawable.icon_adaptive_foreground)
                .setOngoing(true)
            notificationManager.notify(BASE_ID + num, not.build())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(BASE_ID + num, not.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(BASE_ID + num, not.build())
            }
        }
        return b
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        notificationManager.cancel(BASE_ID + num)
    }

    override fun onStartPython() {
        val inst = instance ?: return
        try {
            val logs = File(inst.publicDirectory, "logs/moonraker.log")
            logs.parentFile?.mkdirs()
            val config = File(inst.publicDirectory, "config")
            val timelapseOutputDir = File(inst.publicDirectory, "timelapses")
            val socket = File(inst.directory, "klippy_uds")
            val tempFramesDir = File(inst.directory, "timelapse_frames")
            val moonSocket = File(inst.directory, "moonraker_uds")

            val resonancesLink = File(config, "beam_resonances")
            val fromResonances = File(KlipperApp.INSTANCE.cacheDir, "resonances")
            if (!resonancesLink.exists()) {
                config.mkdirs()
                fromResonances.mkdirs()
                try {
                    Os.symlink(fromResonances.absolutePath, resonancesLink.absolutePath)
                } catch (e: Throwable) {
                    Log.w("moonraker_$num", "symlink resonances fallback (copy instead)", e)
                }
            }

            val moonrakerCfg = File(config, "moonraker.conf")
            if (!moonrakerCfg.exists()) {
                moonrakerCfg.parentFile?.mkdirs()
                KlipperApp.withMoonrakerPortLock(this) {
                    val used = HashSet<Int>()
                    for (otherInst in KlipperInstance.getInstances()) {
                        val f = File(otherInst.publicDirectory, "config/moonraker.conf")
                        if (f.exists()) {
                            try {
                                val m = MOONRAKER_PORT_PATTERN.matcher(readString(f))
                                if (m.find()) {
                                    used.add(m.group(1).toInt())
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                    var freePort = 7125
                    while (used.contains(freePort)) {
                        freePort++
                    }
                    FileOutputStream(moonrakerCfg).use { fos ->
                        fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.assets, "moonraker/default.conf")
                            .replace("\${KLIPPY_UDS}", socket.absolutePath)
                            .replace("\${MOONRAKER_PORT}", freePort.toString())
                            .replace("\${TIMELAPSE_FRAME_PATH}", tempFramesDir.absolutePath)
                            .replace("\${TIMELAPSE_OUTPUT}", timelapseOutputDir.absolutePath)
                            .toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }
            val timelapseCfg = File(config, "timelapse.cfg")
            if (!timelapseCfg.exists()) {
                FileOutputStream(timelapseCfg).use { fos ->
                    fos.write(BundleInstaller.readString(KlipperApp.INSTANCE.assets, "moonraker/timelapse.cfg")
                        .toByteArray(StandardCharsets.UTF_8))
                }
            }
            val mrDir = File(KlipperApp.INSTANCE.filesDir, "moonraker")
            val mrBs = File(mrDir, "moonraker_bs.py")
            if (!mrDir.isDirectory) mrDir.mkdirs()
            try {
                mrBs.writeText(
                    "import os\nimport importlib.util\nimport sys\n\ndef main():\n    here = os.path.dirname(os.path.abspath(__file__))\n    sub = os.path.join(here, \"moonraker\")\n    sys.path.insert(0, sub)\n    init_py = os.path.join(sub, \"__init__.py\")\n    if os.path.isfile(init_py):\n        s1 = importlib.util.spec_from_file_location(\"moonraker\", init_py, submodule_search_locations=[sub])\n        m1 = importlib.util.module_from_spec(s1)\n        sys.modules[\"moonraker\"] = m1\n        s1.loader.exec_module(m1)\n    entry = os.path.join(sub, \"server.py\")\n    spec = importlib.util.spec_from_file_location(\"moonraker.server\", entry)\n    m = importlib.util.module_from_spec(spec)\n    sys.modules[\"moonraker.server\"] = m\n    if \"moonraker\" in sys.modules:\n        setattr(sys.modules[\"moonraker\"], \"server\", m)\n    spec.loader.exec_module(m)\n    m.main()\n",
                    StandardCharsets.UTF_8
                )
            } catch (e: Throwable) {
                Log.w("moonraker_$num", "Bootstrap write failed", e)
            }

            runPython(mrDir, "moonraker_bs", "moonraker.py", "-u", moonSocket.absolutePath, "-l", logs.absolutePath, "-d", inst.publicDirectory.absolutePath, "-c", moonrakerCfg.absolutePath)
        } catch (e: Exception) {
            Log.e("moonraker_$num", "Failed to start moonraker", e)
        }
    }

}
