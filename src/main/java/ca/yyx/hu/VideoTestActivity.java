package ca.yyx.hu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import ca.yyx.hu.decoder.VideoDecoder;


public class VideoTestActivity extends Activity implements TextureView.SurfaceTextureListener {

    public static final String RES_FILE = ContentResolver.SCHEME_ANDROID_RESOURCE + "://ca.yyx.hu/raw/husam_h264";
    private static String[] sFiles = {
            "/sdcard/Download/husam.h264",
            "/sdcard/Download/husam.mp4",
            RES_FILE
    };

    private String h264Filename = null;
    private VideoDecoder mVideoDecoder;
    private TextureView mTexuterView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        mTexuterView = (TextureView) findViewById(R.id.tv_vid);
        mVideoDecoder = new VideoDecoder(this);

        mTexuterView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new AlertDialog.Builder(this)
                .setTitle("Choose a file")
                .setItems(sFiles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onFileSelected(sFiles[which]);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create().show();
    }

    private void onFileSelected(String filePath) {
        InputStream stream;
        if (filePath.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
            Uri uri = Uri.parse(filePath);
            List<String> path = uri.getPathSegments();
            int resId = getResources().getIdentifier(path.get(1), path.get(0), uri.getAuthority());
            stream = getResources().openRawResource(resId);
        } else {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(VideoTestActivity.this, "File does not exist", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
                stream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                showMessageAndFinish(e);
                return;
            }
        }
        present(stream);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoDecoder.stop_record();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Utils.logd("--- sur_tex: " + surface + "  width: " + width + "  height: " + height);  // N9: width: 2048  height: 1253
        mVideoDecoder.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    private void present(InputStream stream) {

        byte[] ba;            // Read entire file, up to 16 MB to byte array ba
        try {
            ba = Utils.toByteArray(stream);
        } catch (IOException e) {
            showMessageAndFinish(e);
            return;
        }

        ByteBuffer bb;

        int size = ba.length;
        int left = size;
        int max_chunk_size = 65536 * 4;//16384;


        int chunk_size = max_chunk_size;
        int after;
        int idx;
        for (idx = 0; idx < size && left > 0; idx = after) {

            after = h264_after_get(ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
            if (after == -1 && left <= max_chunk_size) {
                after = size;
            } else if (after <= 0 || after > size) {
                Utils.loge("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
                return;
            }

            chunk_size = after - idx;

            byte[] bc = new byte[chunk_size];                               // Create byte array bc to hold chunk
            for (int ctr = 0; ctr < chunk_size; ctr++) {
                bc[ctr] = ba[idx + ctr];                                      // Copy chunk_size bytes from byte array ba at idx to byte array bc
            }

            idx += chunk_size;
            left -= chunk_size;

            bb = ByteBuffer.wrap(bc);                                        // Wrap chunk byte array bc to create byte buffer bb

            mVideoDecoder.decode(bb);                                       // Decode H264 video content
            Utils.ms_sleep(20);                                             // Wait a frame
        }

    }

    private void showMessageAndFinish(Throwable e) {
        Toast.makeText(VideoTestActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        Utils.loge(e);
        finish();
    }

    int h264_after_get(byte[] ba, int idx) {
        idx += 4; // Pass 0, 0, 0, 1
        for (; idx < ba.length - 4; idx++) {
            if (idx > 24)   // !!!! HACK !!!! else 0,0,0,1 indicates first size 21, instead of 25
                if (ba[idx] == 0 && ba[idx + 1] == 0 && ba[idx + 2] == 0 && ba[idx + 3] == 1)
                    return (idx);
        }
        return (-1);
    }


}
