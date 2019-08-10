package info.anodsplace.headunit.aap

import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.media.AudioManager
import android.os.*
import android.util.SparseIntArray
import android.view.KeyEvent
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.messages.*
import info.anodsplace.headunit.aap.protocol.proto.Input
import info.anodsplace.headunit.aap.protocol.proto.Sensors
import info.anodsplace.headunit.connection.AccessoryConnection
import info.anodsplace.headunit.contract.ProjectionActivityRequest
import info.anodsplace.headunit.decoder.AudioDecoder
import info.anodsplace.headunit.decoder.MicRecorder
import info.anodsplace.headunit.decoder.VideoDecoder
import info.anodsplace.headunit.main.BackgroundNotification
import info.anodsplace.headunit.utils.*
import java.util.*

class AapTransport(
        audioDecoder: AudioDecoder,
        videoDecoder: VideoDecoder,
        audioManager: AudioManager,
        private val settings: Settings,
        private val notification: BackgroundNotification,
        private val context: Context)
    : Handler.Callback, MicRecorder.Listener {

    private val aapAudio: AapAudio
    private val aapVideo: AapVideo
    private val pollThread: HandlerThread = HandlerThread("AapTransport:Handler", Process.THREAD_PRIORITY_AUDIO)
    private val micRecorder: MicRecorder = MicRecorder(settings.micSampleRate)
    private val sessionIds = SparseIntArray(4)
    private val startedSensors = HashSet<Int>(4)
    private val ssl = AapSslNative()
    private val keyCodes = settings.keyCodes.entries.associateTo(mutableMapOf()) {
        it.value to it.key
    }
    private val modeManager: UiModeManager =  context.getSystemService(UI_MODE_SERVICE) as UiModeManager
    private var connection: AccessoryConnection? = null
    private var aapRead: AapRead? = null
    private var handler: Handler? = null

    val isAlive: Boolean
        get() = pollThread.isAlive

    init {
        micRecorder.listener = this
        aapAudio = AapAudio(audioDecoder, audioManager)
        aapVideo = AapVideo(videoDecoder)
    }

    internal fun startSensor(type: Int) {
        startedSensors.add(type)
        if (type == Sensors.SensorType.NIGHT_VALUE) {
            send(NightModeEvent(false))
        }
    }

    override fun handleMessage(msg: Message): Boolean {

        if (msg.what == MSG_SEND) {
            val size = msg.arg2
            this.sendEncryptedMessage(msg.obj as ByteArray, size)
            return true
        }

        if (msg.what == MSG_POLL) {
            val ret = aapRead?.read() ?: -1
            if (handler == null) {
                return false
            }
            handler?.let {
                if (!it.hasMessages(MSG_POLL))
                {
                    it.sendEmptyMessage(MSG_POLL)
                }
            }

            if (ret < 0) {
                this.quit()
            }
        }

        return true
    }

    private fun sendEncryptedMessage(data: ByteArray, length: Int): Int {
        val ba = ssl.encrypt(AapMessage.HEADER_SIZE, length - AapMessage.HEADER_SIZE, data) ?: return -1

        ba.data[0] = data[0]
        ba.data[1] = data[1]
        Utils.intToBytes(ba.limit - AapMessage.HEADER_SIZE, 2, ba.data)

        val size = connection!!.send(ba.data, ba.limit, 250)

        if (AppLog.LOG_VERBOSE) {
            AppLog.v("Sent size: %d", size)
            AapDump.logvHex("US", 0, ba.data, ba.limit)
        }
        return 0
    }

    internal fun quit() {
        micRecorder.listener = null
        pollThread.quit()
        aapRead = null
        handler = null
    }

    internal fun start(connection: AccessoryConnection): Boolean {
        AppLog.i("Start Aap transport for $connection")

        if (!handshake(connection)) {
            AppLog.e("Handshake failed")
            return false
        }

        this.connection = connection

        aapRead = AapRead.Factory.create(connection, this, micRecorder, aapAudio, aapVideo, settings, notification)

        pollThread.start()
        handler = Handler(pollThread.looper, this)
        handler!!.sendEmptyMessage(MSG_POLL)
        // Create and start Transport Thread
        return true
    }

    private fun handshake(connection: AccessoryConnection): Boolean {
        val buffer = ByteArray(Messages.DEF_BUFFER_LENGTH)

        // Version request

        val version = Messages.createRawMessage(0, 3, 1, Messages.VERSION_REQUEST, Messages.VERSION_REQUEST.size) // Version Request
        var ret = connection.send(version, version.size, 1000)
        if (ret < 0) {
            AppLog.e("Version request sendEncrypted ret: $ret")
            return false
        }

        ret = connection.recv(buffer, buffer.size, 1000)
        if (ret <= 0) {
            AppLog.e("Version request recv ret: $ret")
            return false
        }
        AppLog.i("Version response recv ret: %d", ret)

        // SSL
        ret = ssl.prepare()
        if (ret < 0) {
            AppLog.e("SSL prepare failed: $ret")
            return false
        }

        var hs_ctr = 0
        // SSL_is_init_finished (hu_ssl_ssl)

        while (hs_ctr++ < 2) {
            ssl.handshake()
            val ba = ssl.bioRead() ?: return false

            val bio = Messages.createRawMessage(Channel.ID_CTR, 3, 3, ba.data, ba.limit)
            var size = connection.send(bio, bio.size, 1000)
            AppLog.i("SSL BIO sent: %d", size)

            size = connection.recv(buffer, buffer.size, 1000)
            AppLog.i("SSL received: %d", size)
            if (size <= 0) {
                AppLog.i("SSL receive error")
                return false
            }

            ret = ssl.bioWrite(6, size - 6, buffer)
            AppLog.i("SSL BIO write: %d", ret)
        }

        // Status = OK
        // byte ac_buf [] = {0, 3, 0, 4, 0, 4, 8, 0};
        val status = Messages.createRawMessage(0, 3, 4, byteArrayOf(8, 0), 2)
        ret = connection.send(status, status.size, 1000)
        if (ret < 0) {
            AppLog.e("Status request sendEncrypted ret: $ret")
            return false
        }

        AppLog.i("Status OK sent: %d", ret)

        return true
    }

    fun send(keyCode: Int, isPress: Boolean) {
        val mapped = keyCodes[keyCode] ?: keyCode
        val aapKeyCode = KeyCode.convert(mapped)

        if (mapped == KeyEvent.KEYCODE_GUIDE) {
            // Hack for navigation button to simulate touch
            val action = if (isPress) Input.TouchEvent.PointerAction.TOUCH_ACTION_DOWN else Input.TouchEvent.PointerAction.TOUCH_ACTION_UP
            this.send(TouchEvent(SystemClock.elapsedRealtime(), action, 99, 444))
            return
        }

        if (mapped == KeyEvent.KEYCODE_N) {
            val enabled = modeManager.nightMode != UiModeManager.MODE_NIGHT_YES
            send(NightModeEvent(enabled))
            modeManager.nightMode = if (enabled) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            return
        }

        if (aapKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
            AppLog.i("Unknown: $keyCode")
        }

        val ts = SystemClock.elapsedRealtime()
        if (aapKeyCode == KeyEvent.KEYCODE_SOFT_LEFT|| aapKeyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            if (isPress) {
                val delta = if (aapKeyCode == KeyEvent.KEYCODE_SOFT_LEFT) -1 else 1
                send(ScrollWheelEvent(ts, delta))
            }
            return
        }

        send(KeyCodeEvent(ts, aapKeyCode, isPress))
    }

    fun send(sensor: SensorEvent): Boolean {
        return if (startedSensors.contains(sensor.sensorType)) {
            send(sensor as AapMessage)
            true
        } else {
            AppLog.e("Sensor " + sensor.sensorType + " is not started yet")
            false
        }
    }

    fun send(message: AapMessage) {
        if (handler == null) {
            AppLog.e("Handler is null")
        } else {
            if (AppLog.LOG_VERBOSE) {
                AppLog.v(message.toString())
            }
            val msg = handler!!.obtainMessage(MSG_SEND, 0, message.size, message.data)
            handler!!.sendMessage(msg)
        }
    }

    internal fun gainVideoFocus() {
        context.sendBroadcast(ProjectionActivityRequest())
    }

    internal fun sendMediaAck(channel: Int) {
        send(MediaAck(channel, sessionIds.get(channel)))
    }

    internal fun setSessionId(channel: Int, sessionId: Int) {
        sessionIds.put(channel, sessionId)
    }

    override fun onMicDataAvailable(mic_buf: ByteArray, mic_audio_len: Int) {
        if (mic_audio_len > 64) {  // If we read at least 64 bytes of audio data
            val length = mic_audio_len + 10
            val data = ByteArray(length)
            data[0] = Channel.ID_MIC.toByte()
            data[1] = 0x0b
            Utils.put_time(2, data, SystemClock.elapsedRealtime())
            System.arraycopy(mic_buf, 0, data, 10, mic_audio_len)
            send(AapMessage(Channel.ID_MIC, 0x0b.toByte(), -1, 2, length, data))
        }
    }

    companion object {
        private const val MSG_POLL = 1
        private const val MSG_SEND = 2
    }

}

