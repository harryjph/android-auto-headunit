package ca.yyx.hu.aap.protocol

import android.media.AudioManager
import android.util.SparseArray

import ca.yyx.hu.aap.protocol.nano.Protocol
import ca.yyx.hu.decoder.AudioDecoder

/**
 * @author algavris
 * *
 * @date 25/11/2016.
 */

object AudioConfigs {
    private val mAudioTracks = SparseArray<Protocol.AudioConfiguration>(3)

    fun stream(channel: Int) : Int
    {
//        when(channel) {
//            Channel.ID_AUD -> return AudioManager.STREAM_MUSIC
//            Channel.ID_AU1 -> return AudioManager.STREAM_SYSTEM
//            Channel.ID_AU2 -> return AudioManager.STREAM_VOICE_CALL
//        }
        return AudioManager.STREAM_MUSIC
    }

    fun get(channel: Int): Protocol.AudioConfiguration {
        return mAudioTracks.get(channel)
    }

    init {
        val audioConfig0 = Protocol.AudioConfiguration()
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48
        audioConfig0.numberOfBits = 16
        audioConfig0.numberOfChannels = 2
        mAudioTracks.put(Channel.ID_AUD, audioConfig0)

        val audioConfig1 = Protocol.AudioConfiguration()
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
        audioConfig1.numberOfBits = 16
        audioConfig1.numberOfChannels = 1
        mAudioTracks.put(Channel.ID_AU1, audioConfig1)

        val audioConfig2 = Protocol.AudioConfiguration()
        audioConfig2.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
        audioConfig2.numberOfBits = 16
        audioConfig2.numberOfChannels = 1
        mAudioTracks.put(Channel.ID_AU2, audioConfig2)
    }
}
