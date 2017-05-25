package ca.yyx.hu.aap

import android.media.AudioManager

import ca.yyx.hu.aap.protocol.AudioConfigs
import ca.yyx.hu.aap.protocol.nano.Protocol
import ca.yyx.hu.decoder.AudioDecoder
import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 01/10/2016.
 * *
 * *
 * @link https://github.com/google/ExoPlayer/blob/release-v2/library/src/main/java/com/google/android/exoplayer2/audio/AudioTrack.java
 */

internal class AapAudio(
        private val mAudioDecoder: AudioDecoder,
        private val mAudioManager: AudioManager) : AudioManager.OnAudioFocusChangeListener {

    init {
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    fun requestFocusChange(focusRequest: Int) {
        val stream = AudioManager.STREAM_MUSIC
        if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_RELEASE) {
            mAudioManager.abandonAudioFocus(this)
        } else if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN) {
            mAudioManager.requestAudioFocus(this, stream, AudioManager.AUDIOFOCUS_GAIN)
        } else if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN_TRANSIENT) {
            mAudioManager.requestAudioFocus(this, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        } else if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_UNKNOWN) {
            mAudioManager.requestAudioFocus(this, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    fun process(message: AapMessage): Int {
        if (message.size >= 10) {
            decode(message.channel, 10, message.data, message.size - 10)
        }

        return 0
    }

    private fun decode(channel: Int, start: Int, buf: ByteArray, len: Int) {
        var length = len
        if (length > AUDIO_BUFS_SIZE) {
            AppLog.e("Error audio len: %d  aud_buf_BUFS_SIZE: %d", length, AUDIO_BUFS_SIZE)
            length = AUDIO_BUFS_SIZE
        }

        if (mAudioDecoder.getTrack(channel) == null) {
            val config = AudioConfigs.get(channel)
            val stream = AudioManager.STREAM_MUSIC
            mAudioDecoder.start(channel, stream, config.sampleRate, config.numberOfBits, config.numberOfChannels)
        }

        mAudioDecoder.decode(channel, buf, start, length)
    }

    fun stopAudio(chan: Int) {
        AppLog.i("Audio Stop: " + chan)
        mAudioDecoder.stop(chan)
    }

    override fun onAudioFocusChange(focusChange: Int) {

        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> AppLog.i("LOSS")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AppLog.i("LOSS TRANSIENT")
            AudioManager.AUDIOFOCUS_GAIN -> AppLog.i("GAIN")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AppLog.i("LOSS TRANSIENT CAN DUCK")
        }
    }

    companion object {
        private val AUDIO_BUFS_SIZE = 65536 * 4      // Up to 256 Kbytes
    }
}

