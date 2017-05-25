/*

pushd ~/b/android-sdk/extras/android/support/v7/appcompat
android update lib-project -p . --target android-23
ant release
popd

*/


// Headunit app Main Activity
/*

Start with USB plugged
start
  usb_attach_handler
    usb_connect

1st Permission granted
usb_receiver
  usb_attach_handler
    usb_connect
      usb_open
    -
      acc_mode_switch
      usb_disconnect

Disconnect
usb_receiver
  usb_detach_handler

Attached in ACC mode
usb_receiver
  usb_attach_handler
    usb_connect

2nd Permission granted
usb_receiver
  usb_attach_handler
    usb_connect
      usb_open
    -
      acc_mode_endpoints_set
  -
    jni_aap_start
*/

/* How to implement Android Open Accessory mode as a service:

Copy the intent that you received when starting your activity that you use to launch the service, because the intent contains the details of the accessory that the ADK implementation needs.
Then, in the service proceed to implement the rest of ADK exactly as before.

if (intent.getAction().equals(USB_OAP_ATTACHED)) {
    Intent i = new Intent(this, YourServiceName.class);
    i.putExtras(intent);
    startService(i);
}


*/
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
import ca.yyx.hu.activities.SurfaceActivity
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.Utils
import ca.yyx.hu.view.ProjectionView


class AapProjectionActivity : SurfaceActivity(), SurfaceHolder.Callback {
    private lateinit var mProjectionView: ProjectionView

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLog.i("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...")

        mProjectionView = findViewById(R.id.surface) as ProjectionView
        mProjectionView.setSurfaceCallback(this)
        mProjectionView.setOnTouchListener { _, event ->
            touch_send(event)
            true
        }

    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, LocalIntent.FILTER_DISCONNECT)
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

    internal fun touch_send(event: MotionEvent) {

        val x = event.getX(0) / (mProjectionView.width / m_virt_vid_wid)
        val y = event.getY(0) / (mProjectionView.height / m_virt_vid_hei)

        if (x < 0 || y < 0 || x >= 65535 || y >= 65535) {   // Infinity if vid_wid_get() or vid_hei_get() return 0
            AppLog.e("Invalid x: $x  y: $y")
            return
        }

        val aa_action: Int
        val me_action = event.actionMasked
        when (me_action) {
            MotionEvent.ACTION_POINTER_DOWN -> aa_action = MotionEvent.ACTION_POINTER_DOWN
            MotionEvent.ACTION_DOWN -> aa_action = MotionEvent.ACTION_DOWN
            MotionEvent.ACTION_MOVE -> aa_action = MotionEvent.ACTION_MOVE
            MotionEvent.ACTION_CANCEL -> aa_action = MotionEvent.ACTION_UP
            MotionEvent.ACTION_POINTER_UP -> aa_action = MotionEvent.ACTION_POINTER_UP
            MotionEvent.ACTION_UP -> aa_action = MotionEvent.ACTION_UP
            else -> {
                AppLog.e("event: $event (Unknown: $me_action)  x: $x  y: $y")
                return
            }
        }
        val ts = SystemClock.elapsedRealtime()
        transport().send(TouchEvent(ts, aa_action, x.toInt(), y.toInt()))
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

        private val m_virt_vid_wid = 800.0
        private val m_virt_vid_hei = 480.0

        fun start(context: Context) {
            val aapIntent = Intent(context, AapProjectionActivity::class.java)
            aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(aapIntent)
        }
    }
}