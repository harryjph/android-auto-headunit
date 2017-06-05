package ca.yyx.hu.aap

import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import android.os.SystemClock
import android.util.SparseIntArray
import android.view.KeyEvent

import java.util.ArrayList

import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.messages.KeyCodeEvent
import ca.yyx.hu.aap.protocol.messages.MediaAck
import ca.yyx.hu.aap.protocol.messages.Messages
import ca.yyx.hu.aap.protocol.messages.NightModeEvent
import ca.yyx.hu.aap.protocol.messages.ScrollWheelEvent

import ca.yyx.hu.aap.protocol.messages.SensorEvent
import ca.yyx.hu.aap.protocol.nano.Protocol
import ca.yyx.hu.connection.AccessoryConnection
import ca.yyx.hu.decoder.AudioDecoder
import ca.yyx.hu.decoder.MicRecorder
import ca.yyx.hu.decoder.VideoDecoder
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.NightMode
import ca.yyx.hu.utils.Settings
import ca.yyx.hu.utils.Utils
import java.util.HashSet

class AapTransport(
        audioDecoder: AudioDecoder,
        videoDecoder: VideoDecoder,
        audioManager: AudioManager,
        private val mSettings: Settings,
        private val mListener: AapTransport.Listener)
    : Handler.Callback, MicRecorder.Listener {

    private val mAapAudio: AapAudio
    private val mAapVideo: AapVideo
    private val mPollThread: HandlerThread = HandlerThread("AapTransport:Handler", Process.THREAD_PRIORITY_AUDIO)
    private val mMicRecorder: MicRecorder = MicRecorder()
    private val mSessionIds = SparseIntArray(4)
    private val mStartedSensors = HashSet<Int>(4)
    private val mSsl = AapSslNative()

    private var mConnection: AccessoryConnection? = null
    private var mAapRead: AapRead? = null
    private var mHandler: Handler? = null

    internal fun startSensor(type: Int) {
        mStartedSensors.add(type)
        if (type == Protocol.SENSOR_TYPE_NIGHT) {
            Utils.ms_sleep(2)
            val nm = NightMode(mSettings)
            AppLog.i("Send night mode")
            send(NightModeEvent(nm.current))
            AppLog.i(nm.toString())
        }
    }

    interface Listener {
        fun gainVideoFocus()
    }

    init {
        mMicRecorder.setListener(this)
        mAapAudio = AapAudio(audioDecoder, audioManager)
        mAapVideo = AapVideo(videoDecoder)
    }

    val isAlive: Boolean
        get() = mPollThread.isAlive

    override fun handleMessage(msg: Message): Boolean {

        if (msg.what == MSG_SEND) {
            val size = msg.arg2
            this.sendEncryptedMessage(msg.obj as ByteArray, size)
            return true
        }

        if (msg.what == MSG_POLL) {
            val ret = mAapRead?.read() ?: -1
            if (mHandler == null) {
                return false
            }
            if (!mHandler!!.hasMessages(MSG_POLL)) {
                mHandler!!.sendEmptyMessage(MSG_POLL)
            }

            if (ret < 0) {
                this.quit()
            }
        }

        return true
    }

    private fun sendEncryptedMessage(data: ByteArray, length: Int): Int {
        val ba = mSsl.encrypt(AapMessage.HEADER_SIZE, length - AapMessage.HEADER_SIZE, data) ?: return -1

        ba.data[0] = data[0]
        ba.data[1] = data[1]
        Utils.intToBytes(ba.limit - AapMessage.HEADER_SIZE, 2, ba.data)

        val size = mConnection!!.send(ba.data, ba.limit, 250)

        if (AppLog.LOG_VERBOSE) {
            AppLog.v("Sent size: %d", size)
            AapDump.logvHex("US", 0, ba.data, ba.limit)
        }
        return 0
    }

    internal fun quit() {
        mMicRecorder.setListener(null)
        mPollThread.quit()
        mAapRead = null
        mHandler = null
    }

    internal fun connectAndStart(connection: AccessoryConnection): Boolean {
        AppLog.i("Start Aap transport for " + connection)

        if (!handshake(connection)) {
            AppLog.e("Handshake failed")
            return false
        }

        mConnection = connection

        mAapRead = AapRead.Factory.create(connection, this, mMicRecorder, mAapAudio, mAapVideo, mSettings)

        mPollThread.start()
        mHandler = Handler(mPollThread.looper, this)
        mHandler!!.sendEmptyMessage(MSG_POLL)
        // Create and start Transport Thread
        return true
    }

    private fun handshake(connection: AccessoryConnection): Boolean {
        val buffer = ByteArray(Messages.DEF_BUFFER_LENGTH)

        // Version request

        val version = Messages.createRawMessage(0, 3, 1, Messages.VERSION_REQUEST, Messages.VERSION_REQUEST.size) // Version Request
        var ret = connection.send(version, version.size, 1000)
        if (ret < 0) {
            AppLog.e("Version request sendEncrypted ret: " + ret)
            return false
        }

        ret = connection.recv(buffer, buffer.size, 1000)
        if (ret <= 0) {
            AppLog.e("Version request recv ret: " + ret)
            return false
        }
        AppLog.i("Version response recv ret: %d", ret)

        // SSL
        ret = mSsl.prepare()
        if (ret < 0) {
            AppLog.e("SSL prepare failed: " + ret)
            return false
        }

        var hs_ctr = 0
        // SSL_is_init_finished (hu_ssl_ssl)

        while (hs_ctr++ < 2) {
            mSsl.handshake()
            val ba = mSsl.bioRead() ?: return false

            val bio = Messages.createRawMessage(Channel.ID_CTR, 3, 3, ba.data, ba.limit)
            var size = connection.send(bio, bio.size, 1000)
            AppLog.i("SSL BIO sent: %d", size)

            size = connection.recv(buffer, buffer.size, 1000)
            AppLog.i("SSL received: %d", size)
            if (size <= 0) {
                AppLog.i("SSL receive error")
                return false
            }

            ret = mSsl.bioWrite(6, size - 6, buffer)
            AppLog.i("SSL BIO write: %d", ret)
        }

        // Status = OK
        // byte ac_buf [] = {0, 3, 0, 4, 0, 4, 8, 0};
        val status = Messages.createRawMessage(0, 3, 4, byteArrayOf(8, 0), 2)
        ret = connection.send(status, status.size, 1000)
        if (ret < 0) {
            AppLog.e("Status request sendEncrypted ret: " + ret)
            return false
        }

        AppLog.i("Status OK sent: %d", ret)

        return true
    }

    fun sendButton(keyCode: Int, isPress: Boolean) {
        val ts = SystemClock.elapsedRealtime()

        val aapKeyCode = KeyCode.convert(keyCode)

        if (aapKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
            AppLog.i("Unknown: " + keyCode)
        }

        if (aapKeyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (isPress) {
                val delta = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                send(ScrollWheelEvent(ts, delta))
            }
            return
        }

        send(KeyCodeEvent(ts, keyCode, isPress))
    }

    fun send(sensor: SensorEvent) {
        if (mStartedSensors.contains(sensor.type)) {
            send(sensor as AapMessage)
        } else {
            AppLog.e("Sensor " + sensor.type + " is not started yet")
        }
    }

    fun send(message: AapMessage) {
        if (mHandler == null) {
            AppLog.e("Handler is null")
        } else {
            if (AppLog.LOG_VERBOSE) {
                AppLog.v(message.toString())
            }
            val msg = mHandler!!.obtainMessage(MSG_SEND, 0, message.size, message.data)
            mHandler!!.sendMessage(msg)
        }
    }

    internal fun gainVideoFocus() {
        mListener.gainVideoFocus()
    }

    internal fun sendMediaAck(channel: Int) {
        send(MediaAck(channel, mSessionIds.get(channel)))
    }

    internal fun setSessionId(channel: Int, sessionId: Int) {
        mSessionIds.put(channel, sessionId)
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
        private val MSG_POLL = 1
        private val MSG_SEND = 2
    }

}

