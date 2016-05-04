package ca.yyx.hu;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ca.yyx.hu.decoder.MediaCodecWrapper;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.extractor.MediaExtractorInterface;
import ca.yyx.hu.extractor.StreamVideoExtractor;
import ca.yyx.hu.extractor.SystemMediaExtractor;


public class VideoTestActivity extends Activity implements TextureView.SurfaceTextureListener {

    public static final String RES_FILE = ContentResolver.SCHEME_ANDROID_RESOURCE + "://ca.yyx.hu/raw/husam_h264";
    private static String[] sFiles = {
            RES_FILE,
            "/sdcard/Download/husam.h264",
            "/sdcard/Download/husam.mp4",
            "/sdcard/Download/husam.mp4",
            "/sdcard/Download/husam.mp4",
            "/sdcard/Download/husam.mp4",
    };

    private static String[] sFileTitles = {
            RES_FILE,
            "husam.h264 (Stream)",
            "husam.mp4 (MediaCodec)",
            "husam.mp4 (MediaPlayer)",
            "husam.mp4 (VideoView)",
            "husam.mp4 (SurfaceView)",
    };

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int BIT_RATE = 2000000;            // 2Mbps
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private String h264Filename = null;
    private VideoDecoder mVideoDecoder;

    private TextureView mTextureView;
    private VideoView mVideoView;
    private SurfaceView mSurfaceView;

    private TimeAnimator mTimeAnimator = new TimeAnimator();

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private MediaCodecWrapper mCodecWrapper;
    private MediaExtractorInterface mExtractor;

    private int mWidth;
    private int mHeight;
    private AlertDialog mDialog;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        mTextureView = (TextureView) findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(this);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurfaceHolder = holder;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurfaceHolder = holder;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mSurfaceHolder = null;
            }
        });
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mVideoView = (VideoView) findViewById(R.id.video);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDialog = new AlertDialog.Builder(this)
                .setTitle("Choose a file")
                .setItems(sFileTitles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        try {
                            onFileSelected(sFiles[which], which);
                        } catch (IOException e) {
                            showMessageAndFinish(e);
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();

        mDialog.show();

        int count = MediaCodecList.getCodecCount();
        Utils.logd("Available codecs:");
        for (int i = 0; i < count; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            Utils.logd("Codec: " + info.getName() + ", Supported types: ");
            String[] types = info.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(types[j]);
                Utils.logd("     [%s]", types[j]);
            }
        }

    }

    private void onFileSelected(String filePath, int which) throws IOException {
        Uri uri = null;

        if (filePath.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
            uri = Uri.parse(filePath);
        } else {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(VideoTestActivity.this, "File does not exist", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            uri = Uri.fromFile(file);
        }

        switch (which)
        {
            case 0:
                List<String> path = uri.getPathSegments();
                int resId = getResources().getIdentifier(path.get(1), path.get(0), uri.getAuthority());
                InputStream stream = getResources().openRawResource(resId);
                present(stream);
                break;
            case 1:
                FileInputStream fstream = new FileInputStream(new File(filePath));
                present(fstream);
                break;
            case 2:
                present(uri);
                break;
            case 3:
                presentWithMediaPlayer(uri);
                break;
            case 4:
                presentWithVideoView(uri);
                break;
            case 5:
                presentWithSurfaceView(uri);
                break;
        }

    }

    private void presentWithSurfaceView(Uri uri) throws IOException {
        mSurfaceView.setVisibility(View.VISIBLE);
        mTextureView.setVisibility(View.GONE);
        mVideoView.setVisibility(View.GONE);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(this, uri);
        mMediaPlayer.setDisplay(mSurfaceHolder);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mSurfaceView.getHolder().setFixedSize(mp.getVideoWidth(), mp.getVideoHeight());
            }
        });
        mMediaPlayer.prepare();
        mMediaPlayer.start();
    }

    private void presentWithVideoView(Uri uri) {
        mSurfaceView.setVisibility(View.GONE);
        mTextureView.setVisibility(View.GONE);
        mVideoView.setVisibility(View.VISIBLE);

        mVideoView.setVideoURI(uri);
        mVideoView.start();
    }

    private void presentWithMediaPlayer(Uri uri) throws IOException {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(this, uri);
        mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
        mMediaPlayer.prepare();
        mMediaPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }

        if (mCodecWrapper != null) {
            mCodecWrapper.stopAndRelease();
        }

        if (mExtractor != null) {
            mExtractor.release();
        }

        if (mDialog != null) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
            mDialog = null;
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Utils.logd("width: " + width + "  height: " + height);  // N9: width: 2048  height: 1253
        mWidth = width;
        mHeight = height;
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
        mSurfaceView.setVisibility(View.GONE);
        mTextureView.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.GONE);

        // BEGIN_INCLUDE(initialize_extractor)
        int nTracks = mExtractor.getTrackCount();

        // Begin by unselecting all of the tracks in the extractor, so we won't see
        // any tracks that we haven't explicitly selected.
        for (int i = 0; i < nTracks; ++i) {
            mExtractor.unselectTrack(i);
        }

        for (int i = 0; i < nTracks; ++i) {

            mCodecWrapper = MediaCodecWrapper.fromVideoFormat(mExtractor.getTrackFormat(i),
                    new Surface(mTextureView.getSurfaceTexture()));
            if (mCodecWrapper != null) {
                mExtractor.selectTrack(i);
                break;
            }
        }
        assert mCodecWrapper != null;
        Utils.logd("Decoder format: " + mCodecWrapper.getTrackFormat().toString());


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

    private void showMessageAndFinish(Throwable e) {
        Toast.makeText(VideoTestActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        Utils.loge(e);
        finish();
    }


    /**
     * Creates a MediaFormat with the basic set of values.
     */
    private static MediaFormat createMediaFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        return format;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int findNonSurfaceColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (colorFormat != MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                return colorFormat;
            }
        }
        Utils.loge("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }
}
