package ca.yyx.hu.extractor;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Map;

import ca.yyx.hu.utils.Utils;


/**
 * @author algavris
 * @date 30/04/2016.
 */
public class StreamVideoExtractor implements MediaExtractorInterface {
    private static final byte SPS_BIT = 0x67;

    private MediaFormat mFormat;
    private byte[] mContentData;
    private int mSampleOffset = -1;
    private int mFlags;

    @Override
    public int readSampleData(ByteBuffer buffer, int offset) {
        if (mSampleOffset >= 0) {
            int nextSample = findNextNAL(mSampleOffset + 4);
            if (nextSample == -1) {
                nextSample = mContentData.length -1;
            }
            int size = nextSample - mSampleOffset;
            buffer.clear();
            buffer.put(mContentData);
            Utils.logd("readSampleData (offset: %d,next: %d,size: %d,length: %d, flags: 0x%08x)",mSampleOffset, nextSample, size, mContentData.length, mFlags);
            return size;
        }
        return 0;
    }

    @Override
    public void getSampleCryptoInfo(MediaCodec.CryptoInfo cryptoInfo) {

    }

    @Override
    public void release() {
        mContentData = null;
    }

    @Override
    public void setDataSource(byte[] content, int width, int height) {
        mContentData = content;
        mFlags = 0;
        mFormat = MediaFormat.createVideoFormat("video/avc", width, height);

        mSampleOffset = findSPS();
        if (mSampleOffset == -1) {
            throw new InvalidParameterException("Cannot find SPS in content");
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {

    }

    @Override
    public int getTrackCount() {
        return 1;
    }

    @Override
    public void unselectTrack(int index) {

    }

    @Override
    public MediaFormat getTrackFormat(int index) {
        return mFormat;
    }

    @Override
    public void selectTrack(int index) {

    }

    @Override
    public int getSampleFlags() {
        return mFlags;
    }

    @Override
    public long getSampleTime() {
        return 0;
    }

    @Override
    public void advance() {
        mFlags &= ~MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        mSampleOffset = findNextNAL(mSampleOffset + 4);
        if (mSampleOffset == -1) {
            mFlags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        }
    }

    private int findNextNAL(int offset) {
        while(offset < mContentData.length) {
            if (mContentData[offset] == 0 && mContentData[offset + 1] == 0 && mContentData[offset + 2] == 0 && mContentData[offset + 3] == 1) {
                Utils.logd("Found sequence at %d: 0x0 0x0 0x0 0x1 0x%01x (%d)",mSampleOffset, mContentData[offset + 4], mContentData[offset + 4]);
                return offset;
            }
            offset++;
        }
        return -1;
    }

    // SPS (Sequence Parameter Set) NAL Unit first
    private int findSPS() {
        int offset = 0;
        while(offset >= 0) {
            offset = findNextNAL(offset);
            if (offset == -1) {
                return -1;
            }

            if (mContentData[offset + 4] == SPS_BIT) {
                mFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
                return offset;
            }
        }
        return -1;
    }
}
