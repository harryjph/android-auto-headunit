package ca.yyx.hu;

import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;

import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 31/05/2016.
 */

public class VideoTestActivity extends SurfaceActivity {
    private boolean mStarted = false;

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
                    AppLog.e(e);
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

            after = h264_after_get(ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
            if (after == -1 && left <= max_chunk_size) {
                after = size;
                //hu_uti.i ("Last chunk  chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
            } else if (after <= 0 || after > size) {
                AppLog.e("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
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

            mVideoDecoder.decode(bc, 0, chunk_size);                                                // Decode audio or H264 video content
            Utils.ms_sleep(20);                                             // Wait a frame
        }
        mStarted = true;
    }


    private int h264_after_get (byte [] ba, int idx) {
        idx += 4; // Pass 0, 0, 0, 1
        for (; idx < ba.length - 4; idx ++) {
            if (idx > 24)   // !!!! HACK !!!! else 0,0,0,1 indicates first size 21, instead of 25
                if (ba [idx] == 0 && ba [idx+1] == 0 && ba [idx+2] == 0 && ba [idx+3] == 1)
                    return (idx);
        }
        return (-1);
    }
}
