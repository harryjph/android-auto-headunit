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
package ca.yyx.hu.aap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.MotionEvent;
import android.view.View;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import ca.yyx.hu.App;
import ca.yyx.hu.R;
import ca.yyx.hu.SettingsActivity;
import ca.yyx.hu.SurfaceActivity;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.Utils;


public class AapActivity extends SurfaceActivity {

    private AapTransport mTransport;

    private static final double m_virt_vid_wid = 800f;
    private static final double m_virt_vid_hei = 480f;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.logd("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...");

        mTransport = App.get(this).transport();
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touch_send(event);
                return true;
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, IntentUtils.DISCONNECT_FILTER);
    }


    private void touch_send(MotionEvent event) {

        int x = (int) (event.getX(0) / (mSurfaceView.getWidth() / m_virt_vid_wid));
        int y = (int) (event.getY(0) / (mSurfaceView.getHeight() / m_virt_vid_hei));

        if (x < 0 || y < 0 || x >= 65535 || y >= 65535) {   // Infinity if vid_wid_get() or vid_hei_get() return 0
            Utils.loge("Invalid x: " + x + "  y: " + y);
            return;
        }

        byte aa_action;
        int me_action = event.getActionMasked();
        switch (me_action) {
            case MotionEvent.ACTION_DOWN:
                aa_action = MotionEvent.ACTION_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                aa_action = MotionEvent.ACTION_MOVE;
                break;
            case MotionEvent.ACTION_CANCEL:
                aa_action = MotionEvent.ACTION_UP;
                break;
            case MotionEvent.ACTION_UP:
                aa_action = MotionEvent.ACTION_UP;
                break;
            default:
                Utils.loge("event: " + event + " (Unknown: " + me_action + ")  x: " + x + "  y: " + y);
                return;
        }
        mTransport.sendTouch(aa_action, x, y);
    }

}