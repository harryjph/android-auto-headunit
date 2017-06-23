package ca.yyx.hu.decoder

import android.util.SparseArray

/**
 * @author algavris
 * *
 * @date 28/04/2016.
 */
class AudioDecoder {

    private val mAudioTracks = SparseArray<AudioTrackWrapper>(3)

    fun getTrack(channel: Int): AudioTrackWrapper? {
        return mAudioTracks.get(channel)
    }

    fun decode(channel: Int, buffer: ByteArray, offset: Int, size: Int) {
        val audioTrack = mAudioTracks.get(channel)
        audioTrack.write(buffer, offset, size)
    }

    fun stop() {
        for (i in 0..mAudioTracks.size() - 1) {
            stop(mAudioTracks.keyAt(i))
        }
    }

    fun stop(chan: Int) {
        val audioTrack = mAudioTracks.get(chan)
        audioTrack?.stop()
        mAudioTracks.put(chan, null)
    }

    fun start(channel: Int, stream: Int, sampleRate: Int, numberOfBits: Int, numberOfChannels: Int) {
        val thread = AudioTrackWrapper(stream, sampleRate, numberOfBits, numberOfChannels)
        mAudioTracks.put(channel, thread)
    }

    companion object {
        const val SAMPLE_RATE_HZ_48 = 48000
        const val SAMPLE_RATE_HZ_16 = 16000
    }
}
