package info.anodsplace.headunit.decoder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import info.anodsplace.headunit.utils.AppLog

/**
 * @author algavris
 * *
 * @date 12/05/2016.
 */
class MicRecorder(val micSampleRate: Int) {

    private var mMicAudioRecord: AudioRecord? = null

    private val micBufferSize = AudioRecord.getMinBufferSize(micSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private var micAudioBuf = ByteArray(micBufferSize)

    private var thread_mic_audio_active = false
    private var thread_mic_audio: Thread? = null
    private var mListener: Listener? = null

    interface Listener {
        fun onMicDataAvailable(mic_buf: ByteArray, mic_audio_len: Int)
    }

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    fun stop() {
        AppLog.i("thread_mic_audio: $thread_mic_audio  thread_mic_audio_active: $thread_mic_audio_active")
        if (thread_mic_audio_active) {
            thread_mic_audio_active = false
            if (thread_mic_audio != null) {
                thread_mic_audio!!.interrupt()
            }
        }

        if (mMicAudioRecord != null) {
            mMicAudioRecord!!.stop()
            mMicAudioRecord!!.release()                                     // Release AudioTrack resources
            mMicAudioRecord = null
        }
    }

    internal fun mic_audio_read(aud_buf: ByteArray, max_len: Int): Int {
        var len = 0
        if (mMicAudioRecord == null) {
            return len
        }
        len = mMicAudioRecord!!.read(aud_buf, 0, max_len)
        if (len <= 0) {
            // If no audio data...
            if (len == android.media.AudioRecord.ERROR_INVALID_OPERATION)
            // -3
                AppLog.e("get expected interruption error due to shutdown: " + len)
            return len
        }

        mListener!!.onMicDataAvailable(aud_buf, len)
        return len
    }

    fun start(): Int {
        try {
            mMicAudioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT, micSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, micBufferSize)
            mMicAudioRecord!!.startRecording()
            // Start input

            thread_mic_audio = Thread(Runnable {
                while (thread_mic_audio_active) {
                    mic_audio_read(micAudioBuf, micBufferSize)
                }
            }, "mic_audio")

            thread_mic_audio_active = true
            thread_mic_audio!!.start()
            return 0
        } catch (e: Exception) {
            AppLog.e(e)
            mMicAudioRecord = null
            return -2
        }

    }

}
