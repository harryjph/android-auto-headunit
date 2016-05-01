package ca.yyx.hu.extractor;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author algavris
 * @date 30/04/2016.
 */
public interface MediaExtractorInterface {
    int readSampleData(ByteBuffer buffer, int offset);

    void getSampleCryptoInfo(MediaCodec.CryptoInfo cryptoInfo);

    void release();

    void setDataSource(byte[] content, int width, int height);

    void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException;

    int getTrackCount();

    void unselectTrack(int index);

    MediaFormat getTrackFormat(int index);

    void selectTrack(int index);

    int getSampleFlags();

    long getSampleTime();

    void advance();
}
