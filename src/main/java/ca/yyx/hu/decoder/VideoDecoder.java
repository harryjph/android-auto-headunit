package ca.yyx.hu.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;

import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class VideoDecoder {
    private MediaCodec mCodec;
    private MediaCodec.BufferInfo mCodecBufferInfo;
    private final static Object sLock = new Object();
    private ByteBuffer[] mInputBuffers;

    private int mHeight;
    private int mWidth;
    private SurfaceHolder mHolder;
    private NalUnitsStore mNalUnitsStore = new NalUnitsStore();
    private boolean mCodecConfigured;

    public void decode(byte[] buffer, int offset, int size) {

        synchronized (sLock) {

            // AppLog.i("Video buffer: %02X %02X %02X %02X %02X (%d)", buffer[offset], buffer[offset+1], buffer[offset+2], buffer[offset+3], buffer[offset+4], size);

            mNalUnitsStore.capture(buffer, offset, size);

            if (mCodec == null) {
                AppLog.v("Codec is not initialized");
                return;
            }

            if (!mCodecConfigured && mNalUnitsStore.isReady())
            {
                ByteBuffer content = mNalUnitsStore.getByteBuffer();

                AppLog.i("Sending SPS & IDR...");

                while (content.hasRemaining()) {

                    if (!codec_input_provide(content)) {
                        AppLog.e("Dropping content because there are no available buffers.");
                        return;
                    }

                    codec_output_consume();
                }
                mCodecConfigured = true;
            }

            if (!mCodecConfigured)
            {
                AppLog.v("Codec is not configured");
                return;
            }

            ByteBuffer content = ByteBuffer.wrap(buffer, offset, size);

            while (content.hasRemaining()) {                                 // While there is remaining content...

                if (!codec_input_provide(content)) {                          // Process buffer; if no available buffers...
                    AppLog.e("Dropping content because there are no available buffers.");
                    return;
                }

                // Send result to video codec
                codec_output_consume();
            }
        }
    }

    private void codec_init() {
        synchronized (sLock) {
            try {
                mCodec = MediaCodec.createDecoderByType("video/avc");       // Create video codec: ITU-T H.264 / ISO/IEC MPEG-4 Part 10, Advanced Video Coding (MPEG-4 AVC)
            } catch (Throwable t) {
                AppLog.e("Throwable creating video/avc decoder: " + t);
            }
            try {
                mCodecBufferInfo = new MediaCodec.BufferInfo();                         // Create Buffer Info
                MediaFormat format = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
                mCodec.configure(format, mHolder.getSurface(), null, 0);               // Configure codec for H.264 with given width and height, no crypto and no flag (ie decode)
                mCodec.start();                                             // Start codec
                mInputBuffers = mCodec.getInputBuffers();
            } catch (Throwable t) {
                AppLog.e(t);
            }
            AppLog.i("Codec started");
        }
    }

    private void codec_stop(String reason) {
        synchronized (sLock) {
            if (mCodec != null) {
                mCodec.stop();
            }
            mCodec = null;
            mInputBuffers = null;
            mCodecBufferInfo = null;
            mCodecConfigured = false;
            AppLog.i("Reason: " + reason);
        }
    }

    private boolean codec_input_provide(ByteBuffer content) {            // Called only by media_decode() with new NAL unit in Byte Buffer
        try {
            final int inputBufIndex = mCodec.dequeueInputBuffer(1000000);           // Get input buffer with 1 second timeout
            if (inputBufIndex < 0) {
                AppLog.e("dequeueInputBuffer: "+inputBufIndex);
                return false;                                                 // Done with "No buffer" error
            }

            final ByteBuffer buffer = mInputBuffers[inputBufIndex];

            final int capacity = buffer.capacity();
            buffer.clear();
            if (content.remaining() <= capacity) {                           // If we can just put() the content...
                buffer.put(content);                                           // Put the content
            } else {                                                            // Else... (Should not happen ?)
                AppLog.e("content.hasRemaining (): " + content.hasRemaining() + "  capacity: " + capacity);

                int limit = content.limit();
                content.limit(content.position() + capacity);                 // Temporarily set constrained limit
                buffer.put(content);
                content.limit(limit);                                          // Restore original limit
            }
            buffer.flip();                                                   // Flip buffer for reading

            mCodec.queueInputBuffer(inputBufIndex, 0 /* offset */, buffer.limit(), 0, 0);
            return true;                                                    // Processed
        } catch (Throwable t) {
            AppLog.e(t);
        }
        return false;                                                     // Error: exception
    }

    private void codec_output_consume() {                                // Called only by media_decode() after codec_input_provide()
        int index;
        for (;;) {                                                          // Until no more buffers...
            index = mCodec.dequeueOutputBuffer (mCodecBufferInfo, 0);        // Dequeue an output buffer but do not wait
            if (index >= 0)
                mCodec.releaseOutputBuffer (index, true /*render*/);           // Return the buffer to the codec
            else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)         // See this 1st shortly after start. API >= 21: Ignore as getOutputBuffers() deprecated
                AppLog.i("INFO_OUTPUT_BUFFERS_CHANGED");
            else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)          // See this 2nd shortly after start. Output format changed for subsequent data. See getOutputFormat()
                AppLog.i("INFO_OUTPUT_FORMAT_CHANGED");
            else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }
            else
                break;
        }
        if (index != MediaCodec.INFO_TRY_AGAIN_LATER)
            AppLog.e("index: " + index);
    }

    public void onSurfaceHolderAvailable(SurfaceHolder holder, int width, int height) {
        codec_stop("onSurfaceHolderAvailable");

        mHolder = holder;
        mWidth = width;
        mHeight = (height > 1080) ? 1080 : height;
        codec_init();
    }

    public void stop(String reason) {
        codec_stop(reason);
    }

}
