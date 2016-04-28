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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Timer;

//import android.net.wifi.p2p.WifiP2pManager;
//import android.net.wifi.p2p.WifiP2pManager.Channel;


public class HeadUnitActivity extends Activity implements TextureView.SurfaceTextureListener {
    /*

    Internal classes:

      public  class wifi_start_task             extends AsyncTask
      private class logs_email_tmr_hndlr        extends java.util.TimerTask
      public  class WiFiDirectBroadcastReceiver extends BroadcastReceiver

    Public for UsbTransport:

      public void mic_audio_stop        ();
      public int  mic_audio_read        (byte [] aud_buf, int max_len);
      public void out_audio_stop        (int chan);
      public void media_decode          (ByteBuffer content);               // Decode audio or H264 video content. Called only by video_test() & UsbTransport.aa_cmd_send()

      public void sys_ui_hide           ();
      public void sys_ui_show           ();
      public void presets_update        (String [] usb_list_name);          // Update Presets. Called only by HeadUnitActivity:usb_add() & HeadUnitActivity:usb_del()
      public void ui_video_started_set  (boolean started);                  // Called directly from UsbTransport:jni_aap_start() because runOnUiThread() won't work
                                                                            // Also from: video_started_set(), video_test_start(), wifi_start
      public  boolean           disable_video_started_set = false;//true;
      public  static final int  PRESET_LEN_USB            = PRESET_LEN_TOT - PRESET_LEN_FIX;  // Room left over for USB entries
      public  static final int  m_mic_bufsize             = 8192;

    */
    private UsbTransport mUsbTransport;        // Transport API

    private TextView m_tv_log;
    private View m_ll_tv_log;

//    private LinearLayout m_ll_tv_ext;

    private FrameLayout m_fl_tv_vid;
    private TextureView m_tv_vid;

    // These fields are guarded by m_surf_codec_lock. This is to ensure that the surface lifecycle is respected.
    // Although decoding happens on the transport thread, we are not allowed to access the surface after it is destroyed by the UI thread so we need to stop the codec immediately.
    private final Object m_surf_codec_lock = new Object();
    private SurfaceTexture m_sur_tex = null;
    //private Surface         m_surface  = null;
    //private int             m_sur_wid  = 0;
    //private int             m_sur_hei  = 0;

    private MediaCodec m_codec;
    private ByteBuffer[] m_codec_input_bufs;
    private BufferInfo m_codec_buf_info;

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

    public void presets_update(String[] usb_list_name) {                // Update Presets. Called only by HeadUnitActivity:usb_add() & HeadUnitActivity:usb_del()
        for (int idx = 0; idx < PRESET_LEN_USB; idx++) {
            Utils.logd("idx: " + idx + "  name: " + usb_list_name[idx]);
            if (usb_list_name[idx] != null) {
                m_drawer_selections[idx + PRESET_LEN_FIX] = usb_list_name[idx];
            }
        }
        m_lv_drawer.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, m_drawer_selections));
    }

    private void tv_log_append(final String str) {
        class log_task implements Runnable {
            String str;

            log_task(String s) {
                str = s;
            }

            public void run() {
                m_tv_log.append(str);
            }
        }
        //Thread t = new Thread(new log_task (s));
        //t.start();
        runOnUiThread(new log_task(str));
    }

    private void screen_logd(final String str) {
        Utils.logd(str);
        tv_log_append(str + "\n");           // at ca.yyx.hu.HeadUnitActivity.media_decode(HeadUnitActivity.java:564)
    }


    private UiModeManager m_uim_mgr = null;
    private PowerManager m_pwr_mgr = null;
    private PowerManager.WakeLock m_wakelock = null;
    private View m_ll_main = null;         // Main view of activity

    private String[] m_drawer_selections = {
            "Exit",
            "Test",
            "Self",
            "Wifi",
            "NFC",

            "Day",
            "Night",
            "Auto",
            "Hide",
            "SUsb",

            "RUsb",

            "", "", "", "", ""
    };

    private DrawerLayout m_dl_drawer;
    private ListView m_lv_drawer;


    private class drawer_item_click_listener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            Utils.logd("position: " + position + "  id: " + id);

            m_dl_drawer.closeDrawer(Gravity.LEFT);

            function_select(position);

            Utils.logd("sys_ui_hide (): " + sys_ui_hide());
        }
    }

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

        m_dl_drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        m_lv_drawer = (ListView) findViewById(R.id.left_drawer);
        m_lv_drawer.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, m_drawer_selections));
        m_lv_drawer.setOnItemClickListener(new drawer_item_click_listener());

        Utils.logd("sys_ui_hide (): " + sys_ui_hide());           // closes drawer

        m_dl_drawer.openDrawer(Gravity.LEFT);

        m_uim_mgr = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        m_tv_log = (TextView) findViewById(R.id.tv_log);
        m_tv_log.setOnClickListener(short_click_lstnr);

        m_ll_tv_log = findViewById(R.id.ll_tv_log);

        m_fl_tv_vid = (FrameLayout) findViewById(R.id.fl_tv_vid);

        m_tv_vid = (TextureView) findViewById(R.id.tv_vid);
        m_tv_vid.setSurfaceTextureListener(this);
        m_tv_vid.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touch_send(event);
                return (true);
            }
        });

        m_pwr_mgr = (PowerManager) getSystemService(POWER_SERVICE);
        m_wakelock = m_pwr_mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadUnitWakelockTag");
        if (m_wakelock != null)
            m_wakelock.acquire();                                          // Android M exception for WAKE_LOCK

        msg_display();

        Utils.file_delete("/data/data/ca.yyx.hu/files/nfc_wifi");

        if (Utils.file_get("/sdcard/hu_selinux_disable"))
            Utils.sys_run("setenforce 0 1>/dev/null 2>/dev/null ; chmod -R 777 /dev/bus 1>/dev/null 2>/dev/null ; rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", true);
        else if (!Utils.file_get("/sdcard/hu_su_disable"))
            Utils.sys_run("chmod -R 777 /dev/bus 1>/dev/null 2>/dev/null ; rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", true);
        else
            Utils.sys_run("rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", false);

        mUsbTransport = new UsbTransport(this);                                       // Start USB/SSL/AAP Transport
        Intent intent = getIntent();                                     // Get launch Intent

        int ret = 0;
        if (!disable_usb)
            ret = mUsbTransport.transport_start(intent);
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


        wifi_resume(); // Does nothing now

        Utils.logd("end");
    }

    private static boolean starting_car_mode = false;

/*
07-12 05:38:27.509 D/             arr_sys_run(17106): su: false  line: am start -a android.nfc.action.NDEF_DISCOVERED -t application/com.google.android.gms.car -n com.google.android.gms/.car.FirstActivity
07-12 05:38:27.767 D/AndroidRuntime(17122): >>>>>> START com.android.internal.os.RuntimeInit uid 10090 <<<<<<
07-12 05:38:27.771 D/AndroidRuntime(17122): CheckJNI is OFF
07-12 05:38:27.804 D/ICU     (17122): No timezone override file found: /data/misc/zoneinfo/current/icu/icu_tzdata.dat
07-12 05:38:27.838 I/Radio-JNI(17122): register_android_hardware_Radio DONE
07-12 05:38:27.866 D/AndroidRuntime(17122): Calling main entry com.android.commands.am.Am
07-12 05:38:27.871 W/ActivityManager(  625): Permission Denial: startActivity asks to run as user -2 but is calling from user 0; this requires android.permission.INTERACT_ACROSS_USERS_FULL
07-12 05:38:27.879 I/art     (17122): System.exit called, status: 1
07-12 05:38:27.879 I/AndroidRuntime(17122): VM exiting with result code 1.
07-12 05:38:27.905 W/             arr_sys_run(17106): cmds [0]: am start -a android.nfc.action.NDEF_DISCOVERED -t application/com.google.android.gms.car -n com.google.android.gms/.car.FirstActivity  exit_val: 1
*/

    private void msg_display() {
        String intro1_str = "HEADUNIT for Android Auto (tm)\n" +
                "You accept all liability for any reason by using, distributing, doing, or not doing anything else in respect of this software.\n" +
                "This software is experimental and may be distracting. Use it only for testing at this time. Do NOT use this software while operating a vehicle of any sort.\n";
        screen_logd(intro1_str);

        String intro2_str = "";
        if (this.getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_USB_HOST))
            intro2_str = "This device/ROM must support USB Host Mode\n";
        else
            intro2_str = "This device/ROM must support USB Host Mode AND IT DOES NOT APPEAR TO BE SUPPORTED !!!!!!!!!!\n";
        screen_logd(intro2_str);

        String intro3_str = "" +
                "See XDA thread for latest info and ask any question\n" +
                "\n" +
                "Connect this device to USB OTG cable\n" +
                "Connect USB OTG cable to regular phone USB cable\n" +
                "Connect regular USB cable to Android 5+ device running Google Android Auto app\n" +
                "Start this Headunit app\n" +
                "Respond OK to prompts\n" +
                "First Time with Android Auto check phone screen to see if apps need to be updated or prompts accepted\n" +
                "If success, this device screen will show Android Auto User Interface and other device screen should go dark\n" +
                "\n" +
                "A LOT of USB and OTG cables will not work\n" +
                "It can be tricky to get working the first time\n" +
                "\n";
        screen_logd(intro3_str);
    }

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
        //Utils.logd ("extras: " + extras.describeContents ());            // Always "extras: 0"

        String val = extras.getString("data", "def");
        if (val == null) {
            Utils.loge("val == null");
            return;
        }
        //byte [] send_buf = new byte [size];
        byte[] send_buf = Utils.hexstr_to_ba(val);
        String val2 = Utils.ba_to_hexstr(send_buf);
        Utils.logd("val: " + val + "  val2: " + val2);

        mUsbTransport.test_send(send_buf, send_buf.length);

    }

    @Override
    public void onRestart() {                                            // Restart comes between Stop and Start or when returning to the app
        super.onRestart();
        Utils.logd("--- ");

        if (m_sur_tex != null && m_tv_vid != null) {
            try {
                m_tv_vid.setSurfaceTexture(m_sur_tex);                         // java.lang.IllegalArgumentException: Trying to setSurfaceTexture to the same SurfaceTexture that's already set.
            } catch (Throwable e) {
                Log.e("setSurfaceTexture: ", e.getMessage());
            }
            Utils.logd("sys_ui_hide (): " + sys_ui_hide());
            Utils.logd("--- setSurfaceTexture done");
        }
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

    // Color:
    private boolean wifi_direct = true;//false;

    private void function_select(int idx) {
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
            video_test_start(false);
        else if (idx == 2 || idx == 3) {                                    // If Self or Wifi...
            if (idx == 2) {                                                   // If Self...
                if (wifi_direct) {
                    Utils.sys_run("mkdir /data/data/ca.yyx.hu/files/ 1>/dev/null 2>/dev/null ; touch /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", false);
                    Utils.file_create("/data/data/ca.yyx.hu/files/nfc_wifi"); // Directory must be created first so this wasn't working
                }
                Toast.makeText(m_context, "Starting Self - Please Wait... :)", Toast.LENGTH_LONG).show();
            } else {
                Utils.sys_run("rm -f /data/data/ca.yyx.hu/files/nfc_wifi 1>/dev/null 2>/dev/null", false);
                Utils.file_delete("/data/data/ca.yyx.hu/files/nfc_wifi");
                Toast.makeText(m_context, "Starting Wifi - Please Wait... :)", Toast.LENGTH_LONG).show();
            }
            //car_mode_start ();
            sys_ui_show();

            //if (idx != 2 || ! wifi_direct) {
            wifi_init();
            //}

            wifi_start();
        } else if (idx == 4)                                            // NFC
            Utils.sys_run("am start -a android.nfc.action.NDEF_DISCOVERED -t application/com.google.android.gms.car -n com.google.android.gms/.car.FirstActivity -f 32768 1>/dev/null 2>/dev/null", true);
        else if (idx == 5)  // Day
            m_uim_mgr.setNightMode(UiModeManager.MODE_NIGHT_NO);
        else if (idx == 6)
            m_uim_mgr.setNightMode(UiModeManager.MODE_NIGHT_YES);
        else if (idx == 7)
            m_uim_mgr.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
        else if (idx == 8)
            //m_uim_mgr.enableCarMode (0);//UiModeManager.ENABLE_CAR_MODE_GO_CAR_HOME);
            Utils.logd("sys_ui_hide (): " + sys_ui_hide());
        else if (idx == 9)
            mUsbTransport.usb_force();
        else if (idx == 10)
            Utils.sys_run("/data/data/ca.yyx.hu/lib/libusb_reset.so /dev/bus/usb/*/* 1>/dev/null 2>/dev/null", true);
        else if (idx >= PRESET_LEN_FIX && idx <= PRESET_LEN_TOT - 1)
            mUsbTransport.presets_select(idx - PRESET_LEN_FIX);
        return;
    }

    private /*static*/ boolean m_tcp_connected = false;

    private boolean async_wifi_start = true;//false;

    private void wifi_start() {                                          // BlocksED !!!!
        if (mUsbTransport != null) {

            m_tcp_connected = false;

            ui_video_started_set(true);                                      // Enable video/disable log view

            if (!async_wifi_start) {
                wifi_long_start();
                return;
            }

            // !! Async makes data lag a LOT !! Because different thread for setup ???
            AsyncTask at = new wifi_start_task(this);
            Utils.logd("at: " + at);
            at.execute();
        }
    }

    private class wifi_start_task extends AsyncTask { //<Void, Void, Void> {

        private Context context;
        //private TextView statusText;

        public wifi_start_task(Context context) {//, View statusText) {
            this.context = context;
            //this.statusText = (TextView) statusText;
            Utils.logd("context: " + context);
        }

        @Override
        protected Object doInBackground(Object... params) {//(Void... params) {// (Params... p) {//Void... v) {//Void... params) {
            //protected String doInBackground (Object... obj) {//String... str) {// params) {

            try {
                Utils.logd("params: " + params);

                wifi_long_start();
                Utils.logd("wifi_long_start done");

/*
        ServerSocket serverSocket = new ServerSocket (30515);//8888);   // Create a server socket
        Utils.logd ("new serverSocket: " + serverSocket);

        Socket client = serverSocket.accept ();                         // Block waiting for client connections
        Utils.logd ("serverSocket.accept client: " + client);

        // If this code is reached, a client has connected

        InputStream  inp_str = client.getInputStream ();
        OutputStream out_str = client.getOutputStream ();

        byte vr_buf [] = {0, 3, 0, 6, 0, 1, 0, 1, 0, 1};                    // Version Request
        int ret = hu_aap_usb_set (0, 3, 1, vr_buf, vr_buf.length);
        //ret = hu_aap_usb_send (vr_buf, sizeof (vr_buf), 1000);              // Send Version Request

        out_str.write (vr_buf, 0, vr_buf.length);
        out_str.flush ();
        Utils.logd ("version request sent to Wifi output");

        Utils.ms_sleep (200);
        byte in_buf [] = new byte [65536];
        int bytes_read = inp_str.read (in_buf, 0, 65536);
        Utils.logd ("version response received from Wifi input bytes_read: " + bytes_read);
        if (bytes_read > 0) {
          Utils.hex_dump ("AA WiFi: ", in_buf, bytes_read);
        }

        out_str.close ();
        inp_str.close ();

        serverSocket.close();
*/
                return (null);// f.getAbsolutePath();
            } catch (Throwable e) {//IOException e) {
                Log.e("WiFiStartTask", e.getMessage());
                return null;
            }
        }

        // Start activity that can handle the JPEG image
        @Override
        protected void onPostExecute(Object result) {//String result) {
            Utils.logd("result: " + result);
//      if (result != null) {
//        //statusText.setText("File copied - " + result);
//        Intent intent = new Intent();
//        intent.setAction(android.content.Intent.ACTION_VIEW);
//        intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//        context.startActivity(intent);
//      }
        }
    }


    //Intent intent = new Intent (this, HeadUnitActivity.class);
    //intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Intent.FLAG_ACTIVITY_NEW_TASK |
    //startActivity (intent);


    private static boolean wifi_starting = false;

    private void wifi_long_start() {

        if (wifi_starting) {
            Utils.loge("wifi_starting: " + wifi_starting);
            return;
        }
        wifi_starting = true;


//wifi_init ();

        //Utils.sys_run ("am start -a android.nfc.action.NDEF_DISCOVERED -t application/com.google.android.gms.car -n com.google.android.gms/.car.FirstActivity -f 32768 & ", true);

        int ret = mUsbTransport.jni_aap_start();                              // Start JNI Android Auto Protocol and Main Thread
        Utils.logd("jni_aap_start() ret: " + ret);

        m_tcp_connected = true;
        wifi_starting = false;

        //Utils.logd ("sys_ui_hide (): " + sys_ui_hide ());

    }


    private void car_mode_start() {
        try {
            if (m_uim_mgr == null)
                m_uim_mgr = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (m_uim_mgr != null) {
                m_uim_mgr.enableCarMode(0);         // API 21+: UiModeManager.ENABLE_CAR_MODE_ALLOW_SLEEP
                //m_uim_mgr.enableCarMode (UiModeManager.ENABLE_CAR_MODE_GO_CAR_HOME);
                //m_uim_mgr.setNightMode (UiModeManager.MODE_NIGHT_YES);
                //m_uim_mgr.setNightMode (UiModeManager.MODE_NIGHT_AUTO);
            }
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }
        //Utils.logd ("sys_ui_hide (): " + sys_ui_hide ());
    }

    private void car_mode_stop() {
        try {
            //m_uim_mgr = (UiModeManager) getSystemService (UI_MODE_SERVICE);
            if (m_uim_mgr != null) {                                          // If was used to enable...
                m_uim_mgr.disableCarMode(0);//UiModeManager.DISABLE_CAR_MODE_GO_HOME);//0);
                //m_uim_mgr.setNightMode (UiModeManager.MODE_NIGHT_AUTO);
                //m_uim_mgr = null;
                Utils.logd("OK disableCarMode");
            }
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }
        Utils.logd("After disableCarMode");
    }


    private Animation m_ani_button = null;

    private void ani(View v) {
        if (v != null)
            v.startAnimation(m_ani_button);
    }

// pm grant org.jtb.alogcat.donate android.permission.READ_LOGS
// chmod 04755 /system/bin/logcat
// logcat k9:V *:S AndroidRuntime:E

    private int click_ctr = 0;
    private long click_start_ms = 0;

    private View.OnClickListener short_click_lstnr = new View.OnClickListener() {
        public void onClick(View v) {

            int log_clicks = 3;//7;

            Utils.logd("view: " + v);
            //ani (v);                                                          // Animate button
            if (v == null) {
                Utils.loge("view: " + v);
            } else if (v == m_tv_log) {
                //m_gui_act.showDialog (GUI_MENU_DIALOG);
                if (click_ctr <= 0) {
                    click_ctr = 1;
                    click_start_ms = Utils.tmr_ms_get();
                    return;
                }

                if (click_ctr <= log_clicks - 1) {    // If still counting clicks, and/or may trigger...
                    if (click_start_ms + 3000 < Utils.tmr_ms_get()) { // If took longer than 3 seconds...
                        click_ctr = 1;
                        click_start_ms = Utils.tmr_ms_get();
                        return;
                    }
                }
                if (click_ctr == log_clicks - 1) {
                    click_ctr++;
                    logs_email();
                    //click_ctr = 0;    // Zero'd when logs processed
                } else if (click_ctr < log_clicks - 1) {   // Protect against buffering more clicks
                    click_ctr++;
                }
            }
        }
    };


    // Video Test:

/*
  private void old_video_test_start (final boolean started) {
    class vs_task implements Runnable {                                 // !!!! Doesn't work !!!!
      boolean started;
      vs_task (boolean s) { started = s; }
        public void run() {
          video_test (started);
        }
    }
    runOnUiThread (new vs_task (started));
  }
*/

    //*
    private void video_test_start(final boolean started) {

        ui_video_started_set(true);                                        // Enable video/disable log view

        Thread thread_vs = new Thread(run_vs, "run_vs");
        //Utils.logd ("thread_vs: " + thread_vs);
        if (thread_vs == null)
            ;//Utils.loge ("thread_vs == null");
        else {
            //thread_vs_active = true;
            java.lang.Thread.State thread_state = thread_vs.getState();
            if (thread_state == java.lang.Thread.State.NEW || thread_state == java.lang.Thread.State.TERMINATED) {
                ////Utils.logd ("thread priority: " + thread_vs.getPriority ());   // Get 5
                thread_vs.start();
            }
            //else
            //  Utils.loge ("thread_vs thread_state: " + thread_state);
        }
    }

    private final Runnable run_vs = new Runnable() {
        public void run() {
            //Utils.logd ("run_vs");
            boolean started = true;
            video_test(started);
        }
    };


    int h264_after_get(byte[] ba, int idx) {
        idx += 4; // Pass 0, 0, 0, 1
        for (; idx < ba.length - 4; idx++) {
            if (idx > 24)   // !!!! HACK !!!! else 0,0,0,1 indicates first size 21, instead of 25
                if (ba[idx] == 0 && ba[idx + 1] == 0 && ba[idx + 2] == 0 && ba[idx + 3] == 1)
                    return (idx);
        }
        return (-1);
    }

    String h264_full_filename = null;

    private void video_test(final boolean started) {
        if (h264_full_filename == null && Utils.file_get("/sdcard/Download/husam.h264"))
            h264_full_filename = "/sdcard/Download/husam.h264";
        if (h264_full_filename == null && Utils.file_get("/sdcard/Download/husam.mp4"))
            h264_full_filename = "/sdcard/Download/husam.mp4";
        if (h264_full_filename == null)
            h264_full_filename = Utils.res_file_create(m_context, R.raw.husam_h264, "husam.h264");
        if (h264_full_filename == null)
            h264_full_filename = "/data/data/ca.yyx.hu/files/husam.h264";

        byte[] ba = Utils.file_read_16m(h264_full_filename);             // Read entire file, up to 16 MB to byte array ba
        ByteBuffer bb;// = ByteBuffer.wrap (ba);

        int size = (int) Utils.file_size_get(h264_full_filename);
        int left = size;
        int idx = 0;
        int max_chunk_size = 65536 * 4;//16384;


        int chunk_size = max_chunk_size;
        int after = 0;
        for (idx = 0; idx < size && left > 0; idx = after) {

            after = h264_after_get(ba, idx);                               // Get index of next packet that starts with 0, 0, 0, 1
            if (after == -1 && left <= max_chunk_size) {
                after = size;
                //Utils.logd ("Last chunk  chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
            } else if (after <= 0 || after > size) {
                Utils.loge("Error chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);
                return;
            }

            chunk_size = after - idx;

            byte[] bc = new byte[chunk_size];                               // Create byte array bc to hold chunk
            int ctr = 0;
            for (ctr = 0; ctr < chunk_size; ctr++)
                bc[ctr] = ba[idx + ctr];                                      // Copy chunk_size bytes from byte array ba at idx to byte array bc

            //Utils.logd ("chunk_size: " + chunk_size + "  idx: " + idx + "  after: " + after + "  size: " + size + "  left: " + left);

            idx += chunk_size;
            left -= chunk_size;

            bb = ByteBuffer.wrap(bc);                                        // Wrap chunk byte array bc to create byte buffer bb

            media_decode(bb);                                                // Decode audio or H264 video content
            Utils.ms_sleep(20);                                             // Wait a frame
        }

    }
//*/

    private boolean video_started = false;

    public void ui_video_started_set(boolean started) {                  // Called directly from UsbTransport:jni_aap_start() because runOnUiThread() won't work
        // Also from: video_started_set(), video_test_start(), wifi_start
        if (video_started == started) {                                       // If no change...
            return;
        }

        Utils.logd("video_started: " + video_started + "started: " + started);

        try {
            if (started) {                                                    // If starting video...
                Utils.logd("sys_ui_hide (): " + sys_ui_hide());
                m_fl_tv_vid.setVisibility(View.VISIBLE);                     // Enable  video
                m_ll_tv_log.setVisibility(View.GONE);                        // Disable log
//                if (m_scr_land && m_scr_wid / m_scr_hei < 1.5)                  // If closer to 4:3 than 16:9...    16:9 = 1.777     //  4:3 = 1.333
//                    m_ll_tv_ext.setVisibility(View.VISIBLE);                     // Enable  aspect ratio bar
//                else
//                    m_ll_tv_ext.setVisibility(View.GONE);
            } else {
                //m_fl_tv_vid.setVisibility     (View.GONE);                    // Disable video
                m_ll_tv_log.setVisibility(View.VISIBLE);                     // Enable  log
//                m_ll_tv_ext.setVisibility(View.GONE);                        // Disable aspect ratio bar
            }
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }

        video_started = started;
    }


    private boolean video_started_direct = false;//true;  ui_video_started_set(12481): Throwable: android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
    public boolean disable_video_started_set = false;//true;

    private void video_started_set(final boolean started) {              // Not used because runOnUiThread() won't work ?

        if (video_started == started)                                       // If no change...
            return;

        Utils.logd("disable_video_started_set: " + disable_video_started_set);

        if (disable_video_started_set)
            return;

        if (video_started_direct) {
            ui_video_started_set(started);
            return;
        }

        class vs_task implements Runnable {
            boolean started;

            vs_task(boolean s) {
                started = s;
            }

            public void run() {
                ui_video_started_set(started);
            }
        }
        runOnUiThread(new vs_task(started));

//    Utils.ms_sleep (100);  // Wait for it ?

        //Utils.logd ("sys_ui_hide (): " + sys_ui_hide ());

    }

    private boolean sys_ui_enable = true;
    //private boolean sys_ui_enable = false;

    public boolean sys_ui_hide() {
        if (!sys_ui_enable)
            return (false);

        if (m_dl_drawer != null)
            m_dl_drawer.closeDrawer(Gravity.LEFT);
        else
            Utils.loge("m_dl_drawer == null");

        // Set the IMMERSIVE flag. Set the content to appear under the system bars so that the content doesn't resize when the system bars hide and show.
        m_ll_main = findViewById(android.R.id.content);
        if (m_ll_main != null)
            m_ll_main.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);//_STICKY);
        else {
            Utils.loge("m_ll_main == null");
            return (false);
        }
        return (true);
    }

    // Show the system bars by removing all the flags except for the ones that make the content appear under the system bars.
    public void sys_ui_show() {
        if (m_ll_main != null)
            m_ll_main.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void all_stop() {

        if (!starting_car_mode) {
            sys_ui_show();

            car_mode_stop();
        }

        wifi_pause();

        audio_record_stop();

        out_audio_stop(UsbTransport.AA_CH_AUD);                                         // In case Byebye terminates without proper audio stop
        out_audio_stop(UsbTransport.AA_CH_AU1);
        out_audio_stop(UsbTransport.AA_CH_AU2);

        video_record_stop();

        video_started_set(false);

        if (mUsbTransport != null)
            if (!disable_usb)
                mUsbTransport.transport_stop();

        wifi_deinit();

        try {
            if (m_wakelock != null)
                m_wakelock.release();
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }

    }


    // Touch:

    //private double        m_vid_wid  = 0;
    //private double        m_vid_hei  = 0;

    private double vid_wid_get() {                                       // Called only by touch_send()
        double vid_wid = 0;
        vid_wid = m_tv_vid.getWidth();

    /*Utils.logd ("vid_wid: "  + vid_wid);
    vid_wid = m_tv_vid.getMeasuredWidth ();                             // Same value
    Utils.logd ("vid_wid: "  + vid_wid);*/
        return (vid_wid);
    }

    private double vid_hei_get() {                                       // Called only by touch_send()
        double vid_hei = 0;
        vid_hei = m_tv_vid.getHeight();

    /*Utils.logd ("vid_hei: "  + vid_hei);
    vid_hei = m_tv_vid.getMeasuredHeight ();                            // Same value
    Utils.logd ("vid_hei: "  + vid_hei);*/
        return (vid_hei);
    }


    private void touch_send(MotionEvent event) {
        //Utils.logd ("event: " + event);

        int x = (int) (event.getX(0) / (vid_wid_get() / m_virt_vid_wid));
        int y = (int) (event.getY(0) / (vid_hei_get() / m_virt_vid_hei));

        if (x < 0 || y < 0 || x >= 65535 || y >= 65535) {   // Infinity if vid_wid_get() or vid_hei_get() return 0
            Utils.loge("Invalid x: " + x + "  y: " + y);
            return;
        }

        byte aa_action = 0;
        int me_action = event.getActionMasked();
        switch (me_action) {
            case MotionEvent.ACTION_DOWN:
                Utils.logd("event: " + event + " (ACTION_DOWN)    x: " + x + "  y: " + y);
                aa_action = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                Utils.logd("event: " + event + " (ACTION_MOVE)    x: " + x + "  y: " + y);
                aa_action = 2;
                break;
            case MotionEvent.ACTION_CANCEL:
                Utils.logd("event: " + event + " (ACTION_CANCEL)  x: " + x + "  y: " + y);
                aa_action = 1;
                break;
            case MotionEvent.ACTION_UP:
                Utils.logd("event: " + event + " (ACTION_UP)      x: " + x + "  y: " + y);
                aa_action = 1;
                break;
            default:
                Utils.loge("event: " + event + " (Unknown: " + me_action + ")  x: " + x + "  y: " + y);
                return;
        }
        if (mUsbTransport != null)
            mUsbTransport.touch_send(aa_action, x, y);
    }


    // Video: TextureView

    @Override
    // Called after onCreate(), which calls "m_tv_vid.setSurfaceTextureListener (HeadUnitActivity.this);"
    public void onSurfaceTextureAvailable(SurfaceTexture sur_tex, int width, int height) {
        Utils.logd("--- sur_tex: " + sur_tex + "  m_sur_tex: " + m_sur_tex + "  width: " + width + "  height: " + height);  // N9: width: 2048  height: 1253

        //if (m_sur_tex != null)    //onRestart
        //  return;

        m_sur_tex = sur_tex;

        if (height > 1080) {                                              // Limit surface height to 1080 or N7 2013 won't work: width: 1920  height: 1104    Screen 1200x1920, nav = 96 pixels
            Utils.loge("height: " + height);
            height = 1080;
        }
        try {
            m_codec = MediaCodec.createDecoderByType("video/avc");       // Create video codec: ITU-T H.264 / ISO/IEC MPEG-4 Part 10, Advanced Video Coding (MPEG-4 AVC)
        } catch (Throwable t) {
            Utils.loge("Throwable creating video/avc decoder: " + t);
        }
        try {
            m_codec_buf_info = new BufferInfo();                         // Create Buffer Info
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            m_codec.configure(format, new Surface(sur_tex), null, 0);               // Configure codec for H.264 with given width and height, no crypto and no flag (ie decode)
            m_codec.start();                                             // Start codec
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture sur_tex, int width, int height) {        // ignore
        Utils.logd("--- sur_tex: " + sur_tex + "  m_sur_tex: " + m_sur_tex + "  width: " + width + "  height: " + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture sur_tex) {
        Utils.logd("--- sur_tex: " + sur_tex + "  m_sur_tex: " + m_sur_tex);
        return (false);                                                     // Prevent destruction of SurfaceTexture
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture sur_tex) {             // Called many times per second when video changes, so ignore
        //Utils.logd ("--- sur_tex: " + sur_tex + "  m_sur_tex: " + m_sur_tex);
    }


    private void codec_stop() {
        if (m_codec != null)
            m_codec.stop();                                                  // Stop codec
        m_codec = null;
        m_codec_input_bufs = null;
        m_codec_buf_info = null;
    }

    private boolean codec_input_provide(ByteBuffer content) {            // Called only by media_decode() with new NAL unit in Byte Buffer
        if (Utils.ena_log_verbo)
            Utils.logv("content: " + content);    //Utils.logd ("content.position (): " + content.position () + "  content.limit (): " + content.limit ());

        try {
            final int index = m_codec.dequeueInputBuffer(1000000);           // Get input buffer with 1 second timeout
            if (index < 0) {
                return (false);                                                 // Done with "No buffer" error
            }
            if (m_codec_input_bufs == null) {
                m_codec_input_bufs = m_codec.getInputBuffers();                // Set m_codec_input_bufs if needed
            }

            final ByteBuffer buffer = m_codec_input_bufs[index];
            final int capacity = buffer.capacity();
            buffer.clear();
            if (content.remaining() <= capacity) {                           // If we can just put() the content...
                buffer.put(content);                                           // Put the content
            } else {                                                            // Else... (Should not happen ?)
                Utils.loge("content.hasRemaining (): " + content.hasRemaining() + "  capacity: " + capacity);

                int limit = content.limit();
                content.limit(content.position() + capacity);                 // Temporarily set constrained limit
                buffer.put(content);
                content.limit(limit);                                          // Restore original limit
            }
            buffer.flip();                                                   // Flip buffer for reading
            //Utils.logd ("buffer.position (): " + buffer.position () + "  buffer.limit (): " + buffer.limit ());

            m_codec.queueInputBuffer(index, 0, buffer.limit(), 0, 0);       // Queue input buffer for decoding w/ offset=0, size=limit, no microsecond timestamp and no flags (not end of stream)
            return (true);                                                    // Processed
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }
        return (false);                                                     // Error: exception
    }

    private void codec_output_consume() {                                // Called only by media_decode() after codec_input_provide()
        //Utils.logd ("");
        int index = -777;
        for (; ; ) {                                                          // Until no more buffers...
            index = m_codec.dequeueOutputBuffer(m_codec_buf_info, 0);        // Dequeue an output buffer but do not wait
            if (index >= 0)
                m_codec.releaseOutputBuffer(index, true /*render*/);           // Return the buffer to the codec
            else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)         // See this 1st shortly after start. API >= 21: Ignore as getOutputBuffers() deprecated
                Utils.logd("INFO_OUTPUT_BUFFERS_CHANGED");
            else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)          // See this 2nd shortly after start. Output format changed for subsequent data. See getOutputFormat()
                Utils.logd("INFO_OUTPUT_FORMAT_CHANGED");
            else if (index == MediaCodec.INFO_TRY_AGAIN_LATER)
                break;
            else
                break;
        }
        if (index != MediaCodec.INFO_TRY_AGAIN_LATER)
            Utils.loge("index: " + index);
    }

/*
  private int camcorder_profile_get () {                                // Get the recording profile to log     Always shows support for all
    //CamcorderProfile profile = null;
    int prof = 0;

    if (CamcorderProfile.hasProfile (CamcorderProfile.QUALITY_1080P)) {
      if (prof < 1080)
        prof = 1080;
      Utils.logd ("1080p");//profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
    }
    if (CamcorderProfile.hasProfile (CamcorderProfile.QUALITY_720P)) {
      if (prof < 720)
        prof = 720;
      Utils.logd (" 720p");//profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
    }
    if (CamcorderProfile.hasProfile (CamcorderProfile.QUALITY_480P)) {
      if (prof < 480)
        prof = 480;
      Utils.logd (" 480p");//profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
    }
    if (CamcorderProfile.hasProfile (CamcorderProfile.QUALITY_HIGH)) {
      if (prof < 240)
        prof = 240; // !!!!!!!! ??
      Utils.logd ("HIGH");//profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
    }
    //Utils.loge ("Unknown");//

    //Utils.loge ("profile: " + profile);
    return (prof);
  }
*/

    // Video recording:
//*
    private boolean video_recording = false;
    private FileOutputStream video_record_fos = null;

    private void video_record_stop() {
        try {
            if (video_record_fos != null)
                video_record_fos.close();                                                     // Close output file
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
            //return;
        }
        video_record_fos = null;
        video_recording = false;
    }

    private void video_record_write(ByteBuffer content) {
        // ffmpeg -i 2015-04-29-00_38_16.mp4 -vcodec copy -an -bsf:v h264_mp4toannexb  aa.h264
        if (!video_recording) {
            try {
                video_record_fos = this.openFileOutput("/sdcard/hurec.h264", Context.MODE_WORLD_READABLE);//, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
            } catch (Throwable t) {
                //Utils.loge ("Throwable: " + t);
                Utils.loge("Throwable: " + t);
                //return;
            }
            try {
                if (video_record_fos == null)
                    video_record_fos = this.openFileOutput("hurec.h264", Context.MODE_WORLD_READABLE);//, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
            } catch (Throwable t) {
                //Utils.loge ("Throwable: " + t);
                Utils.loge("Throwable: " + t);
                return;
            }

            video_recording = true;
        }

        int pos = content.position();
        int siz = content.remaining();
        int last = pos + siz - 1;
        byte[] ba = content.array();
        if (ba == null) {
            Utils.loge("ba == null...   pos: " + pos + "  siz: " + siz + " (" + Utils.hex_get(siz) + ")  last: " + last);
            return;
        }
        byte b1 = ba[pos + 3];
        byte bl = ba[last];
        if (Utils.ena_log_verbo)
            Utils.logv("pos: " + pos + "  siz: " + siz + "  last: " + last + " (" + Utils.hex_get(b1) + ")  b1: " + b1 + "  bl: " + bl + " (" + Utils.hex_get(bl) + ")");

        try {
            video_record_fos.write(ba, pos, siz);                                               // Copy input to output file
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
            return;
        }
    }
//*/

    // Audio recording:
//*
    private boolean audio_recording = false;
    private FileOutputStream audio_record_fos = null;

    private void audio_record_stop() {
        try {
            if (audio_record_fos != null)
                audio_record_fos.close();                                                     // Close output file
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
            //return;
        }
        audio_record_fos = null;
        audio_recording = false;
    }

    private void audio_record_write(ByteBuffer content) {    // ffmpeg -i 2015-04-29-00_38_16.mp4 -vcodec copy -an -bsf:v h264_mp4toannexb  aa.h264
        if (!audio_recording) {
//*  Throwable: java.lang.IllegalArgumentException: File /sdcard/hurec.pcm contains a path separator
            try {
                audio_record_fos = this.openFileOutput("/sdcard/hurec.pcm", Context.MODE_WORLD_READABLE);//, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
                Utils.logw("audio_record_fos: " + audio_record_fos);
            } catch (Throwable t) {
                Utils.logw("Throwable: " + t);
                //return;
            }
//*/
            try {
                if (audio_record_fos == null)     // -> /data/data/ca.yyx.hu/files/hurec.pcm
                    audio_record_fos = this.openFileOutput("hurec.pcm", Context.MODE_WORLD_READABLE);//, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
                Utils.logw("audio_record_fos: " + audio_record_fos);
            } catch (Throwable t) {
                //Utils.loge ("Throwable: " + t);
                Utils.loge("Throwable: " + t);
                return;
            }

            audio_recording = true;
        }

        int pos = content.position();
        int siz = content.remaining();
        int last = pos + siz - 1;
        byte[] ba = content.array();
        if (ba == null) {
            Utils.loge("ba == null...   pos: " + pos + "  siz: " + siz + " (" + Utils.hex_get(siz) + ")  last: " + last);
            return;
        }
        byte b1 = ba[pos + 3];
        byte bl = ba[last];
        if (Utils.ena_log_verbo)
            Utils.logv("pos: " + pos + "  siz: " + siz + "  last: " + last + " (" + Utils.hex_get(b1) + ")  b1: " + b1 + "  bl: " + bl + " (" + Utils.hex_get(bl) + ")");

        Utils.loge("pos: " + pos + "  siz: " + siz + "  last: " + last + " (" + Utils.hex_get(b1) + ")  b1: " + b1 + "  bl: " + bl + " (" + Utils.hex_get(bl) + ")");

        try {
            audio_record_fos.write(ba, pos, siz);                            // Copy input to output file
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
            return;
        }
    }
//*/

    // Audio:

    private int m_mic_channels = 1;
    private int m_mic_samplerate = 16000;
    private int m_mic_totbufsize = 32768;
    public static final int m_mic_bufsize = 8192;
    private int m_mic_src = 0;  // Default
    private AudioRecord m_mic_audiorecord = null;

    private int m_out_bufsize0 = 32768;            // 186 ms at 44.1K stereo 16 bit        //5120 * 16;//65536;
    private int m_out_bufsize1 = 4096;//8192;//32768;//2048;
    private int m_out_bufsize2 = 4096;//8192;//32768;//2048;
    private static final int m_out_audio_stream = AudioManager.STREAM_MUSIC;
    private int m_out_channels0 = 2;
    private int m_out_channels1 = 1;
    private int m_out_channels2 = 1;
    private int m_out_samplerate0 = 48000;//44100;            // Default that all devices can handle =  44.1Khz CD samplerate
    private int m_out_samplerate1 = 16000;
    private int m_out_samplerate2 = 16000;
    private AudioTrack m_out_audiotrack0 = null;
    private AudioTrack m_out_audiotrack1 = null;
    private AudioTrack m_out_audiotrack2 = null;

    private int chan_out_get(int channels) {
        if (channels == 1)
            return (AudioFormat.CHANNEL_OUT_MONO);
        else
            return (AudioFormat.CHANNEL_OUT_STEREO);
    }

    private int chan_in_get(int channels) {
        if (channels == 1)
            return (AudioFormat.CHANNEL_IN_MONO);
        else
            return (AudioFormat.CHANNEL_IN_STEREO);
    }

    private void mic_audio_pause() {
        //if (m_mic_audiorecord != null)
        //  m_mic_audiorecord.pause ();                                       // Pause Audiotrack
    }

    private boolean thread_mic_audio_active = false;
    private Thread thread_mic_audio = null;

    public void mic_audio_stop() {
        Utils.logd("thread_mic_audio: " + thread_mic_audio + "  thread_mic_audio_active: " + thread_mic_audio_active);
        mic_audio_len = 0;
        if (thread_mic_audio_active) {
            thread_mic_audio_active = false;
            if (thread_mic_audio != null)
                thread_mic_audio.interrupt();
        }

        if (m_mic_audiorecord != null) {
            m_mic_audiorecord.stop();
            m_mic_audiorecord.release();                                     // Release AudioTrack resources
            m_mic_audiorecord = null;
        }
    }

    private boolean disable_mic = false;//true;

    private int mic_audio_start() {
        if (disable_mic)
            return (-33);

        try {
            m_mic_audiorecord = new AudioRecord(m_mic_src, m_mic_samplerate, chan_in_get(m_mic_channels), AudioFormat.ENCODING_PCM_16BIT, m_mic_totbufsize);
            int rec_state = m_mic_audiorecord.getState();
            Utils.logd("rec_state: " + rec_state);
            if (rec_state == AudioRecord.STATE_INITIALIZED) {                 // If Init OK...
                Utils.logd("Success with m_mic_src: " + m_mic_src);
                m_mic_audiorecord.startRecording();                            // Start input

                thread_mic_audio = new Thread(run_mic_audio, "mic_audio");
                Utils.logd("thread_mic_audio: " + thread_mic_audio);
                if (thread_mic_audio == null)
                    Utils.loge("thread_mic_audio == null");
                else {
                    thread_mic_audio_active = true;
                    java.lang.Thread.State thread_state = thread_mic_audio.getState();
                    if (thread_state == java.lang.Thread.State.NEW || thread_state == java.lang.Thread.State.TERMINATED) {
                        //Utils.logd ("thread priority: " + thread_mic_audio.getPriority ());   // Get 5
                        thread_mic_audio.start();
                    } else
                        Utils.loge("thread_mic_audio thread_state: " + thread_state);
                }

                return (0);
            }
        } catch (Exception e) {
            Utils.loge("Exception: " + e);  // "java.lang.IllegalArgumentException: Invalid audio source."
            m_mic_audiorecord = null;
            return (-2);
        }
        m_mic_audiorecord = null;
        return (-1);
    }

    private byte[] mic_audio_buf = new byte[m_mic_bufsize];
    private int mic_audio_len = 0;
    private final Runnable run_mic_audio = new Runnable() {
        public void run() {
            Utils.logd("run_mic_audio");
            //native_priority_set (pcm_priority);
/*                                                                      // Setup temp vars before loop to minimize garbage collection   (!! but aud_mod() has this issue)
      int bufs = 0;
      int len = 0;
      int len_written = 0;
      int new_len = 0;
*/

            mic_audio_len = 0;                                                  // Reset for next read into single buffer

            while (thread_mic_audio_active) {                                 // While Thread should be active...

                while (mic_audio_len > 0)                                       // While single buffer is in use...
                    Utils.ms_sleep(3);

                mic_audio_len = phys_mic_audio_read(mic_audio_buf, m_mic_bufsize);
            }
        }
    };

    public int mic_audio_read(byte[] aud_buf, int max_len) {

        if (!thread_mic_audio_active)                                      // If mic audio thread not active...
            mic_audio_start();                                               // Start it

        if (!thread_mic_audio_active)                                      // If mic audio thread STILL not active...
            return (-1);                                                      // Done with error

        if (mic_audio_len <= 0)
            return (mic_audio_len);

        int len = mic_audio_len;
        if (len > max_len)
            len = max_len;
        int ctr = 0;
        for (ctr = 0; ctr < len; ctr++)
            aud_buf[ctr] = mic_audio_buf[ctr];
        mic_audio_len = 0;                                                  // Reset for next read into single buffer
        return (len);
    }

    private int phys_mic_audio_read(byte[] aud_buf, int max_len) {
        int len = 0;
        if (m_mic_audiorecord == null) {
            return (len);
        }
        len = m_mic_audiorecord.read(aud_buf, 0, max_len);//m_mic_bufsize);
        if (len <= 0) {                                               // If no audio data...
            if (len == android.media.AudioRecord.ERROR_INVALID_OPERATION)   // -3
                Utils.logd("get expected interruption error due to shutdown: " + len);
                // -2: ERROR_BAD_VALUE
            else
                Utils.loge("get error: " + len);
            return (len);
        }
        return (len);
    }

    /*private void out_audio_pause () {
      if (m_out_audiotrack0 != null)
        m_out_audiotrack0.pause ();                                        // Pause Audiotrack
    }*/
    public void out_audio_stop(int chan) {

        if (enable_audio_recycle) {
            return;
        }

        AudioTrack out_audiotrack = null;
        if (chan == UsbTransport.AA_CH_AUD) {
            out_audiotrack = m_out_audiotrack0;
        } else if (chan == UsbTransport.AA_CH_AU1) {
            out_audiotrack = m_out_audiotrack1;
        } else if (chan == UsbTransport.AA_CH_AU2) {
            out_audiotrack = m_out_audiotrack2;
        } else {
            Utils.loge("!!!!");
            return;
        }
        if (out_audiotrack == null) {
            Utils.logd("out_audiotrack == null");
            return;
        }

        long ms_tmo = Utils.tmr_ms_get() + 2000;                        // Wait for maximum of 2 seconds
        int last_frames = 0;
        int curr_frames = 1;

        out_audiotrack.flush();
        // While audio still running and 2 second wait timeout has not yet elapsed...
        while (last_frames != curr_frames && Utils.tmr_ms_get() < ms_tmo) {
            Utils.ms_sleep(150);//300);//100);                                          // 100 ms = time for about 3 KBytes (1.5 buffers of 2Bytes (1024 samples) as used for 16,000 samples per second
            last_frames = curr_frames;
            curr_frames = out_audiotrack.getPlaybackHeadPosition();
            Utils.logd("curr_frames: " + curr_frames + "  last_frames: " + last_frames);
        }

        if (enable_audio_recycle) {
//      return;
        }
        out_audiotrack.stop();
        out_audiotrack.release();                                      // Release AudioTrack resources
        out_audiotrack = null;
        if (chan == UsbTransport.AA_CH_AUD)
            m_out_audiotrack0 = null;
        else if (chan == UsbTransport.AA_CH_AU1)
            m_out_audiotrack1 = null;
        else if (chan == UsbTransport.AA_CH_AU2)
            m_out_audiotrack2 = null;
    }

    private boolean enable_audio_recycle = false;//true;

    private AudioTrack out_audio_start(int chan) {
        AudioTrack out_audiotrack = null;
        //Utils.logd (".... m_out_audiotrack0: " + m_out_audiotrack0);

        if (enable_audio_recycle) {
            if (chan == UsbTransport.AA_CH_AUD && m_out_audiotrack0 != null)
                return (m_out_audiotrack0);
            if (chan == UsbTransport.AA_CH_AU1 && m_out_audiotrack1 != null)
                return (m_out_audiotrack1);
            if (chan == UsbTransport.AA_CH_AU2 && m_out_audiotrack2 != null)
                return (m_out_audiotrack2);
        }
        try {
            if (chan == UsbTransport.AA_CH_AUD)
                m_out_audiotrack0 = out_audiotrack = new AudioTrack(m_out_audio_stream, m_out_samplerate0, chan_out_get(m_out_channels0), AudioFormat.ENCODING_PCM_16BIT, m_out_bufsize0, AudioTrack.MODE_STREAM);
            else if (chan == UsbTransport.AA_CH_AU1)
                m_out_audiotrack1 = out_audiotrack = new AudioTrack(m_out_audio_stream, m_out_samplerate1, chan_out_get(m_out_channels1), AudioFormat.ENCODING_PCM_16BIT, m_out_bufsize1, AudioTrack.MODE_STREAM);
            else if (chan == UsbTransport.AA_CH_AU2)
                m_out_audiotrack2 = out_audiotrack = new AudioTrack(m_out_audio_stream, m_out_samplerate2, chan_out_get(m_out_channels2), AudioFormat.ENCODING_PCM_16BIT, m_out_bufsize2, AudioTrack.MODE_STREAM);
            else
                return (out_audiotrack);

            if (out_audiotrack == null)
                Utils.loge("out_audiotrack == null");
            else
                out_audiotrack.play();                                         // Start output
        } catch (Throwable e) {
            Utils.loge("Throwable: " + e);
            e.printStackTrace();
        }
        return (out_audiotrack);
    }

    private void out_audio_write(int chan, byte[] aud_buf, int len) {
        AudioTrack out_audiotrack = null;
        if (chan == UsbTransport.AA_CH_AUD) {
            out_audiotrack = m_out_audiotrack0;
        } else if (chan == UsbTransport.AA_CH_AU1) {
            out_audiotrack = m_out_audiotrack1;
        } else if (chan == UsbTransport.AA_CH_AU2) {
            out_audiotrack = m_out_audiotrack2;
        } else {
            Utils.loge("!!!!");
            return;
        }

        if (out_audiotrack == null) {
            out_audiotrack = out_audio_start(chan);
            if (out_audiotrack == null)
                return;
        }
/*
    int len_written = 0;
    int new_len = 0;
                                                                        // Write head buffer to audiotrack  All parameters in bytes (but could be all in shorts)
    new_len = out_audiotrack.write (aud_buf, len_written, len - len_written);
    if (new_len > 0)
      len_written += new_len;                                           // If we wrote ANY bytes, update total length written
    else
      Utils.loge ("Error Audiotrack write: " + new_len);
*/
        int written = out_audiotrack.write(aud_buf, 0, len);
        if (written == len)
            Utils.logv("OK Audiotrack written: " + written);
        else
            Utils.loge("Error Audiotrack written: " + written + "  len: " + len);
    }


    public void media_decode(ByteBuffer content) {                       // Decode audio or H264 video content. Called only by video_test() & UsbTransport.aa_cmd_send()

        video_started_set(true);                                           // Start video if needed

        if (Utils.ena_log_verbo)
            Utils.logv("content: " + content);
        //else
        //  Utils.logd ("content: " + content);

        if (content == null) {                                              // If no content
            Utils.loge("!!!");
            return;
        }

        int pos = content.position();
        if (pos != 0)                                                       // For content byte array we assume position = 0 so test and log error if position not 0
            Utils.loge("pos != 0  change hardcode 0, 1, 2, 3");

        int siz = content.remaining();
        int last = pos + siz - 1;
        byte[] ba = content.array();                                      // Create content byte array

        //byte b1 = ba [pos + 3];
        //byte bl = ba [last];
        //Utils.logd ("pos: " + pos + "  siz: " + siz + "  last: " + last + " (" + Utils.hex_get (b1) + ")  b1: " + b1 + "  bl: " + bl + " (" + Utils.hex_get (bl) + ")");

        if (ba[0] == 0 && ba[1] == 0 && ba[2] == 0 && ba[3] == 1) {
            Utils.logv("H264 video");
        } else {
            Utils.logv("Audio");
            if (Utils.quiet_file_get("/sdcard/hureca"))                     // If audio record flag file exists...
                audio_record_write(content);
            else if (audio_recording)                                         // Else if was recording... (file must have been removed)
                audio_record_stop();

            if (siz <= 2048 + 96)
                out_audio_write(UsbTransport.AA_CH_AU1, ba, pos + siz);                     // Position always 0 so just use siz as len ?
            else
                out_audio_write(UsbTransport.AA_CH_AUD, ba, pos + siz);                     // Position always 0 so just use siz as len ?
            return;
        }

        if (Utils.quiet_file_get("/sdcard/hurecv"))                       // If video record flag file exists...
            video_record_write(content);
        else if (video_recording)                                           // Else if was recording... (file must have been removed)
            video_record_stop();


        synchronized (m_surf_codec_lock) {
            if (m_codec == null) {
                return;
            }

            while (content.hasRemaining()) {                                 // While there is remaining content...

                if (!codec_input_provide(content)) {                          // Process buffer; if no available buffers...
                    Utils.loge("Dropping content because there are no available buffers.");
                    return;
                }
                if (content.hasRemaining())                                    // Never happens now
                    Utils.loge("content.hasRemaining ()");

                codec_output_consume();                                        // Send result to video codec
            }
        }
    }


    // Debug logs Email:

    private boolean new_logs = true;
    String logfile = "/sdcard/hulog.txt";//"/data/data/ca.yyx.hu/hu.txt";

    String cmd_build(String cmd) {
        String cmd_head = " ; ";
        String cmd_tail = " >> " + logfile;
        return (cmd_head + cmd + cmd_tail);
    }

    String new_logs_cmd_get() {
        String cmd = "rm -f " + logfile;

        //cmd += cmd_build ("cat /data/data/ca.yyx.hu/shared_prefs/prefs.xml");
        cmd += cmd_build("id");
        cmd += cmd_build("uname -a");
        cmd += cmd_build("getprop");
        cmd += cmd_build("ps");
        cmd += cmd_build("lsmod");
        // !! Wildcard * can lengthen command line unexpectedly !!
        cmd += cmd_build("modinfo /system/vendor/lib/modules/* /system/lib/modules/*");
        //cmd += cmd_build ("dumpsys");                                     // 11 seconds on MotoG
        //cmd += cmd_build ("dumpsys audio");
        //cmd += cmd_build ("dumpsys media.audio_policy");
        //cmd += cmd_build ("dumpsys media.audio_flinger");
        cmd += cmd_build("dumpsys usb");

        cmd += cmd_build("dmesg");

        cmd += cmd_build("logcat -d -v time");

        cmd += cmd_build("ls -lR /data/data/ca.yyx.hu/ /data/data/ca.yyx.hu/lib/ /init* /sbin/ /firmware/ /data/anr/ /data/tombstones/ /dev/ /system/ /sys/");

        cmd += cmd_build("cat " + hu_min_log);

        return (cmd);
    }

    Context m_context = this;

    private boolean file_email(String subject, String filename) {        // See http://stackoverflow.com/questions/2264622/android-multiple-email-attachment-using-intent-question
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");                                       // Doesn't work well: i.setType ("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"mikereidis@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, "Please write write problem, device/model and ROM/version. Please ensure " + filename + " file is actually attached or send manually. Thanks ! Mike.");
        i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + filename)); // File -> attachment
        try {
            startActivity(Intent.createChooser(i, "Send email..."));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(m_context, "No email. Manually send " + filename, Toast.LENGTH_LONG).show();
        }
        //dlg_dismiss (DLG_WAIT);
        return (true);
    }

    private String hu_min_log = "/data/data/ca.yyx.hu/files/hu.log";
    private String hu_sho_log = "hu.log"; // Short name

    private int long_logs_email() {
        String cmd = "bugreport > " + logfile;
        if (new_logs) {

            String str = "" + m_tv_log.getText();
            Utils.file_write(this, hu_sho_log, Utils.str_to_ba(str));

            logfile = "/sdcard/hulog.txt";//"/data/data/ca.yyx.hu/hu.txt";
            //Utils.daemon_set ("audio_alsa_log", "1");                       // Log ALSA controls
            cmd = new_logs_cmd_get();
        }

        int ret = Utils.sys_run(cmd, false);//true);                              // Run "bugreport" and output to file
        if (Utils.su_installed_get()) {
            Utils.sys_run("logcat -d -v time >> " + logfile, true);
        }


        String subject = "Headunit " + Utils.app_version_get(m_context);
        boolean bret = file_email(subject, logfile);                       // Email debug log file

        return (0);
    }

    private Timer logs_email_tmr = null;

    private class logs_email_tmr_hndlr extends java.util.TimerTask {
        public void run() {
            int ret = long_logs_email();
            click_ctr = 0;                                                    // Reset click_ctr to re-allow next 7 clicks
            Utils.logd("done ret: " + ret);
        }
    }

    private int logs_email() {
        int ret = 0;
        logs_email_tmr = new Timer("Logs Email", true);                    // One shot Poll timer for logs email
        if (logs_email_tmr != null) {
            logs_email_tmr.schedule(new logs_email_tmr_hndlr(), 10);        // Once after 0.01 seconds.
            Toast.makeText(m_context, "Please wait while debug log is collected. Will prompt when done...", Toast.LENGTH_LONG).show();
        }
        return (ret);
    }


    // Wifi:

/*
07-05 22:50:20.041 D/               wifi_init(32204): m_wifidir_mgr: android.net.wifi.p2p.WifiP2pManager@2b3c24b2  m_wifidir_chan: android.net.wifi.p2p.WifiP2pManager$Channel@f760c03  m_wifidir_bcr: ca.yyx.hu.HeadUnitActivity$WiFiDirectBroadcastReceiver@1a3e3780
07-05 22:50:20.044 D/               wifi_init(32204): at: ca.yyx.hu.HeadUnitActivity$aa_wifi_async_task@23c3d9fe

07-05 22:50:20.052 D/          doInBackground(32204): params: [Ljava.lang.Object;@3f0f4bac
07-05 22:50:20.054 D/          doInBackground(32204): new serverSocket: ServerSocket[addr=::/::,port=0,localport=30515]

07-05 22:50:20.085 E/               onFailure(32204): createGroup Failure reasonCode: 2

07-05 22:50:20.086 D/               onReceive(32204): action: android.net.wifi.p2p.STATE_CHANGED
07-05 22:50:20.086 D/               onReceive(32204): STATE_CHANGED Wifi P2P is enabled

07-05 22:50:20.086 D/               onReceive(32204): action: android.net.wifi.p2p.CONNECTION_STATE_CHANGE
07-05 22:50:20.086 D/               onReceive(32204): CONNECTION_CHANGED

07-05 22:50:20.087 D/               onReceive(32204): action: android.net.wifi.p2p.THIS_DEVICE_CHANGED
07-05 22:50:20.087 D/               onReceive(32204): THIS_DEVICE_CHANGED
07-05 22:50:20.087 D/               onSuccess(32204): discoverPeers Success

07-05 22:50:20.530 I/wpa_supplicant(  643): P2P-DEVICE-FOUND 52:2e:5c:e5:92:1f p2p_dev_addr=52:2e:5c:e5:12:1f pri_dev_type=10-0050F204-5 name='Android_381d' config_methods=0x188 dev_capab=0x25 group_capab=0xab vendor_elems=1

07-05 22:50:20.532 D/               onReceive(32204): action: android.net.wifi.p2p.PEERS_CHANGED
07-05 22:50:20.532 D/               onReceive(32204): PEERS_CHANGED

07-05 22:50:20.536 D/        onPeersAvailable(32204): myPeerListListener onPeersAvailable peers:
07-05 22:50:20.536 D/        onPeersAvailable(32204): Device: Android_381d
07-05 22:50:20.536 D/        onPeersAvailable(32204):  deviceAddress: 52:2e:5c:e5:12:1f
07-05 22:50:20.536 D/        onPeersAvailable(32204):  primary type: 10-0050F204-5
07-05 22:50:20.536 D/        onPeersAvailable(32204):  secondary type: null
07-05 22:50:20.536 D/        onPeersAvailable(32204):  wps: 392
07-05 22:50:20.536 D/        onPeersAvailable(32204):  grpcapab: 171
07-05 22:50:20.536 D/        onPeersAvailable(32204):  devcapab: 37
07-05 22:50:20.536 D/        onPeersAvailable(32204):  status: 0
07-05 22:50:20.536 D/        onPeersAvailable(32204):  wfdInfo: WFD enabled: falseWFD DeviceInfo: 0
07-05 22:50:20.536 D/        onPeersAvailable(32204):  WFD CtrlPort: 0
07-05 22:50:20.536 D/        onPeersAvailable(32204):  WFD MaxThroughput: 0

*/

    private WifiP2pManager m_wifidir_mgr;
    private Channel m_wifidir_chan;
    private BroadcastReceiver m_wifidir_bcr = null;

    private IntentFilter mIntentFilter = null;

    private boolean wifi_deinit_bypass = false;//true;

    private int wifi_deinit() {
        Utils.logd("m_wifidir_mgr: " + m_wifidir_mgr + "  m_wifidir_chan: " + m_wifidir_chan + "  m_wifidir_bcr: " + m_wifidir_bcr);

        if (wifi_deinit_bypass)
            return (-1);

        if (m_wifidir_chan == null || m_wifidir_mgr == null)
            return (-1);
//*
        m_wifidir_mgr.stopPeerDiscovery(m_wifidir_chan, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Utils.logd("stopPeerDiscovery Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                Utils.loge("stopPeerDiscovery Failure reasonCode: " + reasonCode);
            }
        });
//*/
        WifiP2pManager.ActionListener wal = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Utils.logd("stopPeerDiscovery/cancelConnect/removeGroup Success");
            }

            @Override
            public void onFailure(int reasonCode) {
                Utils.loge("stopPeerDiscovery/cancelConnect/removeGroup Failure reasonCode: " + reasonCode);
            }
        };

        m_wifidir_mgr.stopPeerDiscovery(m_wifidir_chan, wal);

        m_wifidir_mgr.cancelConnect(m_wifidir_chan, wal);

        m_wifidir_mgr.removeGroup(m_wifidir_chan, wal);

        m_wifidir_chan = null;
        m_wifidir_mgr = null;
        m_wifidir_bcr = null;

        return (0);
    }

    private int wifi_init() {  // call from onCreate()
        m_wifidir_mgr = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        m_wifidir_chan = m_wifidir_mgr.initialize(this, getMainLooper(), null);
        m_wifidir_bcr = new WiFiDirectBroadcastReceiver(m_wifidir_mgr, m_wifidir_chan, this);

        Utils.logd("m_wifidir_mgr: " + m_wifidir_mgr + "  m_wifidir_chan: " + m_wifidir_chan + "  m_wifidir_bcr: " + m_wifidir_bcr);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager.ActionListener wal = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Utils.logd("createGroup Success");                            // This is only triggered after we have a connection
            }

            @Override
            public void onFailure(int reasonCode) {
                Utils.loge("createGroup Failure reasonCode: " + reasonCode);  // Already exists ?: reateGroup Failure reasonCode: 2
            }
        };
        m_wifidir_mgr.createGroup(m_wifidir_chan, wal);


        return (0);
    }


/*
  private int wifi_connect (WifiP2pDevice device) {
    //obtain a peer from the WifiP2pDeviceList
    WifiP2pConfig config = new WifiP2pConfig ();
    config.deviceAddress = device.deviceAddress;
    m_wifidir_mgr.connect (m_wifidir_chan, config, new ActionListener() {

      @Override
      public void onSuccess () {
        Utils.logd ("connect Success");
      }
      @Override
      public void onFailure (int reasonCode) {
        Utils.loge ("connect Failure reasonCode: " + reasonCode);
      }
    });
    return (0);
  }
*/


    private void wifi_resume() {
        Utils.logd("m_wifidir_bcr: " + m_wifidir_bcr);
/*
    if (m_wifidir_bcr == null)  // If was registered earlier...
    if (m_wifidir_bcr != null)  !!!!! ????

      return;
    try {
      registerReceiver (m_wifidir_bcr, mIntentFilter);    // re-register
    }
    catch (Throwable e) {
      e.printStackTrace ();
    };

    m_wifidir_mgr.discoverPeers (m_wifidir_chan, new WifiP2pManager.ActionListener () {
      @Override
      public void onSuccess () {
        Utils.logd ("discoverPeers Success");
      }
      @Override
      public void onFailure (int reasonCode) {
        Utils.loge ("discoverPeers Failure reasonCode: " + reasonCode);
      }
    });
*/
    }

    private void wifi_pause() {
        Utils.logd("m_wifidir_bcr: " + m_wifidir_bcr);
/*
    if (m_wifidir_bcr == null)  // If was registered...
      return;
    try {
      unregisterReceiver (m_wifidir_bcr);
    }
    catch (Throwable e) {
      e.printStackTrace ();
    };
*/
    }

    PeerListListener myPeerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            Utils.logd("myPeerListListener onPeersAvailable peers: " + peers);
        }
    };

    // A BroadcastReceiver that notifies of important Wi-Fi p2p events.
    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager m_wifidir_mgr;
        private Channel m_wifidir_chan;
        private HeadUnitActivity m_wifidir_act;

        public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, HeadUnitActivity activity) {
            super();
            this.m_wifidir_mgr = manager;
            this.m_wifidir_chan = channel;
            this.m_wifidir_act = activity;
        }


        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Utils.logd("action: " + action);

            if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {          // Check to see if Wi-Fi is enabled and notify appropriate activity
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Utils.logd("STATE_CHANGED Wifi P2P is enabled");
                } else {
                    Utils.logd("STATE_CHANGED Wi-Fi P2P is not enabled");
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                Utils.logd("PEERS_CHANGED");        // Request currently available peers from the wifi p2p manager. This is an asynchronous call and the calling activity is notified with a callback on PeerListListener.onPeersAvailable()
                if (m_wifidir_mgr != null) {
                    m_wifidir_mgr.requestPeers(m_wifidir_chan, myPeerListListener);
                }
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                Utils.logd("CONNECTION_CHANGED");          // Respond to new connection or disconnections
            } else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
                Utils.logd("THIS_DEVICE_CHANGED");          // Respond to this device's wifi state changing
            } else if (action.equals(UiModeManager.ACTION_ENTER_CAR_MODE)) {
                Utils.logd("ACTION_ENTER_CAR_MODE");
            } else if (action.equals(UiModeManager.ACTION_EXIT_CAR_MODE)) {
                Utils.logd("ACTION_EXIT_CAR_MODE");
            } else if (action.equals(UiModeManager.ACTION_ENTER_DESK_MODE)) {
                Utils.logd("ACTION_ENTER_DESK_MODE");
            } else if (action.equals(UiModeManager.ACTION_EXIT_DESK_MODE)) {
                Utils.logd("ACTION_EXIT_DESK_MODE");
            } else {
                Utils.loge("OTHER !! ??");
            }
        }

    }


/*
  public class aa_wifi_async_task extends AsyncTask { //<Void, Void, Void> {

    private Context context;
    //private TextView statusText;

    public aa_wifi_async_task (Context context) {//, View statusText) {
      this.context = context;
      //this.statusText = (TextView) statusText;
      Utils.logd ("context: " + context);
    }

    @Override
    protected Object doInBackground (Object... params) {//(Void... params) {// (Params... p) {//Void... v) {//Void... params) {
    //protected String doInBackground (Object... obj) {//String... str) {// params) {

      try {
        Utils.logd ("params: " + params);

        ServerSocket serverSocket = new ServerSocket (30515);//8888);   // Create a server socket
        Utils.logd ("new serverSocket: " + serverSocket);

        Socket client = serverSocket.accept ();                         // Block waiting for client connections
        Utils.logd ("serverSocket.accept client: " + client);

        // If this code is reached, a client has connected

        InputStream  inp_str = client.getInputStream ();
        OutputStream out_str = client.getOutputStream ();

        byte vr_buf [] = {0, 3, 0, 6, 0, 1, 0, 1, 0, 1};                    // Version Request
        int ret = hu_aap_usb_set (0, 3, 1, vr_buf, vr_buf.length);
        //ret = hu_aap_usb_send (vr_buf, sizeof (vr_buf), 1000);              // Send Version Request

        out_str.write (vr_buf, 0, vr_buf.length);
        out_str.flush ();
        Utils.logd ("version request sent to Wifi output");

        Utils.ms_sleep (200);
        byte in_buf [] = new byte [65536];
        int bytes_read = inp_str.read (in_buf, 0, 65536);
        Utils.logd ("version response received from Wifi input bytes_read: " + bytes_read);
        if (bytes_read > 0) {
          Utils.hex_dump ("AA WiFi: ", in_buf, bytes_read);
        }

        out_str.close ();
        inp_str.close ();

        serverSocket.close();
        return (null);// f.getAbsolutePath();
      }
      catch (IOException e) {
        Log.e ("AAWiFi", e.getMessage ());
        return null;
      }
    }

    // Start activity that can handle the JPEG image
    @Override
    protected void onPostExecute (Object result) {//String result) {
      Utils.logd ("result: " + result);
//      if (result != null) {
//        //statusText.setText("File copied - " + result);
//        Intent intent = new Intent();
//        intent.setAction(android.content.Intent.ACTION_VIEW);
//        intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//        context.startActivity(intent);
//      }
    }
  }

  private int hu_aap_usb_set (int chan, int flags, int type, byte [] buf, int len) {  // Convenience function sets up 6 byte USB header: chan, flags, len, type

    buf [0] = (byte) chan;                                              // Encode channel and flags
    buf [1] = (byte) flags;
    buf [2] = (byte) ((len -4) / 256);                                            // Encode length of following data:
    buf [3] = (byte) ((len -4) % 256);
    if (type >= 0) {                                                    // If type not negative, which indicates encrypted type should not be touched...
      buf [4] = (byte) (type / 256);
      buf [5] = (byte) (type % 256);                                             // Encode msg_type
    }

    return (len);
  }

*/

}