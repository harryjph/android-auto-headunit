package ca.yyx.hu;

import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 31/05/2016.
 */

public class VideoTestActivity extends SurfaceActivity {


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        super.surfaceChanged(holder, format, width, height);
        startVideoTest();
    }

    private void startVideoTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    videoTestRun();
                } catch (IOException e) {
                    Utils.loge(e);
                }
            }
        }, "run_vs").start();
    }

    private void videoTestRun() throws IOException {
        InputStream stream = getResources().openRawResource(R.raw.husam_h264);
        byte[] ba = Utils.toByteArray(stream);             // Read entire file, up to 16 MB to byte array ba
        stream.close();

        int size = ba.length;
        int left = size;
        int idx;
        int max_chunk_size = 65536 * 4;//16384;

        int chunk_size = max_chunk_size;
        int after;
        for (idx = 0; idx < size && left > 0; idx = after) {

            after = mVideoDecoder.h264_after_get(ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
            if (after == -1 && left <= max_chunk_size) {
                after = size;
                //hu_uti.logd ("Last chunk  chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
            } else if (after <= 0 || after > size) {
                Utils.loge("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
                return;
            }

            chunk_size = after - idx;

            byte[] bc = new byte[chunk_size];                               // Create byte array bc to hold chunk
            int ctr;
            for (ctr = 0; ctr < chunk_size; ctr++) {
                bc[ctr] = ba[idx + ctr];                                      // Copy chunk_size bytes from byte array ba at idx to byte array bc
            }

            idx += chunk_size;
            left -= chunk_size;

            mVideoDecoder.decode(bc, chunk_size);                                                // Decode audio or H264 video content
            Utils.ms_sleep(20);                                             // Wait a frame
        }
    }
}
