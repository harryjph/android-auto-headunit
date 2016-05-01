package ca.yyx.hu.extractor;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author algavris
 * @date 30/04/2016.
 */
public class SystemMediaExtractor implements MediaExtractorInterface {
    private final MediaExtractor mExtractor = new MediaExtractor();

    @Override
    public int readSampleData(ByteBuffer buffer, int offset) {
        return mExtractor.readSampleData(buffer, offset);
    }

    @Override
    public void getSampleCryptoInfo(MediaCodec.CryptoInfo cryptoInfo) {
        mExtractor.getSampleCryptoInfo(cryptoInfo);
    }

    @Override
    public void release() {
        mExtractor.release();
    }

    @Override
    public void setDataSource(byte[] content, int width, int height) {

    }


    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
        mExtractor.setDataSource(context, uri, headers);
    }

    @Override
    public int getTrackCount() {
        return mExtractor.getTrackCount();
    }

    @Override
    public void unselectTrack(int index) {
        mExtractor.unselectTrack(index);
    }

    @Override
    public MediaFormat getTrackFormat(int index) {
        return mExtractor.getTrackFormat(index);
    }

    @Override
    public void selectTrack(int index) {
        mExtractor.selectTrack(index);
    }

    @Override
    public int getSampleFlags() {
        return mExtractor.getSampleFlags();
    }

    @Override
    public long getSampleTime() {
        return mExtractor.getSampleTime();
    }

    @Override
    public void advance() {
        mExtractor.advance();
    }
}
