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
package ca.yyx.hu;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.SystemUI;
import ca.yyx.hu.utils.Utils;


public class HeadUnitActivity extends SurfaceActivity {

    public static void start(UsbDevice device, Context context) {
        Intent intent = new Intent(context, HeadUnitActivity.class);
        intent.putExtra(UsbManager.EXTRA_DEVICE, device);
        context.startActivity(intent);
    }

    private AapTransport mTransport;        // Transport API

    private static final double m_virt_vid_wid = 800f;
    private static final double m_virt_vid_hei = 480f;

    private static final String DATA_DATA = "/data/data/ca.yyx.hu";
    private static final String SDCARD = "/sdcard/";

    private AudioDecoder mAudioDecoder;

    private UiModeManager mUiModeManager = null;
    private boolean isShowing = true;
    private static boolean starting_car_mode = false;
    private UsbDeviceCompat mUsbDevice;

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

        UsbDevice device = IntentUtils.getDevice(getIntent());
        if (device == null) {
            Utils.loge("No device in intent");
            finish();
            return;
        }
        mUsbDevice = new UsbDeviceCompat(device);

        (findViewById(R.id.settings_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HeadUnitActivity.this, SettingsActivity.class));
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isShowing) {
                        SystemUI.hide(getWindow().getDecorView());
                        isShowing = false;
                    }
                }
                touch_send(event);
                return true;
            }
        });

        mAudioDecoder = new AudioDecoder(this);
        mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        mTransport = new AapTransport(mAudioDecoder, mVideoDecoder);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UpdateManager.unregister();

        //Utils.sys_run(DATA_DATA + "/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CrashManager.register(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, IntentUtils.DISCONNECT_FILTER);

        try {
            if (App.get(this).connect(mUsbDevice))
            {
                (new StartTask()).execute();
            }
            else
            {
                Utils.loge("Cannot connect to device " + mUsbDevice.getUniqueName());
                finish();
            }
        } catch (UsbAccessoryConnection.UsbOpenException e) {
            Utils.loge(e);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!starting_car_mode) {  // Else if have at least 1 USB device and we are not starting yet car mode...
            Utils.logd("Start Car Mode");
            starting_car_mode = true;
            try {
                mUiModeManager.enableCarMode(0);
            } catch (Throwable t) {
                Utils.loge(t);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mTransport.stop();
        mAudioDecoder.stop();

        try {
            mUiModeManager.disableCarMode(0);
        } catch (Throwable t) {
            Utils.loge(t);
        }
        Utils.logd("Stop Car Mode");

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
        mTransport.touch_send(aa_action, x, y);
    }

    private class StartTask extends AsyncTask<Object, Void, Integer> {

        @Override
        protected Integer doInBackground(Object... params) {
            Utils.logd("wifi_long_start start ");
            return mTransport.start(App.get(HeadUnitActivity.this).connection());
        }

        // Start activity that can handle the JPEG image
        @Override
        protected void onPostExecute(Integer result) {//String result) {
            if (result != null) {
                Utils.loge("AAP Start result " + result);
                finish();
            }
        }
    }
}