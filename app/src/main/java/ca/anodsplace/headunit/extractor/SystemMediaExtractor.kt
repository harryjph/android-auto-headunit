package ca.anodsplace.headunit.extractor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

import java.io.IOException
import java.nio.ByteBuffer

/**
 * @author algavris
 * *
 * @date 30/04/2016.
 */
class SystemMediaExtractor : MediaExtractorInterface {
    private val mExtractor = MediaExtractor()

    override fun readSampleData(buffer: ByteBuffer, offset: Int): Int {
        return mExtractor.readSampleData(buffer, offset)
    }

    override fun getSampleCryptoInfo(cryptoInfo: MediaCodec.CryptoInfo) {
        mExtractor.getSampleCryptoInfo(cryptoInfo)
    }

    override fun release() {
        mExtractor.release()
    }

    override fun setDataSource(content: ByteArray, width: Int, height: Int) {

    }

    @Throws(IOException::class)
    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>) {
        mExtractor.setDataSource(context, uri, headers)
    }

    override val trackCount: Int
        get() = mExtractor.trackCount

    override fun unselectTrack(index: Int) {
        mExtractor.unselectTrack(index)
    }

    override fun getTrackFormat(index: Int): MediaFormat {
        return mExtractor.getTrackFormat(index)
    }

    override fun selectTrack(index: Int) {
        mExtractor.selectTrack(index)
    }

    override val sampleFlags: Int
        get() = mExtractor.sampleFlags

    override val sampleTime: Long
        get() = mExtractor.sampleTime

    override fun advance() {
        mExtractor.advance()
    }
}
