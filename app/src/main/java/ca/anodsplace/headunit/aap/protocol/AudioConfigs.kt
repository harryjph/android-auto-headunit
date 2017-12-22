package ca.anodsplace.headunit.aap.protocol

import android.media.AudioManager
import android.util.SparseArray
import ca.anodsplace.headunit.aap.protocol.nano.Media

import ca.anodsplace.headunit.decoder.AudioDecoder

/**
 * @author algavris
 * *
 * @date 25/11/2016.
 */

object AudioConfigs {
    private val mAudioTracks = SparseArray<Media.AudioConfiguration>(3)

    fun stream(channel: Int) : Int
    {
//        when(channel) {
//            Channel.ID_AUD -> return AudioManager.STREAM_MUSIC
//            Channel.ID_AU1 -> return AudioManager.STREAM_SYSTEM
//            Channel.ID_AU2 -> return AudioManager.STREAM_VOICE_CALL
//        }
        return AudioManager.STREAM_MUSIC
    }

    fun get(channel: Int): Media.AudioConfiguration {
        return mAudioTracks.get(channel)
    }

    init {
        val audioConfig0 = Media.AudioConfiguration()
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48
        audioConfig0.numberOfBits = 16
        audioConfig0.numberOfChannels = 2
        mAudioTracks.put(Channel.ID_AUD, audioConfig0)

        val audioConfig1 = Media.AudioConfiguration()
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
        audioConfig1.numberOfBits = 16
        audioConfig1.numberOfChannels = 1
        mAudioTracks.put(Channel.ID_AU1, audioConfig1)

        val audioConfig2 = Media.AudioConfiguration()
        audioConfig2.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
        audioConfig2.numberOfBits = 16
        audioConfig2.numberOfChannels = 1
        mAudioTracks.put(Channel.ID_AU2, audioConfig2)
    }
}
