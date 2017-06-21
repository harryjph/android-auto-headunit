package ca.yyx.hu.aap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder

import ca.yyx.hu.App
import ca.yyx.hu.R
import ca.yyx.hu.aap.protocol.messages.TouchEvent
import ca.yyx.hu.aap.protocol.messages.VideoFocusEvent
import ca.yyx.hu.app.SurfaceActivity
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.Utils
import ca.yyx.hu.view.ProjectionView

class AapProjectionActivity : SurfaceActivity(), SurfaceHolder.Callback {
    private lateinit var mProjectionView: ProjectionView

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    private val keyCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.getParcelableExtra<KeyEvent>(LocalIntent.EXTRA_EVENT)
            onKeyEvent(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLog.i("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...")

        mProjectionView = findViewById(R.id.surface) as ProjectionView
        mProjectionView.setSurfaceCallback(this)
        mProjectionView.setOnTouchListener { _, event ->
            sendTouchEvent(event)
            true
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(disconnectReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(keyCodeReceiver)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(disconnectReceiver, LocalIntent.FILTER_DISCONNECT)
        LocalBroadcastManager.getInstance(this).registerReceiver(keyCodeReceiver, LocalIntent.FILTER_KEY_EVENT)
    }

    private fun transport(): AapTransport {
        return App.get(this).transport()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        transport().send(VideoFocusEvent(true, true))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        transport().send(VideoFocusEvent(false, true))
    }

    private fun sendTouchEvent(event: MotionEvent) {

        val x = event.getX(0) / (mProjectionView.width / VIDEO_WIDTH)
        val y = event.getY(0) / (mProjectionView.height / VIDEO_HEIGHT)

        if (x < 0 || y < 0 || x >= 65535 || y >= 65535) {   // Infinity if vid_wid_get() or vid_hei_get() return 0
            AppLog.e("Invalid x: $x  y: $y")
            return
        }

        val action = TouchEvent.motionEventToAction(event)
        if (action == -1) {
            AppLog.e("event: $event (Unknown: ${event.actionMasked})  x: $x  y: $y")
            return
        }
        val ts = SystemClock.elapsedRealtime()
        transport().send(TouchEvent(ts, action, x.toInt(), y.toInt()))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("KeyCode: %d", keyCode)
        onKeyEvent(keyCode, true)

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("KeyCode: %d", keyCode)
        Utils.ms_sleep(100)
        onKeyEvent(keyCode, false)
        return super.onKeyUp(keyCode, event)
    }

    private fun onKeyEvent(keyCode: Int, isPress: Boolean) {
        transport().sendButton(keyCode, isPress)
    }

    companion object {
        val EXTRA_FOCUS = "focus"
        private val VIDEO_WIDTH = 800.0
        private val VIDEO_HEIGHT = 480.0

        fun start(context: Context) {
            val aapIntent = Intent(context, AapProjectionActivity::class.java)
            aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(aapIntent)
        }
    }
}