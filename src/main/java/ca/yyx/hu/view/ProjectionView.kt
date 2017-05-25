package ca.yyx.hu.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

import ca.yyx.hu.App
import ca.yyx.hu.decoder.VideoDecoder
import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 09/11/2016.
 */

class ProjectionView : SurfaceView, SurfaceHolder.Callback {
    private var mVideoDecoder: VideoDecoder? = null
    private var mSurfaceCallback: SurfaceHolder.Callback? = null


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    fun setSurfaceCallback(surfaceCallback: SurfaceHolder.Callback) {
        mSurfaceCallback = surfaceCallback
    }

    private fun init() {
        mVideoDecoder = App.get(context).videoDecoder()
        holder.addCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mVideoDecoder?.stop("onDetachedFromWindow")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        AppLog.i("holder " + holder)
        mSurfaceCallback?.surfaceCreated(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        AppLog.i("holder %s, format: %d, width: %d, height: %d", holder, format, width, height)
        mVideoDecoder?.onSurfaceHolderAvailable(holder, width, height)
        mSurfaceCallback?.surfaceChanged(holder, format, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        AppLog.i("holder " + holder)
        mVideoDecoder?.stop("surfaceDestroyed")
        mSurfaceCallback?.surfaceDestroyed(holder)
    }
}
