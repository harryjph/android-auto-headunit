package ca.yyx.hu;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import ca.yyx.hu.decoder.MediaCodecWrapper;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.extractor.MediaExtractorInterface;
import ca.yyx.hu.extractor.StreamVideoExtractor;
import ca.yyx.hu.extractor.SystemMediaExtractor;


public class VideoTestActivity extends Activity implements TextureView.SurfaceTextureListener {

    public static final String RES_FILE = ContentResolver.SCHEME_ANDROID_RESOURCE + "://ca.yyx.hu/raw/husam_h264";
    private static String[] sFiles = {
            "/sdcard/Download/husam.h264",
            "/sdcard/Download/husam.mp4",
            RES_FILE
    };

    private String h264Filename = null;
    private VideoDecoder mVideoDecoder;
    private TextureView mPlaybackView;
    private TimeAnimator mTimeAnimator = new TimeAnimator();

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private MediaCodecWrapper mCodecWrapper;
    private MediaExtractorInterface mExtractor;

    private int mWidth;
    private int mHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        mPlaybackView = (TextureView) findViewById(R.id.tv_vid);
        mVideoDecoder = new VideoDecoder(this);

        mPlaybackView.setSurfaceTextureListener(this);
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
        if (filePath.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
            Uri uri = Uri.parse(filePath);
            List<String> path = uri.getPathSegments();
            int resId = getResources().getIdentifier(path.get(1), path.get(0), uri.getAuthority());
            try {
                present(uri);
                return;
            } catch (Exception e) {
                Utils.loge("Uri: " + uri.toString(), e);
            }

            try {
                InputStream stream = getResources().openRawResource(resId);
                present(stream);
            } catch (Exception e) {
                showMessageAndFinish(e);
            }
        } else {
            File file = new File(filePath);
            Uri uri = Uri.fromFile(file);
            if (!file.exists()) {
                Toast.makeText(VideoTestActivity.this, "File does not exist", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
                present(uri);
                return;
            } catch (Exception e) {
                Utils.loge("Uri: " + uri.toString(), e);
            }

            try {
                FileInputStream stream = new FileInputStream(file);
                present(stream);
            } catch (Exception e) {
                showMessageAndFinish(e);
            }
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoDecoder.stop_record();

        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }

        if (mCodecWrapper != null) {
            mCodecWrapper.stopAndRelease();
            mExtractor.release();
        }

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Utils.logd("width: " + width + "  height: " + height);  // N9: width: 2048  height: 1253
        mWidth = width;
        mHeight = height;
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

    private void present(InputStream fis) throws IOException {
        mExtractor = new StreamVideoExtractor();
        byte[] ba; // Read entire file, up to 16 MB to byte array ba
        try {
            ba = Utils.toByteArray(fis);
        } catch (IOException e) {
            showMessageAndFinish(e);
            return;
        }
        mExtractor.setDataSource(ba, mWidth, mHeight);
        present();
    }

    private void present(Uri videoUri) throws IOException {
        mExtractor = new SystemMediaExtractor();
        try {
            mExtractor.setDataSource(this, videoUri, null);
        } catch (IOException e) {
            mExtractor.release();
            mExtractor = null;
            throw e;
        }
        present();
    }

    private void present() throws IOException {

        // BEGIN_INCLUDE(initialize_extractor)
        int nTracks = mExtractor.getTrackCount();

        // Begin by unselecting all of the tracks in the extractor, so we won't see
        // any tracks that we haven't explicitly selected.
        for (int i = 0; i < nTracks; ++i) {
            mExtractor.unselectTrack(i);
        }

        // Find the first video track in the stream. In a real-world application
        // it's possible that the stream would contain multiple tracks, but this
        // sample assumes that we just want to play the first one.
        for (int i = 0; i < nTracks; ++i) {
            // Try to create a video codec for this track. This call will return null if the
            // track is not a video track, or not a recognized video format. Once it returns
            // a valid MediaCodecWrapper, we can break out of the loop.
            mCodecWrapper = MediaCodecWrapper.fromVideoFormat(mExtractor.getTrackFormat(i),
                    new Surface(mPlaybackView.getSurfaceTexture()));
            if (mCodecWrapper != null) {
                mExtractor.selectTrack(i);
                break;
            }
        }
        // END_INCLUDE(initialize_extractor)


        // By using a {@link TimeAnimator}, we can sync our media rendering commands with
        // the system display frame rendering. The animator ticks as the {@link Choreographer}
        // recieves VSYNC events.
        mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(final TimeAnimator animation,
                                     final long totalTime,
                                     final long deltaTime) {

                boolean isEos = ((mExtractor.getSampleFlags() & MediaCodec
                        .BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                // BEGIN_INCLUDE(write_sample)
                if (!isEos) {
                    // Try to submit the sample to the codec and if successful advance the
                    // extractor to the next available sample to read.
                    boolean result = mCodecWrapper.writeSample(mExtractor, false,
                            mExtractor.getSampleTime(), mExtractor.getSampleFlags());

                    if (result) {
                        // Advancing the extractor is a blocking operation and it MUST be
                        // executed outside the main thread in real applications.
                        mExtractor.advance();
                    }
                }
                // END_INCLUDE(write_sample)

                // Examine the sample at the head of the queue to see if its ready to be
                // rendered and is not zero sized End-of-Stream record.
                MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();
                mCodecWrapper.peekSample(out_bufferInfo);

                // BEGIN_INCLUDE(render_sample)
                if (out_bufferInfo.size <= 0 && isEos) {
                    mTimeAnimator.end();
                    mCodecWrapper.stopAndRelease();
                    mExtractor.release();
                } else if (out_bufferInfo.presentationTimeUs / 1000 < totalTime) {
                    // Pop the sample off the queue and send it to {@link Surface}
                    mCodecWrapper.popSample(true);
                }
                // END_INCLUDE(render_sample)

            }
        });

        // We're all set. Kick off the animator to process buffers and render video frames as
        // they become available
        mTimeAnimator.start();
    }

//    private void present(InputStream stream) {
//
//        byte[] ba; // Read entire file, up to 16 MB to byte array ba
//        try {
//            ba = Utils.toByteArray(stream);
//        } catch (IOException e) {
//            showMessageAndFinish(e);
//            return;
//        }
//
//        mVideoDecoder.codec_init();
//
//        ByteBuffer bb;
//
//        int size = ba.length;
//        int left = size;
//        int max_chunk_size = 65536 * 4;//16384;
//
//        int chunk_size = max_chunk_size;
//        int after;
//        int idx;
//        for (idx = 0; idx < size && left > 0; idx = after) {
//
//            after = h264_after_get(ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
//            if (after == -1 && left <= max_chunk_size) {
//                after = size;
//            } else if (after <= 0 || after > size) {
//                Utils.loge("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
//                return;
//            }
//
//            chunk_size = after - idx;
//
//            byte[] bc = new byte[chunk_size];                               // Create byte array bc to hold chunk
//            for (int ctr = 0; ctr < chunk_size; ctr++) {
//                bc[ctr] = ba[idx + ctr];                                      // Copy chunk_size bytes from byte array ba at idx to byte array bc
//            }
//
//            idx += chunk_size;
//            left -= chunk_size;
//
//            bb = ByteBuffer.wrap(bc);                                        // Wrap chunk byte array bc to create byte buffer bb
//
//            mVideoDecoder.decode(bb);                                       // Decode H264 video content
//            Utils.ms_sleep(20);                                             // Wait a frame
//        }
//
//    }

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
