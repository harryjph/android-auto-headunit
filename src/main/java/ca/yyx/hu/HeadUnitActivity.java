/*

pushd ~/b/android-sdk/extras/android/support/v7/appcompat
android update lib-project -p . --target android-23
ant release
popd

*/


// Headunit app Main Activity
/*

Start with USB plugged
transport_start
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
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;


public class HeadUnitActivity extends Activity implements TextureView.SurfaceTextureListener, WifiManager.Listener {
    /*

    Internal classes:

      public  class wifi_start_task             extends AsyncTask
      private class logs_email_tmr_hndlr        extends java.util.TimerTask
      public  class WiFiDirectBroadcastReceiver extends BroadcastReceiver

    Public for HeadUnitTransport:

      public void mic_audio_stop        ();
      public int  mic_audio_read        (byte [] aud_buf, int max_len);
      public void out_audio_stop        (int chan);
      public void media_decode          (ByteBuffer content);               // Decode audio or H264 video content. Called only by video_test() & HeadUnitTransport.aa_cmd_send()

      public void sys_ui_hide           ();
      public void sys_ui_show           ();
      public void presets_update        (String [] usb_list_name);          // Update Presets. Called only by HeadUnitActivity:usb_add() & HeadUnitActivity:usb_del()
      public void ui_video_started_set  (boolean started);                  // Called directly from HeadUnitTransport:jni_aap_start() because runOnUiThread() won't work
                                                                            // Also from: video_started_set(), video_test_start(), wifi_start
      public  boolean           disable_video_started_set = false;//true;
      public  static final int  PRESET_LEN_USB            = PRESET_LEN_TOT - PRESET_LEN_FIX;  // Room left over for USB entries
      public  static final int  MIC_BUFFER_SIZE             = 8192;

    */

    private HeadUnitTransport mTransport;        // Transport API
    private TextureView mTextureView;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mContentView;

    private WifiManager mWifiManager;

    private double m_scr_wid = 0;
    private double m_scr_hei = 0;

    private static final double m_virt_vid_wid = 800f;
    private static final double m_virt_vid_hei = 480f;
    //private double        m_virt_vid_wid = 1280f;
    //private double        m_virt_vid_hei =  720f;
    //private double        m_virt_vid_wid = 1920f;
    //private double        m_virt_vid_hei = 1080f;

    private boolean m_scr_land = true;

    // Presets:

    private static final int PRESET_LEN_TOT = 16;
    private static final int PRESET_LEN_FIX = 11;
    public static final int PRESET_LEN_USB = PRESET_LEN_TOT - PRESET_LEN_FIX;  // Room left over for USB entries
    private String[] mDrawerSections;
    private boolean isVideoStarted;
    private AudioDecoder mAudioDecoder;
    private VideoDecoder mVideoDecoder;

    public void presets_update(String[] usb_list_name) {                // Update Presets. Called only by HeadUnitActivity:usb_add() & HeadUnitActivity:usb_del()
        for (int idx = 0; idx < PRESET_LEN_USB; idx++) {
            Utils.logd("idx: " + idx + "  name: " + usb_list_name[idx]);
            if (usb_list_name[idx] != null) {
                mDrawerSections[idx + PRESET_LEN_FIX] = usb_list_name[idx];
            }
        }
        mDrawerListView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, mDrawerSections));
    }

    private UiModeManager mUiModeManager = null;
    private PowerManager.WakeLock mWakelock = null;

    private boolean m_tcp_connected = false;

    private boolean disable_usb = false;//true; // For x86 emulator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // !! Keep Screen on !!
        setContentView(R.layout.layout);

        Utils.logd("Headunit for Android Auto (tm) - Copyright 2011-2015 Michael A. Reid. All Rights Reserved...");

        Utils.logd("--- savedInstanceState: " + savedInstanceState);
        Utils.logd("--- m_tcp_connected: " + m_tcp_connected);

        m_scr_hei = getWindowManager().getDefaultDisplay().getHeight();
        m_scr_wid = getWindowManager().getDefaultDisplay().getWidth();
        Utils.logd("m_scr_wid: " + m_scr_wid + "  m_scr_hei: " + m_scr_hei);

        android.graphics.Point size = new android.graphics.Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        m_scr_wid = size.x;
        m_scr_hei = size.y;

        m_scr_land = m_scr_wid > m_scr_hei;

        Utils.logd("m_scr_wid: " + m_scr_wid + "  m_scr_hei: " + m_scr_hei + "  m_scr_land: " + m_scr_land);

        mDrawerSections = getResources().getStringArray(R.array.drawer_items);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerListView = (ListView) findViewById(R.id.left_drawer);
        mDrawerListView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, mDrawerSections));
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                drawerSelect(position);
                SystemUI.hide(mContentView, mDrawerLayout);
            }
        });

        mContentView = findViewById(android.R.id.content);

        SystemUI.hide(mContentView, null);           // closes drawer

        mDrawerLayout.openDrawer(Gravity.LEFT);

        mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);

        mTextureView = (TextureView) findViewById(R.id.tv_vid);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touch_send(event);
                return (true);
            }
        });

        PowerManager m_pwr_mgr = (PowerManager) getSystemService(POWER_SERVICE);
        mWakelock = m_pwr_mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadUnitWakelockTag");
        mWakelock.acquire();                                          // Android M exception for WAKE_LOCK

        mAudioDecoder = new AudioDecoder(this);
        mVideoDecoder = new VideoDecoder(this);

        Utils.file_delete("/data/data/ca.yyx.hu/files/nfc_wifi");

        if (Utils.file_get("/sdcard/hu_selinux_disable"))
            Utils.sys_run("setenforce 0 1>/dev/null 2>/dev/null ; chmod -R 777 /dev/bus 1>/dev/null 2>/dev/null ; rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", true);
        else if (!Utils.file_get("/sdcard/hu_su_disable"))
            Utils.sys_run("chmod -R 777 /dev/bus 1>/dev/null 2>/dev/null ; rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", true);
        else
            Utils.sys_run("rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", false);

        mTransport = new HeadUnitTransport(this, mAudioDecoder);                                       // Start USB/SSL/AAP Transport
        Intent intent = getIntent();                                     // Get launch Intent

        int ret = 0;
        if (!disable_usb)
            ret = mTransport.transport_start(intent);
        if (ret <= 0) {                                                   // If no USB devices...

            if (!Utils.file_get("/sdcard/hu_nocarm") && !starting_car_mode) {  // Else if have at least 1 USB device and we are not starting yet car mode...
                Utils.logd("Before car_mode_start()");
                starting_car_mode = true;
                car_mode_start();
                Utils.logd("After  car_mode_start()");
            } else {
                Utils.logd("Starting car mode or disabled so don't call car_mode_start()");
                starting_car_mode = false;
            }
        }

        mWifiManager = new WifiManager(this, mTransport, this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mTransport.unregisterUsbReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTransport.registerUsbReceiver();
    }


    private static boolean starting_car_mode = false;

    @Override
    protected void onNewIntent(Intent intent) {    // am start -n ca.yyx.hu/.HeadUnitActivity -a "send" -e data 040b000000130801       #AUDIO_FOCUS_STATE_GAIN
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
    protected void onDestroy() {
        super.onDestroy();
        Utils.logd("--- m_tcp_connected: " + m_tcp_connected);

        all_stop();

        if (!Utils.file_get("/sdcard/hu_usbr_disable"))
            Utils.sys_run("/data/data/ca.yyx.hu/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);

        if (!starting_car_mode)
            android.os.Process.killProcess(android.os.Process.myPid());       // Kill this process completely for "super cleanup"
    }

    // Drawer:

    private void drawerSelect(int idx) {
        // Exit, Test, Self, Wifi, NFC, Day, Night, Auto, SUsb, RUsb
        if (idx == 0) {                                                     // If Exit...
            car_mode_stop();
            if (!m_tcp_connected) {                                          // If not TCP
                finish();
                return;
            }
            //android.os.Process.killProcess (android.os.Process.myPid ());// Kill this process completely for "super cleanup"
            finish();                                                        // Hangs with TCP
        } else if (idx == 1)                                                  // If Test...
            startActivity(new Intent(this, VideoTestActivity.class));
        else if (idx == 2 || idx == 3) {                                    // If Self or Wifi...
            if (idx == 2) {
                // Wifi Direct
                Utils.sys_run("mkdir /data/data/ca.yyx.hu/files/ 1>/dev/null 2>/dev/null ; touch /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", false);
                Utils.file_create("/data/data/ca.yyx.hu/files/nfc_wifi"); // Directory must be created first so this wasn't working
                Toast.makeText(HeadUnitActivity.this, "Starting Self - Please Wait... :)", Toast.LENGTH_LONG).show();
            } else {
                Utils.sys_run("rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", false);
                Utils.file_delete("/data/data/ca.yyx.hu/files/nfc_wifi");
                Toast.makeText(HeadUnitActivity.this, "Starting Wifi - Please Wait... :)", Toast.LENGTH_LONG).show();
            }

            SystemUI.show(mContentView);

            mWifiManager.initP2P();

            m_tcp_connected = false;
            ui_video_started_set(true);                                      // Enable video/disable log view
            mWifiManager.start();

        } else if (idx == 4)                                            // NFC
            Utils.sys_run("am start -a android.nfc.action.NDEF_DISCOVERED -t application/com.google.android.gms.car -n com.google.android.gms/.car.FirstActivity -f 32768 1>/dev/null 2>/dev/null", true);
        else if (idx == 5)  // Day
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
        else if (idx == 6)
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
        else if (idx == 7)
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
        else if (idx == 8)
            SystemUI.hide(mContentView, mDrawerLayout);
        else if (idx == 9)
            mTransport.usb_force();
        else if (idx == 10)
            Utils.sys_run("/data/data/ca.yyx.hu/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
        else if (idx >= PRESET_LEN_FIX && idx <= PRESET_LEN_TOT - 1)
            mTransport.presets_select(idx - PRESET_LEN_FIX);
    }

    private void car_mode_start() {
        try {
            if (mUiModeManager == null)
                mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (mUiModeManager != null) {
                mUiModeManager.enableCarMode(0);
            }
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }
    }

    private void car_mode_stop() {
        try {
            if (mUiModeManager != null) {                                          // If was used to enable...
                mUiModeManager.disableCarMode(0);
                Utils.logd("OK disableCarMode");
            }
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }
        Utils.logd("After disableCarMode");
    }


    public void ui_video_started_set(boolean started) {                  // Called directly from HeadUnitTransport:jni_aap_start() because runOnUiThread() won't work
        if (isVideoStarted == started) {
            return;
        }

        if (started) {
            SystemUI.hide(mContentView, mDrawerLayout);
            mTextureView.setVisibility(View.VISIBLE);                     // Enable  video
//                if (m_scr_land && m_scr_wid / m_scr_hei < 1.5)                  // If closer to 4:3 than 16:9...    16:9 = 1.777     //  4:3 = 1.333
//                    m_ll_tv_ext.setVisibility(View.VISIBLE);                     // Enable  aspect ratio bar
//                else
//                    m_ll_tv_ext.setVisibility(View.GONE);
        } else {
            mTextureView.setVisibility(View.GONE);                    // Disable video
//                m_ll_tv_ext.setVisibility(View.GONE);                        // Disable aspect ratio bar
        }

        isVideoStarted = started;
    }


    private void all_stop() {

        if (!starting_car_mode) {
            SystemUI.show(mContentView);

            car_mode_stop();
        }

        mAudioDecoder.stop();
        mVideoDecoder.stop();
        isVideoStarted = false;

        if (mTransport != null)
            if (!disable_usb)
                mTransport.transport_stop();

        mWifiManager.deinitP2P();

        try {
            if (mWakelock != null)
                mWakelock.release();
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }

    }

    // Touch:

    private void touch_send(MotionEvent event) {
        //Utils.logd ("event: " + event);

        int x = (int) (event.getX(0) / (mTextureView.getWidth() / m_virt_vid_wid));
        int y = (int) (event.getY(0) / (mTextureView.getHeight() / m_virt_vid_hei));

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
        if (mTransport != null)
            mTransport.touch_send(aa_action, x, y);
    }


    // Video: TextureView

    @Override
    // Called after onCreate(), which calls "mTextureView.setSurfaceTextureListener (HeadUnitActivity.this);"
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Utils.logd("--- sur_tex: " + surface + "  width: " + width + "  height: " + height);  // N9: width: 2048  height: 1253
        mVideoDecoder.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture sur_tex, int width, int height) {        // ignore
        Utils.logd("--- sur_tex: " + sur_tex + "  width: " + width + "  height: " + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture sur_tex) {
        Utils.logd("--- sur_tex: " + sur_tex);
        return false;                                                     // Prevent destruction of SurfaceTexture
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture sur_tex) {             // Called many times per second when video changes, so ignore
        //Utils.logd ("--- sur_tex: " + sur_tex + "  m_sur_tex: " + m_sur_tex);
    }


    @Override
    public void onWifiStartListener() {
        m_tcp_connected = true;
        // TODO
    }

    public void sys_ui_hide() {
        SystemUI.hide(mContentView, mDrawerLayout);
    }

    public void handleMedia(byte[] buffer,int size) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.limit(size);
        bb.position(0);

        if (VideoDecoder.isH246Video(buffer)) {
            mVideoDecoder.decode(bb);
        } else {
            mAudioDecoder.decode(bb);
        }
    }
}