package info.anodsplace.headunit.aap.protocol

import android.media.AudioManager
import android.util.SparseArray
import info.anodsplace.headunit.aap.protocol.proto.Media

import info.anodsplace.headunit.decoder.AudioDecoder

/**
 * @author algavris
 * *
 * @date 25/11/2016.
 */

object AudioConfigs {
    private val audioTracks = SparseArray<Media.AudioConfiguration>(3)

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
        return audioTracks.get(channel)
    }

    init {
        val audioConfig0 = Media.AudioConfiguration.newBuilder().apply {
            sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48
            numberOfBits = 16
            numberOfChannels = 2
        }.build()
        audioTracks.put(Channel.ID_AUD, audioConfig0)

        val audioConfig1 = Media.AudioConfiguration.newBuilder().apply {
            sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
            numberOfBits = 16
            numberOfChannels = 1
        }.build()
        audioTracks.put(Channel.ID_AU1, audioConfig1)

        val audioConfig2 = Media.AudioConfiguration.newBuilder().apply {
            sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
            numberOfBits = 16
            numberOfChannels = 1
        }.build()
        audioTracks.put(Channel.ID_AU2, audioConfig2)
    }
}
