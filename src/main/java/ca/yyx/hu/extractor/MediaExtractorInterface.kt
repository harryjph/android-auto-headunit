package ca.yyx.hu.extractor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri

import java.io.IOException
import java.nio.ByteBuffer

/**
 * @author algavris
 * *
 * @date 30/04/2016.
 */
interface MediaExtractorInterface {
    fun readSampleData(buffer: ByteBuffer, offset: Int): Int

    fun getSampleCryptoInfo(cryptoInfo: MediaCodec.CryptoInfo)

    fun release()

    fun setDataSource(content: ByteArray, width: Int, height: Int)

    @Throws(IOException::class)
    fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>)

    val trackCount: Int

    fun unselectTrack(index: Int)

    fun getTrackFormat(index: Int): MediaFormat

    fun selectTrack(index: Int)

    val sampleFlags: Int

    val sampleTime: Long

    fun advance()
}
