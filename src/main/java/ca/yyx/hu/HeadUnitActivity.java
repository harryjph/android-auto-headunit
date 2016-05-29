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
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.SystemUI;
import ca.yyx.hu.utils.Utils;


public class HeadUnitActivity extends Activity implements SurfaceHolder.Callback, UsbReceiver.Listener {

    private AapTransport mTransport;        // Transport API
    private SurfaceView mSurfaceView;

    private static final double m_virt_vid_wid = 800f;
    private static final double m_virt_vid_hei = 480f;

    private static final String DATA_DATA = "/data/data/ca.yyx.hu";
    private static final String SDCARD = "/sdcard/";

    private boolean isVideoStarted;

    private AudioDecoder mAudioDecoder;
    private VideoDecoder mVideoDecoder;

    private UiModeManager mUiModeManager = null;
    private UsbReceiver mUsbReceiver;
    private UsbManager mUsbManager;
    private boolean isShowing = true;
    private Settings mSettings;
    private static boolean starting_car_mode = false;
    private UsbAccessoryConnection mUsbAcceccoryConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // !! Keep Screen on !!
        setContentView(R.layout.activity_headunit);

        Utils.logd("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...");

        (findViewById(R.id.settings_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HeadUnitActivity.this, SettingsActivity.class));
            }
        });

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.getHolder().addCallback(this);
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
        mVideoDecoder = new VideoDecoder(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);

        mUsbAcceccoryConnection = new UsbAccessoryConnection(mUsbManager);
        mTransport = new AapTransport(mUsbAcceccoryConnection, mAudioDecoder, mVideoDecoder);
        mUsbReceiver = new UsbReceiver(this);
        mSettings = new Settings(this);

        UpdateManager.register(this);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                SystemUI.hide(getWindow().getDecorView());
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SystemUI.hide(getWindow().getDecorView());
    }

    @Override
    protected void onPause() {
        super.onPause();
        UpdateManager.unregister();

        Utils.sys_run(DATA_DATA + "/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CrashManager.register(this);
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
        mUsbAcceccoryConnection.disconnect();

        try {
            mUiModeManager.disableCarMode(0);
        } catch (Throwable t) {
            Utils.loge(t);
        }
        Utils.logd("Stop Car Mode");

    }

    @Override
    protected void onNewIntent(Intent intent) {
        // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 040b000000130801       #AUDIO_FOCUS_STATE_GAIN
        // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 000b0000000f0800       Byebye request
        // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 020b0000800808021001   VideoFocus lost focusState=0 unsolicited=true
        super.onNewIntent(intent);

        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (UsbReceiver.match(intent.getAction())) {
            mUsbReceiver.onReceive(this, intent);
        }
    }

//    private void drawerSelect(int idx) {
//        if (idx == 0) {                                                     // If Exit...
//            finish();                                                        // Hangs with TCP
//        } else if (idx == 1) {                                               // If Test...
//            startVideoTest();
//        } else if (idx == 2) {
//            mTransport.usb_force();
//        } else if (idx == 3) {
//            Utils.sys_run(DATA_DATA + "/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
//        }
//    }

    private void startVideoTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    videoTestRun();
                } catch (IOException e) {
                    Utils.loge(e);
                }
            }
        }, "run_vs").start();
    }

    private void videoTestRun() throws IOException {
        InputStream stream = getResources().openRawResource(R.raw.husam_h264);
        byte[] ba = Utils.toByteArray(stream);             // Read entire file, up to 16 MB to byte array ba
        stream.close();
        ByteBuffer bb;

        int size = ba.length;
        int left = size;
        int idx;
        int max_chunk_size = 65536 * 4;//16384;

        int chunk_size = max_chunk_size;
        int after;
        for (idx = 0; idx < size && left > 0; idx = after) {

            after = mVideoDecoder.h264_after_get(ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
            if (after == -1 && left <= max_chunk_size) {
                after = size;
                //hu_uti.logd ("Last chunk  chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
            } else if (after <= 0 || after > size) {
                Utils.loge("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
                return;
            }

            chunk_size = after - idx;

            byte[] bc = new byte[chunk_size];                               // Create byte array bc to hold chunk
            int ctr;
            for (ctr = 0; ctr < chunk_size; ctr++)
                bc[ctr] = ba[idx + ctr];                                      // Copy chunk_size bytes from byte array ba at idx to byte array bc

            //hu_uti.logd ("chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);

            idx += chunk_size;
            left -= chunk_size;

            bb = ByteBuffer.wrap(bc);                                        // Wrap chunk byte array bc to create byte buffer bb

            mVideoDecoder.decode(bb);                                                // Decode audio or H264 video content
            Utils.ms_sleep(20);                                             // Wait a frame
        }
    }

    // Touch:

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
        if (mTransport != null) {
            mTransport.touch_send(aa_action, x, y);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Utils.logd("holder %s, format: %d, width: %d, height: %d", holder, format, width, height);
        mVideoDecoder.onSurfaceHolderAvailable(holder, width, height);
        tryToConnect();

        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mVideoDecoder.stop();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onUsbDetach(UsbDeviceCompat deviceCompat) {
        if (mUsbAcceccoryConnection.isDeviceRunning(deviceCompat)) {
            finish();
        }
    }

    @Override
    public void onUsbAttach(UsbDeviceCompat deviceCompat) {

        if (deviceCompat.isInAccessoryMode() && !mTransport.isAapStarted()) {
            connect(deviceCompat);
            return;
        }

        if (!mSettings.isConnectingDevice(deviceCompat)) {
            return;
        }

        if (!mTransport.isAapStarted()) {
            connect(deviceCompat);
        }
    }

    @Override
    public void onUsdPermission(boolean granted, boolean connect, UsbDeviceCompat deviceCompat) {
        if (granted && connect) {
            connect(deviceCompat);
        }
    }

    private void tryToConnect() {

        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();

        for (String name : devices.keySet()) {
            UsbDeviceCompat deviceCompat = new UsbDeviceCompat(devices.get(name));
            if (mSettings.isConnectingDevice(deviceCompat) || deviceCompat.isInAccessoryMode()) {
                if (connect(deviceCompat))
                {
                    return;
                }
            }
        }

        if (getIntent() != null && UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(getIntent().getAction())) {      // If launched by a USB connection event... (Do nothing, let BCR handle)
            UsbDevice device = getIntent().<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE);
            UsbDeviceCompat deviceCompat = new UsbDeviceCompat(device);
            if (mSettings.isConnectingDevice(deviceCompat)) {
                connect(deviceCompat);
            }
        }
    }

    private boolean connect(UsbDeviceCompat deviceCompat) {

        if (!mUsbManager.hasPermission(deviceCompat.getWrappedDevice())) {                               // If we DON'T have permission to access the USB device...
            Toast.makeText(this, "Request USB permission for " + deviceCompat.getUniqueName(), Toast.LENGTH_SHORT).show();
            Utils.logd("Request USB Permission");    // Request permission
            Intent intent = new Intent(UsbReceiver.ACTION_USB_DEVICE_PERMISSION);                 // Our App specific Intent for permission request
            intent.setPackage(getPackageName());
            intent.putExtra(UsbReceiver.EXTRA_CONNECT, true);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            mUsbManager.requestPermission(deviceCompat.getWrappedDevice(), pendingIntent);              // Request permission. BCR called later if we get it.
            return true;
        }

        Toast.makeText(this, "Connecting to USB device " + deviceCompat.getUniqueName(), Toast.LENGTH_SHORT).show();

        try {
            if (mUsbAcceccoryConnection.connect(deviceCompat))
            {
                mTransport.start();
            }
        } catch (UsbAccessoryConnection.UsbOpenException e) {
            Toast.makeText(this, "["  + deviceCompat.getUniqueName() + "] Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

}