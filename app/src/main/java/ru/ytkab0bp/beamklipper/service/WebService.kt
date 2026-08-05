package ru.ytkab0bp.beamklipper.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.protocols.websockets.CloseCode
import org.nanohttpd.protocols.websockets.NanoWSD
import org.nanohttpd.protocols.websockets.OpCode
import org.nanohttpd.protocols.websockets.WebSocket
import org.nanohttpd.protocols.websockets.WebSocketFrame
import ru.ytkab0bp.beamklipper.KlipperApp
import ru.ytkab0bp.beamklipper.KlipperInstance
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.serial.KlipperProbeTable
import ru.ytkab0bp.beamklipper.serial.UsbSerialManager
import ru.ytkab0bp.beamklipper.utils.Prefs
import ru.ytkab0bp.beamklipper.utils.ViewUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

class WebService : Service() {
    companion object {
        const val PORT = 8889
        private const val ID = 300000
        private const val BEEPER_SAMPLE_RATE = 8000
        private val API_PATTERN = Pattern.compile("^/(printer|api|access|machine|server)/")
        private var mPrefs: SharedPreferences? = null
        private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ROOT)
        private val MOONRAKER_PORT_RE = Regex("port: (\\d+)")

        init {
            System.loadLibrary("beeper")
        }
    }

    private val httpServer = HttpServer()
    private var notificationManager: NotificationManager? = null
    private var beeperThread: HandlerThread? = null
    private var beeperHandler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val not = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, KlipperApp.SERVICES_CHANNEL)
        else
            Notification.Builder(this)
        not.setContentTitle(getString(R.string.WebTitle))
            .setContentText(getString(R.string.WebDescription))
            .setSmallIcon(R.drawable.icon_adaptive_foreground)
            .setOngoing(true)
        notificationManager!!.notify(ID, not.build())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID, not.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ID, not.build())
        }
        return Binder()
    }

    override fun onCreate() {
        super.onCreate()
        mPrefs = KlipperApp.INSTANCE.getSharedPreferences("web", 0)
        beeperThread = HandlerThread("beeper").also { it.start() }
        beeperHandler = Handler(beeperThread!!.looper)
        try {
            httpServer.start()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer.stop()
        beeperThread?.quit()
        beeperThread = null
        beeperHandler = null
        stopForeground(true)
        notificationManager?.cancel(ID)
    }

    private fun getMoonrakerPort(): Int {
        for (inst in KlipperInstance.getInstances()) {
            if (inst.getState() == KlipperInstance.State.RUNNING) {
                val cfg = File(inst.publicDirectory, "config/moonraker.conf")
                if (cfg.exists()) {
                    try {
                        val m = MOONRAKER_PORT_RE.find(cfg.readText())
                        if (m != null) return m.groupValues[1].toInt()
                    } catch (_: Exception) {}
                }
            }
        }
        return 7125
    }

    private external fun generateTone(numSamples: Int, freq: Float): FloatArray

    private fun playTone(duration: Int, frequency: Int) {
        val numSamples = duration * BEEPER_SAMPLE_RATE
        val buffer = generateTone(numSamples, frequency.toFloat() / BEEPER_SAMPLE_RATE)
        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioFormat(AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(BEEPER_SAMPLE_RATE)
                    .build())
                .setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setBufferSizeInBytes(2 * numSamples)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            AudioTrack(AudioManager.STREAM_MUSIC, BEEPER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT, 2 * numSamples, AudioTrack.MODE_STATIC)
        }
        track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        track.play()
        beeperHandler?.postDelayed({ track.release() }, duration.toLong())
    }

    private inner class HttpServer : NanoWSD(PORT) {
        private fun serveStatic(path: String): Response {
            val ctx = KlipperApp.INSTANCE
            val resolvedPath = if (path == "/") "/index.html" else path
            try {
                val mimeType = when {
                    resolvedPath.endsWith(".js") -> "text/javascript"
                    resolvedPath.endsWith(".html") -> "text/html"
                    resolvedPath.endsWith(".css") -> "text/css"
                    else -> "text/plain"
                }
                val prefix = Prefs.webFrontend
                val assetPath = prefix + resolvedPath
                val input = ctx.assets.open(assetPath)
                val response = Response.newChunkedResponse(Status.OK, mimeType, input)
                response.addHeader("Date", dateFormat.format(Date()))
                response.addHeader("Last-Modified", lastModifiedString)
                if (resolvedPath.endsWith(".html") || resolvedPath.endsWith(".json")) {
                    response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    response.addHeader("Pragma", "no-cache")
                    response.addHeader("Expires", "0")
                } else {
                    response.addHeader("Cache-Control", "max-age=604800, immutable")
                }
                return response
            } catch (e: IOException) {
                if (path == "/index.html" || path == "/") {
                    return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found")
                }
                return serveStatic("/index.html")
            }
        }

        private val lastModifiedString: String
            get() {
                val lastModified = mPrefs!!.getLong("last_modified", System.currentTimeMillis())
                return dateFormat.format(Date(lastModified))
            }

        private fun checkRemote(session: IHTTPSession): Boolean =
            "127.0.0.1" != session.remoteIpAddress

        override fun serve(session: IHTTPSession): Response {
            when (session.uri) {
                "/beam/arduino_reset" -> {
                    if (checkRemote(session)) return Response.newFixedLengthResponse("")
                    val serial = session.parameters["serial"]?.get(0) ?: return Response.newFixedLengthResponse("")
                    val uid = serial.substring(
                        File(KlipperApp.INSTANCE.filesDir, "serial").absolutePath.length + 1)
                    val device = UsbSerialManager.getDevice(uid)
                    if (device != null) {
                        UsbSerialManager.close(uid)
                        ViewUtils.postOnMainThread({
                            val prober = UsbSerialProber(KlipperProbeTable.getInstance())
                            val drv = prober.probeDevice(device)
                            if (drv != null) {
                                UsbSerialManager.connect(drv, UsbSerialManager.FLAG_RESET_ARDUINO)
                            }
                        }, 100)
                    }
                    return Response.newFixedLengthResponse("{\"ok\": true}")
                }
                "/beam/ffmpeg" -> {
                    if (checkRemote(session)) return Response.newFixedLengthResponse("")
                    val cmd = session.parameters["cmd"]?.get(0) ?: return Response.newFixedLengthResponse("")
                    val s = FFmpegKit.execute(cmd)
                    return Response.newFixedLengthResponse(s.output)
                }
                "/beam/play_tone" -> {
                    if (checkRemote(session)) return Response.newFixedLengthResponse("{\"ok\": false}")
                    val duration = session.parameters["duration"]?.get(0)?.toIntOrNull()
                        ?: return Response.newFixedLengthResponse("{\"ok\": false}")
                    val frequency = session.parameters["frequency"]?.get(0)?.toIntOrNull()
                        ?: return Response.newFixedLengthResponse("{\"ok\": false}")
                    playTone(duration, frequency)
                    return Response.newFixedLengthResponse("{\"ok\": true}")
                }
                "/beam/set_camera_flashlight" -> {
                    if (checkRemote(session)) return Response.newFixedLengthResponse("{\"ok\": false}")
                    val flashlight = session.parameters.containsKey("enabled") && session.parameters["enabled"]?.get(0) == "true"
                    KlipperApp.INSTANCE.sendBroadcast(
                        Intent(CameraService.ACTION_TOGGLE_FLASHLIGHT).putExtra(CameraService.KEY_FLASHLIGHT, flashlight),
                        KlipperApp.PERMISSION)
                    return Response.newFixedLengthResponse("{\"ok\": true}")
                }
                "/beam/set_camera_focus" -> {
                    if (checkRemote(session)) return Response.newFixedLengthResponse("{\"ok\": false}")
                    val autofocus = session.parameters.containsKey("autofocus") && session.parameters["autofocus"]?.get(0) == "true"
                    val distance = session.parameters["focus"]?.get(0)?.toFloatOrNull() ?: 0f
                    KlipperApp.INSTANCE.sendBroadcast(
                        Intent(CameraService.ACTION_TOGGLE_FOCUS)
                            .putExtra(CameraService.KEY_AUTOFOCUS, autofocus)
                            .putExtra(CameraService.KEY_FOCUS, distance),
                        KlipperApp.PERMISSION)
                    return Response.newFixedLengthResponse("{\"ok\": true}")
                }
            }

            val m = API_PATTERN.matcher(session.uri)
            if (m.find()) {
                try {
                    val con = URL("http://127.0.0.1:${getMoonrakerPort()}/${session.uri.substring(1)}?${session.queryParameterString}")
                        .openConnection() as HttpURLConnection
                    con.requestMethod = session.method.name
                    if (session.method == Method.POST || session.method == Method.PUT || session.method == Method.PATCH) {
                        for ((key, value) in session.headers) {
                            con.addRequestProperty(key, value)
                        }
                        con.doOutput = true
                        session.inputStream.use { input ->
                            con.outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    val responseStream = if (con.responseCode in 200..299) con.inputStream else con.errorStream
                    val r = Response.newChunkedResponse(Status.OK, con.contentType, responseStream)
                    for ((key, values) in con.headerFields) {
                        for (value in values) {
                            r.addHeader(key, value)
                        }
                    }
                    r.addHeader("Connection", "close")
                    return r
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }

            return if (session.uri.startsWith("/index.html") || session.uri == "/") {
                serveStatic("/")
            } else {
                serveStatic(session.uri)
            }
        }

        override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
            return try {
                val localRef = AtomicReference<WebSocket>()
                val remote: WebSocketClient = object : WebSocketClient(URI("ws://127.0.0.1:${getMoonrakerPort()}/websocket?${handshake.queryParameterString}")) {
                    override fun onOpen(handshakedata: ServerHandshake) {}
                    override fun onMessage(message: String) {
                        if (!localRef.get().isOpen) { close(); return }
                        try { localRef.get().send(message) } catch (e: IOException) { onError(e) }
                    }
                    override fun onMessage(bytes: ByteBuffer) {
                        try { localRef.get().send(bytes.array()) } catch (e: IOException) { onError(e) }
                    }
                    override fun onClose(code: Int, reason: String, remote: Boolean) {
                        if (!remote) {
                            try { localRef.get().close(CloseCode.NormalClosure, reason, false) } catch (e: IOException) { throw RuntimeException(e) }
                        }
                    }
                    override fun onError(ex: Exception) {
                        Log.e("websocket_proxy", "Remote socket error", ex)
                    }
                }
                val local: WebSocket = object : WebSocket(handshake) {
                    override fun onOpen() {
                        try { remote.connectBlocking() } catch (e: InterruptedException) { onException(IOException(e)) }
                    }
                    override fun onClose(code: CloseCode, reason: String, initiatedByRemote: Boolean) {
                        if (!initiatedByRemote) remote.close()
                    }
                    override fun onMessage(message: WebSocketFrame) {
                        if (!remote.isOpen) {
                            try { close(CloseCode.NormalClosure, "", false) } catch (e: IOException) { onException(e) }
                            return
                        }
                        if (message.opCode == OpCode.Text) remote.send(message.textPayload)
                        else remote.send(message.binaryPayload)
                    }
                    override fun onPong(pong: WebSocketFrame) {}
                    override fun onException(exception: IOException) {
                        if (exception is SocketTimeoutException) return
                        Log.e("websocket_proxy", "Local socket error", exception)
                    }
                }
                localRef.set(local)
                local
            } catch (e: Exception) {
                null
            }
        }
    }
}
