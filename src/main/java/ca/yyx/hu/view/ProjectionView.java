package ca.yyx.hu.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import ca.yyx.hu.App;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 09/11/2016.
 */

public class ProjectionView extends SurfaceView implements SurfaceHolder.Callback {
    protected VideoDecoder mVideoDecoder;
    private  SurfaceHolder.Callback mSurfaceCallback;


    public ProjectionView(Context context) {
        super(context);
        init();
    }

    public ProjectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProjectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setSurfaceCallback(SurfaceHolder.Callback surfaceCallback) {
        mSurfaceCallback = surfaceCallback;
    }

    private void init() {
        mVideoDecoder = App.get(getContext()).videoDecoder();
        getHolder().addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVideoDecoder.stop("onDetachedFromWindow");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        AppLog.i("holder " + holder);
        if (mSurfaceCallback != null) {
            mSurfaceCallback.surfaceCreated(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        AppLog.i("holder %s, format: %d, width: %d, height: %d", holder, format, width, height);
        mVideoDecoder.onSurfaceHolderAvailable(holder, width, height);
        if (mSurfaceCallback != null) {
            mSurfaceCallback.surfaceChanged(holder, format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        AppLog.i("holder " + holder);
        mVideoDecoder.stop("surfaceDestroyed");
        if (mSurfaceCallback != null) {
            mSurfaceCallback.surfaceDestroyed(holder);
        }
    }
}
