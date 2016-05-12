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
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbDeviceCompat;


public class HeadUnitActivity extends Activity implements SurfaceHolder.Callback {

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

    private String[] mDrawerSections;
    private boolean isVideoStarted;
    private AudioDecoder mAudioDecoder;
    private VideoDecoder mVideoDecoder;

    private UiModeManager mUiModeManager = null;
    private PowerManager.WakeLock mWakelock = null;

    private boolean m_tcp_connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // !! Keep Screen on !!
        setContentView(R.layout.layout);

        Utils.logd("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...");

        Utils.logd("--- savedInstanceState: " + savedInstanceState);
        Utils.logd("--- m_tcp_connected: " + m_tcp_connected);

        mDrawerSections = getResources().getStringArray(R.array.drawer_items);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                mDrawerLayout.bringChildToFront(drawerView);
                mDrawerLayout.requestLayout();
            }
        });

        ((ImageButton) findViewById(R.id.drawer_button)).setOnClickListener(new View.OnClickListener() {
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
                SystemUI.hide(mContentView, mDrawerLayout);
            }
        });

        mContentView = findViewById(android.R.id.content);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.getHolder().addCallback(this);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touch_send(event);
                return true;
            }
        });

        PowerManager m_pwr_mgr = (PowerManager) getSystemService(POWER_SERVICE);
        mWakelock = m_pwr_mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadUnitWakeLockTag");
        mWakelock.acquire();                                          // Android M exception for WAKE_LOCK

        mAudioDecoder = new AudioDecoder(this);
        mVideoDecoder = new VideoDecoder(this);

        mTransport = new HeadUnitTransport(this, mAudioDecoder);                                       // Start USB/SSL/AAP Transport
        Intent intent = getIntent();                                     // Get launch Intent

        int ret = mTransport.start(intent);
        if (ret <= 0) {                                                   // If no USB devices...
            if (!Utils.file_get(SDCARD + "/hu_nocarm") && !starting_car_mode) {  // Else if have at least 1 USB device and we are not starting yet car mode...
                Utils.logd("Before car_mode_start()");
                starting_car_mode = true;
                car_mode_start();
                Utils.logd("After  car_mode_start()");
            } else {
                Utils.logd("Starting car mode or disabled so don't call car_mode_start()");
                starting_car_mode = false;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.logd("unregisterUsbReceiver");
        mTransport.unregisterUsbReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.logd("registerUsbReceiver + Hide System UI");
        mTransport.registerUsbReceiver();
        SystemUI.hide(mContentView, null);
    }


    private static boolean starting_car_mode = false;

    @Override
    protected void onNewIntent(Intent intent) {
        // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 040b000000130801       #AUDIO_FOCUS_STATE_GAIN
        // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 000b0000000f0800       Byebye request
        // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 020b0000800808021001   VideoFocus lost focusState=0 unsolicited=true
        super.onNewIntent(intent);
        Utils.logd("--- intent: " + intent);

        String action = intent.getAction();
        if (action == null) {
            Utils.loge("action == null");
            return;
        }
        // --- intent: Intent { act=android.hardware.usb.action.USB_DEVICE_ATTACHED flg=0x10000000 cmp=ca.yyx.hu/.HeadUnitActivity (has extras) }
        if (!action.equals("send")) {                                     // If this is NOT our "fm.a2d.s2.send" Intent...
            //Utils.logd ("action: " + action);                              // action: android.hardware.usb.action.USB_DEVICE_ATTACHED
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
    protected void onStop() {
        super.onStop();
        Utils.logd("Stop app");

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
            car_mode_stop();
            finish();                                                        // Hangs with TCP
        } else if (idx == 1) {                                               // If Test...
            startVideoTest();
            //startActivity(new Intent(this, VideoTestActivity.class));
        } else if (idx == 2) {
            mTransport.usb_force();
        } else if (idx == 3) {
            Utils.sys_run(DATA_DATA + "/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
        } else if (idx >= PRESET_LEN_FIX) {
            mTransport.presets_select(idx - PRESET_LEN_FIX);
        }
    }


    private void car_mode_start() {
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
                Utils.logd("OK disableCarMode");
            }
        } catch (Throwable t) {
            Utils.loge(t);
        }
        Utils.logd("After disableCarMode");
    }


    public void ui_video_started_set(boolean started) {                  // Called directly from HeadUnitTransport:jni_aap_start() because runOnUiThread() won't work
        if (isVideoStarted == started) {
            return;
        }

        Utils.logd("Started: "+started);

        if (started) {
            SystemUI.hide(mContentView, mDrawerLayout);
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
        byte [] ba = convertStreamToByteArray(stream);             // Read entire file, up to 16 MB to byte array ba
        stream.close();
        ByteBuffer bb;

        int size = ba.length;
        int left = size;
        int idx = 0;
        int max_chunk_size = 65536 * 4;//16384;


        int chunk_size = max_chunk_size;
        int after = 0;
        for (idx = 0; idx < size && left > 0; idx = after) {

            after = h264_after_get (ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
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

    public static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[10240];
        int i;
        while ((i = is.read(buff, 0, buff.length)) > 0) {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function
    }

    int h264_after_get (byte [] ba, int idx) {
        idx += 4; // Pass 0, 0, 0, 1
        for (; idx < ba.length - 4; idx ++) {
            if (idx > 24)   // !!!! HACK !!!! else 0,0,0,1 indicates first size 21, instead of 25
                if (ba [idx] == 0 && ba [idx+1] == 0 && ba [idx+2] == 0 && ba [idx+3] == 1)
                    return (idx);
        }
        return (-1);
    }

    private void all_stop() {

        if (!starting_car_mode) {
            SystemUI.show(mContentView);

            car_mode_stop();
        }

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
            Utils.loge("Throwable: " + t);
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
                Utils.logd("event: " + event + " (ACTION_DOWN)    x: " + x + "  y: " + y);
                aa_action = MotionEvent.ACTION_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                Utils.logd("event: " + event + " (ACTION_MOVE)    x: " + x + "  y: " + y);
                aa_action = MotionEvent.ACTION_MOVE;
                break;
            case MotionEvent.ACTION_CANCEL:
                Utils.logd("event: " + event + " (ACTION_CANCEL)  x: " + x + "  y: " + y);
                aa_action = MotionEvent.ACTION_UP;
                break;
            case MotionEvent.ACTION_UP:
                Utils.logd("event: " + event + " (ACTION_UP)      x: " + x + "  y: " + y);
                aa_action = MotionEvent.ACTION_UP;
                break;
            default:
                Utils.loge("event: " + event + " (Unknown: " + me_action + ")  x: " + x + "  y: " + y);
                return;
        }
        if (mTransport != null) {
            mTransport.touch_send(aa_action, x, y);
        }

        if (mDrawerLayout != null)
        {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    public void sys_ui_hide() {
        SystemUI.hide(mContentView, mDrawerLayout);
    }

    public void handleMedia(byte[] buffer, int size) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.limit(size);
        bb.position(0);

        if (VideoDecoder.isH246Video(buffer)) {
            mVideoDecoder.decode(bb);
        } else {
            mAudioDecoder.decode(bb);
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

    public void presets_update(ArrayList<UsbDeviceCompat> devices) {
        for (int idx = 0; idx < PRESET_LEN_USB; idx++) {
            if (idx < devices.size()) {
                mDrawerSections[idx + PRESET_LEN_FIX] = devices.get(idx).getName();
            } else {
                mDrawerSections[idx + PRESET_LEN_FIX] = "";
            }
        }
        mDrawerListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDrawerSections));
    }
}