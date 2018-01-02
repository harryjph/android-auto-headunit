package info.anodsplace.headunit.decoder

import android.util.SparseArray

/**
 * @author algavris
 * *
 * @date 28/04/2016.
 */
class AudioDecoder {

    private val audioTracks = SparseArray<AudioTrackWrapper>(3)

    fun getTrack(channel: Int): AudioTrackWrapper? {
        return audioTracks.get(channel)
    }

    fun decode(channel: Int, buffer: ByteArray, offset: Int, size: Int) {
        val audioTrack = audioTracks.get(channel)
        audioTrack.write(buffer, offset, size)
    }

    fun stop() {
        for (i in 0..audioTracks.size() - 1) {
            stop(audioTracks.keyAt(i))
        }
    }

    fun stop(chan: Int) {
        val audioTrack = audioTracks.get(chan)
        audioTrack?.stop()
        audioTracks.put(chan, null)
    }

    fun start(channel: Int, stream: Int, sampleRate: Int, numberOfBits: Int, numberOfChannels: Int) {
        val thread = AudioTrackWrapper(stream, sampleRate, numberOfBits, numberOfChannels)
        audioTracks.put(channel, thread)
    }

    companion object {
        const val SAMPLE_RATE_HZ_48 = 48000
        const val SAMPLE_RATE_HZ_16 = 16000
    }
}
