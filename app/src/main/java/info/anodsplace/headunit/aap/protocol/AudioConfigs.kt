package info.anodsplace.headunit.aap.protocol

import info.anodsplace.headunit.aap.protocol.proto.Media
import info.anodsplace.headunit.decoder.AudioDecoder

val AudioConfigs = mapOf(
        // Media audio
        Channel.ID_AUD to Media.AudioConfiguration.newBuilder().apply {
            sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48
            numberOfBits = 16
            numberOfChannels = 2
        }.build(),

        // Speech audio
        Channel.ID_AU1 to Media.AudioConfiguration.newBuilder().apply {
            sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
            numberOfBits = 16
            numberOfChannels = 1
        }.build(),

        // System audio
        Channel.ID_AU2 to Media.AudioConfiguration.newBuilder().apply {
            sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16
            numberOfBits = 16
            numberOfChannels = 1
        }.build()
)
