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
import android.os.PowerManager;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;


public class HeadUnitActivity extends Activity implements SurfaceHolder.Callback, UsbReceiver.Listener, HeadUnitTransport.Listener {

    private HeadUnitTransport mTransport;        // Transport API
    private SurfaceView mSurfaceView;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mContentView;

    private static final double m_virt_vid_wid = 800f;
    private static final double m_virt_vid_hei = 480f;

    // Presets:

    private static final int PRESET_LEN_FIX = 4;
    public static final int PRESET_LEN_USB = 5;  // Room left over for USB entries

    private static final String DATA_DATA = "/data/data/ca.yyx.hu";
    private static final String SDCARD = "/sdcard/";

    private ArrayList<String> mDrawerSections = new ArrayList<>();
    private boolean isVideoStarted;
    private AudioDecoder mAudioDecoder;
    private VideoDecoder mVideoDecoder;

    private UiModeManager mUiModeManager = null;
    private PowerManager.WakeLock mWakelock = null;

    private boolean m_tcp_connected = false;
    private UsbReceiver mUsbReceiver;


    private SimpleArrayMap<String,UsbDeviceCompat> mDevices = new SimpleArrayMap<>(HeadUnitActivity.PRESET_LEN_USB);
    private UsbManager mUsbManager;
    private boolean isShowing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // !! Keep Screen on !!
        setContentView(R.layout.layout);

        Utils.logd("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...");

        Utils.logd("--- savedInstanceState: " + savedInstanceState);
        Utils.logd("--- m_tcp_connected: " + m_tcp_connected);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                mDrawerLayout.bringChildToFront(drawerView);
                mDrawerLayout.requestLayout();
            }
        });

        findViewById(R.id.drawer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        mDrawerListView = (ListView) findViewById(R.id.left_drawer);
        mDrawerListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDrawerSections));
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                drawerSelect(position);
            }
        });

        mContentView = findViewById(android.R.id.content);

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

        PowerManager m_pwr_mgr = (PowerManager) getSystemService(POWER_SERVICE);
        mWakelock = m_pwr_mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadUnitWakeLockTag");
        mWakelock.acquire();                                          // Android M exception for WAKE_LOCK

        mAudioDecoder = new AudioDecoder(this);
        mVideoDecoder = new VideoDecoder(this);

        mUsbManager =  (UsbManager) getSystemService(Context.USB_SERVICE);
        loadUsbDevices();
        updateDrawerList();

        mTransport = new HeadUnitTransport(mUsbManager, mAudioDecoder, mVideoDecoder, this);                                       // Start USB/SSL/AAP Transport
        mUsbReceiver = new UsbReceiver(this);

        UpdateManager.register(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                SystemUI.hide(getWindow().getDecorView());
            }
        });

//        getWindow().getDecorView().setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    if (isShowing) {
//                        SystemUI.hide(getWindow().getDecorView());
//                        isShowing = false;
//                    }
//                }
//                return false;
//            }
//        });
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        CrashManager.register(this);
    }

    private static boolean starting_car_mode = false;

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
        // --- intent: Intent { act=android.hardware.usb.action.USB_DEVICE_ATTACHED flg=0x10000000 cmp=ca.yyx.hu/.HeadUnitActivity (has extras) }
        if (!intent.getAction().equals("send")) {                                     // If this is NOT our "fm.a2d.s2.send" Intent...
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            Utils.loge("extras == null");
            return;
        }

        String val = extras.getString("data", "def");
        if (val == null) {
            Utils.loge("val == null");
            return;
        }
        byte[] send_buf = Utils.hexstr_to_ba(val);
        String val2 = Utils.ba_to_hexstr(send_buf);
        Utils.logd("val: " + val + "  val2: " + val2);

        mTransport.test_send(send_buf, send_buf.length);

    }


    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());

        if (!starting_car_mode) {  // Else if have at least 1 USB device and we are not starting yet car mode...
            starting_car_mode = true;
            car_mode_start();
        }
        if (getIntent() != null && UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(getIntent().getAction())) {      // If launched by a USB connection event... (Do nothing, let BCR handle)
            UsbDevice device = getIntent().<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE);
            connect(new UsbDeviceCompat(device));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.logd("Stop app");

        unregisterReceiver(mUsbReceiver);

        Utils.logd("--- m_tcp_connected: " + m_tcp_connected);

        all_stop();

        if (!Utils.file_get(SDCARD + "/hu_usbr_disable"))
            Utils.sys_run(DATA_DATA + "/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);

        if (!starting_car_mode)
            android.os.Process.killProcess(android.os.Process.myPid());       // Kill this process completely for "super cleanup"
    }

    // Drawer:

    private void drawerSelect(int idx) {
        if (idx == 0) {                                                     // If Exit...
            finish();                                                        // Hangs with TCP
        } else if (idx == 1) {                                               // If Test...
            startVideoTest();
            //startActivity(new Intent(this, VideoTestActivity.class));
        } else if (idx == 2) {
            mTransport.usb_force();
        } else if (idx == 3) {
            Utils.sys_run(DATA_DATA + "/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
        } else if (idx >= PRESET_LEN_FIX) {
            mTransport.usb_select_device(mDevices.get(mDrawerSections.get(idx)));
        }
    }


    private void car_mode_start() {
        Utils.logd("Start Car Mode");
        try {
            if (mUiModeManager == null) {
                mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            }
            mUiModeManager.enableCarMode(0);
        } catch (Throwable t) {
            Utils.loge(t);
        }
    }

    private void car_mode_stop() {
        try {
            if (mUiModeManager != null) {                                          // If was used to enable...
                mUiModeManager.disableCarMode(0);
            }
        } catch (Throwable t) {
            Utils.loge(t);
        }
        Utils.logd("Stop Car Mode");
    }


    public void ui_video_started_set(boolean started) {                  // Called directly from HeadUnitTransport:jni_aap_start() because runOnUiThread() won't work
        if (isVideoStarted == started) {
            return;
        }

        Utils.logd("Started: "+started);

        if (started) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            mSurfaceView.setVisibility(View.VISIBLE);                     // Enable  video
        } else {
            mSurfaceView.setVisibility(View.GONE);                    // Disable video
        }

        isVideoStarted = started;
    }

    private void startVideoTest() {
        ui_video_started_set(true);
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
        byte [] ba = Utils.toByteArray(stream);             // Read entire file, up to 16 MB to byte array ba
        stream.close();
        ByteBuffer bb;

        int size = ba.length;
        int left = size;
        int idx = 0;
        int max_chunk_size = 65536 * 4;//16384;


        int chunk_size = max_chunk_size;
        int after = 0;
        for (idx = 0; idx < size && left > 0; idx = after) {

            after = mVideoDecoder.h264_after_get (ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
            if (after == -1 && left <= max_chunk_size) {
                after = size;
                //hu_uti.logd ("Last chunk  chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
            }
            else if (after <= 0 || after > size) {
                Utils.loge ("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
                return;
            }

            chunk_size = after - idx;

            byte [] bc = new byte [chunk_size];                               // Create byte array bc to hold chunk
            int ctr = 0;
            for (ctr = 0; ctr < chunk_size; ctr ++)
                bc [ctr] = ba [idx + ctr];                                      // Copy chunk_size bytes from byte array ba at idx to byte array bc

            //hu_uti.logd ("chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);

            idx += chunk_size;
            left -= chunk_size;

            bb = ByteBuffer.wrap (bc);                                        // Wrap chunk byte array bc to create byte buffer bb

            mVideoDecoder.decode(bb);                                                // Decode audio or H264 video content
            Utils.ms_sleep (20);                                             // Wait a frame
        }
    }


    private void all_stop() {

        car_mode_stop();

        mAudioDecoder.stop();
        mVideoDecoder.stop();

        ui_video_started_set(false);

        if (mTransport != null) {
            mTransport.stop();
        }

        try {
            if (mWakelock != null) {
                mWakelock.release();
            }
        } catch (Throwable t) {
            Utils.loge(t);
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
            Utils.loge("event: " + event + " (Translated: " + aa_action + ")  x: " + x + "  y: " + y);
            mTransport.touch_send(aa_action, x, y);
        }

        if (mDrawerLayout != null)
        {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Utils.logd("holder" + holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Utils.logd("holder %s, format: %d, width: %d, height: %d", holder, format, width, height);
        mVideoDecoder.onSurfaceHolderAvailable(holder, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mVideoDecoder.stop();
    }

    public void updateDrawerList() {
        mDrawerSections.clear();
        String[] sections = getResources().getStringArray(R.array.drawer_items);
        mDrawerSections.addAll(Arrays.asList(sections));

        for (int idx = 0; idx < mDevices.size(); idx++) {
            String name = mDevices.keyAt(idx);
            mDrawerSections.add(name);
        }
        mDrawerListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDrawerSections));
    }

    @Override
    public void onUsbDetach(UsbDeviceCompat deviceCompat) {
        mDevices.remove(deviceCompat.getName());
        updateDrawerList();
        if (mTransport.isDeviceRunning(deviceCompat)) {
            finish();
        }
    }

    @Override
    public void onUsbAttach(UsbDeviceCompat deviceCompat) {
        mDevices.put(deviceCompat.getName(),deviceCompat);
        updateDrawerList();
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

    private void loadUsbDevices()
    {
        Map<String, UsbDevice> device_list = null;
        try {
            device_list = mUsbManager.getDeviceList();
        } catch (Throwable e) {
            Utils.loge(e);
        }

        if (device_list != null && device_list.size() > 0) {
            for (UsbDevice device : device_list.values()) {
                UsbDeviceCompat deviceCompat = new UsbDeviceCompat(device);
                if (deviceCompat.isSupported()) {
                    mDevices.put(deviceCompat.getName(),deviceCompat);
                }
            }
        }
    }

    private void connect(UsbDeviceCompat deviceCompat) {

        if (!mUsbManager.hasPermission(deviceCompat.getWrappedDevice())) {                               // If we DON'T have permission to access the USB device...
            Utils.logd("Request USB Permission");    // Request permission
            Intent intent = new Intent(UsbReceiver.ACTION_USB_DEVICE_PERMISSION);                 // Our App specific Intent for permission request
            intent.setPackage(getPackageName());
            intent.putExtra(UsbReceiver.EXTRA_CONNECT, true);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            mUsbManager.requestPermission(deviceCompat.getWrappedDevice(), pendingIntent);              // Request permission. BCR called later if we get it.
            return;                                                           // Done for now. Wait for permission
        }

        mTransport.usb_select_device(deviceCompat);
    }

    @Override
    public void onAapStart() {
        ui_video_started_set(true);                             // Enable video/disable log view
    }

}