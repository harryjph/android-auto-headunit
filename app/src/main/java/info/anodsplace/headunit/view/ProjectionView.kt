package info.anodsplace.headunit.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

import info.anodsplace.headunit.App
import info.anodsplace.headunit.decoder.VideoDecoder
import info.anodsplace.headunit.utils.AppLog

/**
 * @author algavris
 * *
 * @date 09/11/2016.
 */

class ProjectionView : SurfaceView, SurfaceHolder.Callback {
    private var videoDecoder: VideoDecoder? = null
    private var surfaceCallback: SurfaceHolder.Callback? = null


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
        this.surfaceCallback = surfaceCallback
    }

    private fun init() {
        videoDecoder = App.provide(context).videoDecoder
        holder.addCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        videoDecoder?.stop("onDetachedFromWindow")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        AppLog.i("holder $holder")
        surfaceCallback?.surfaceCreated(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        AppLog.i("holder %s, format: %d, width: %d, height: %d", holder, format, width, height)
        videoDecoder?.onSurfaceHolderAvailable(holder, width, height)
        surfaceCallback?.surfaceChanged(holder, format, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        AppLog.i("holder $holder")
        videoDecoder?.stop("surfaceDestroyed")
        surfaceCallback?.surfaceDestroyed(holder)
    }
}
