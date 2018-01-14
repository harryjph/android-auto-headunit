package info.anodsplace.headunit.aap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder

import info.anodsplace.headunit.App
import info.anodsplace.headunit.R
import info.anodsplace.headunit.aap.protocol.Screen
import info.anodsplace.headunit.aap.protocol.messages.TouchEvent
import info.anodsplace.headunit.aap.protocol.messages.VideoFocusEvent
import info.anodsplace.headunit.app.SurfaceActivity
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.IntentFilters
import info.anodsplace.headunit.view.ProjectionView
import info.anodsplace.headunit.contract.KeyIntent

class AapProjectionActivity : SurfaceActivity(), SurfaceHolder.Callback {
    private val projectionView: ProjectionView by lazy { findViewById<ProjectionView>(R.id.surface) }

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    private val keyCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.getParcelableExtra<KeyEvent>(KeyIntent.extraEvent)
            onKeyEvent(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLog.i("HeadUnit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...")

        projectionView.setSurfaceCallback(this)
        projectionView.setOnTouchListener { _, event ->
            sendTouchEvent(event)
            true
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(disconnectReceiver)
        unregisterReceiver(keyCodeReceiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(disconnectReceiver, IntentFilters.disconnect)
        registerReceiver(keyCodeReceiver, IntentFilters.keyEvent)
    }

    val transport: AapTransport
        get() = App.provide(this).transport

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        transport.send(VideoFocusEvent(true, true))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        transport.send(VideoFocusEvent(false, true))
    }

    private fun sendTouchEvent(event: MotionEvent) {

        val x = event.getX(0) / (projectionView.width / Screen.width.toFloat())
        val y = event.getY(0) / (projectionView.height / Screen.height.toFloat())

        if (x < 0 || y < 0 || x >= 65535 || y >= 65535) {
            AppLog.e("Invalid x: $x  y: $y")
            return
        }

        val action = TouchEvent.motionEventToAction(event)
        if (action == -1) {
            AppLog.e("event: $event (Unknown: ${event.actionMasked})  x: $x  y: $y")
            return
        }
        val ts = SystemClock.elapsedRealtime()
        transport.send(TouchEvent(ts, action, x.toInt(), y.toInt()))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("KeyCode: %d", keyCode)
        // PRes navigation on the screen
        onKeyEvent(keyCode, true)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("KeyCode: %d", keyCode)
        onKeyEvent(keyCode, false)
        return super.onKeyUp(keyCode, event)
    }

    private fun onKeyEvent(keyCode: Int, isPress: Boolean) {
        transport.send(keyCode, isPress)
    }

    companion object {
        const val EXTRA_FOCUS = "focus"

        fun intent(context: Context): Intent {
            val aapIntent = Intent(context, AapProjectionActivity::class.java)
            aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return aapIntent
        }
    }
}