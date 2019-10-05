package info.anodsplace.headunit.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.SurfaceHolder

import java.nio.ByteBuffer

import info.anodsplace.headunit.utils.AppLog
class VideoDecoder {
    private var mCodec: MediaCodec? = null
    private var mCodecBufferInfo: MediaCodec.BufferInfo? = null
    private var mInputBuffers: Array<ByteBuffer>? = null

    private var mHeight: Int = 0
    private var mWidth: Int = 0
    private var mHolder: SurfaceHolder? = null
    private var mCodecConfigured: Boolean = false

    fun decode(buffer: ByteArray, offset: Int, size: Int) {
        synchronized(sLock) {
            if (mCodec == null) {
                AppLog.d { "Codec is not initialized" }
                return
            }

            if (!mCodecConfigured && isSps(buffer, offset)) {
                AppLog.i { "Got SPS sequence..." }
                mCodecConfigured = true
            }

            if (!mCodecConfigured) {
                AppLog.d { "Codec is not configured" }
                return
            }

            val content = ByteBuffer.wrap(buffer, offset, size)

            while (content.hasRemaining()) {
                if (!codec_input_provide(content)) {
                    AppLog.e { "Dropping content because there are no available buffers." }
                    return
                }
                codecOutputConsume()
            }
        }
    }

    private fun codec_init() {
        synchronized(sLock) {
            try {
                mCodec = MediaCodec.createDecoderByType("video/avc")       // Create video codec: ITU-T H.264 / ISO/IEC MPEG-4 Part 10, Advanced Video Coding (MPEG-4 AVC)
            } catch (t: Throwable) {
                AppLog.e { "Throwable creating video/avc decoder: $t" }
            }

            try {
                mCodecBufferInfo = MediaCodec.BufferInfo()                         // Create Buffer Info
                val format = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight)
                mCodec!!.configure(format, mHolder!!.surface, null, 0)               // Configure codec for H.264 with given width and height, no crypto and no flag (ie decode)
                mCodec!!.start()                                             // Start codec
                mInputBuffers = mCodec!!.inputBuffers
            } catch (e: Exception) {
                AppLog.e(e)
            }

            AppLog.i { "Codec started" }
        }
    }

    private fun codec_stop(reason: String) {
        synchronized(sLock) {
            if (mCodec != null) {
                mCodec!!.stop()
            }
            mCodec = null
            mInputBuffers = null
            mCodecBufferInfo = null
            mCodecConfigured = false
            AppLog.i { "Reason: $reason" }
        }
    }

    private fun codec_input_provide(content: ByteBuffer): Boolean {            // Called only by media_decode() with new NAL unit in Byte Buffer
        try {
            val inputBufIndex = mCodec!!.dequeueInputBuffer(1000000)           // Get input buffer with 1 second timeout
            if (inputBufIndex < 0) {
                AppLog.e { "dequeueInputBuffer: $inputBufIndex" }
                return false                                                 // Done with "No buffer" error
            }

            val buffer = mInputBuffers!![inputBufIndex]

            val capacity = buffer.capacity()
            buffer.clear()
            if (content.remaining() <= capacity) {                           // If we can just put() the content...
                buffer.put(content)                                           // Put the content
            } else {                                                            // Else... (Should not happen ?)
                AppLog.e { "content.hasRemaining (): " + content.hasRemaining() + "  capacity: " + capacity }

                val limit = content.limit()
                content.limit(content.position() + capacity)                 // Temporarily set constrained limit
                buffer.put(content)
                content.limit(limit)                                          // Restore original limit
            }
            buffer.flip()                                                   // Flip buffer for reading

            mCodec!!.queueInputBuffer(inputBufIndex, 0 /* offset */, buffer.limit(), 0, 0)
            return true                                                    // Processed
        } catch (e: Exception) {
            AppLog.e(e)
        }

        return false                                                     // Error: exception
    }

    private fun codecOutputConsume() {                                // Called only by media_decode() after codec_input_provide()
        var index: Int
        while (true) {                                                          // Until no more buffers...
            index = mCodec!!.dequeueOutputBuffer(mCodecBufferInfo!!, 0)        // Dequeue an output buffer but do not wait
            if (index >= 0)
                mCodec!!.releaseOutputBuffer(index, true /*render*/)           // Return the buffer to the codec
            else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
            // See this 1st shortly after start. API >= 21: Ignore as getOutputBuffers() deprecated
                AppLog.i { "INFO_OUTPUT_BUFFERS_CHANGED" }
            else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            // See this 2nd shortly after start. Output format changed for subsequent data. See getOutputFormat()
                AppLog.i { "INFO_OUTPUT_FORMAT_CHANGED" }
            else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else
                break
        }
        if (index != MediaCodec.INFO_TRY_AGAIN_LATER)
            AppLog.e { "index: $index" }
    }

    fun onSurfaceHolderAvailable(holder: SurfaceHolder, width: Int, height: Int) {
        synchronized(sLock) {
            if (mCodec != null) {
                AppLog.i { "Codec is running" }
                return
            }
        }

        mHolder = holder
        mWidth = width
        mHeight = if (height > 1080) 1080 else height
        codec_init()
    }

    fun stop(reason: String) {
        codec_stop(reason)
    }

    companion object {
        private val sLock = Object()

        // For NAL units having nal_unit_type equal to 7 or 8 (indicating
        // a sequence parameter set or a picture parameter set,
        // respectively)
        private fun isSps(ba: ByteArray, offset: Int): Boolean {
            return getNalType(ba, offset) == 7
        }

        private fun getNalType(ba: ByteArray, offset: Int): Int {
            // nal_unit_type
            // ba[4] == 0x67
            // +---------------+
            // |0|1|1|0|0|1|1|1|
            // +-+-+-+-+-+-+-+-+
            // |F|NRI|  Type   |
            // +---------------+
            return ba[offset + 4] and 0x1f
        }
    }
}

private infix fun Byte.and(i: Int): Int = this.toInt() and i
