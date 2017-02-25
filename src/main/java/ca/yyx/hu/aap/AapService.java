package ca.yyx.hu.aap;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import ca.yyx.hu.App;
import ca.yyx.hu.R;
import ca.yyx.hu.RemoteControlReceiver;
import ca.yyx.hu.aap.protocol.messages.NightModeEvent;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.connection.UsbAccessoryConnection;
import ca.yyx.hu.connection.SocketAccessoryConnection;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.location.GpsLocationService;
import ca.yyx.hu.roadrover.DeviceListener;
import ca.yyx.hu.connection.UsbReceiver;
import ca.yyx.hu.utils.LocalIntent;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.NightMode;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 03/06/2016.
 */

public class AapService extends Service implements UsbReceiver.Listener, AccessoryConnection.Listener {
    private static final int TYPE_USB = 1;
    private static final int TYPE_WIFI = 2;
    public static final String EXTRA_CONNECTION_TYPE = "extra_connection_type";
    public static final String EXTRA_IP = "extra_ip";

    private MediaSessionCompat mMediaSession;
    private AudioDecoder mAudioDecoder;
    private UiModeManager mUiModeManager = null;
    private AccessoryConnection mAccessoryConnection;
    private UsbReceiver mUsbReceiver;
    private BroadcastReceiver mTimeTickReceiver;
    private DeviceListener mDeviceListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent createIntent(UsbDevice device, Context context) {
        Intent intent = new Intent(context, AapService.class);
        intent.putExtra(UsbManager.EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_CONNECTION_TYPE, TYPE_USB);
        return intent;
    }

    public static Intent createIntent(String ip, Context context) {
        Intent intent = new Intent(context, AapService.class);
        intent.putExtra(EXTRA_IP, ip);
        intent.putExtra(EXTRA_CONNECTION_TYPE, TYPE_WIFI);
        return intent;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);

        mAudioDecoder = App.get(this).audioDecoder();

        mMediaSession = new MediaSessionCompat(this, "MediaSession", new ComponentName(this, RemoteControlReceiver.class), null);
        mMediaSession.setCallback(new MediaSessionCallback(this));
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mUsbReceiver = new UsbReceiver(this);
        mTimeTickReceiver = new TimeTickReceiver(new Settings(this), mUiModeManager);

        mDeviceListener = new DeviceListener();
        registerReceiver(mDeviceListener, DeviceListener.createIntentFilter());
        registerReceiver(mTimeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onDisconnect();
        unregisterReceiver(mDeviceListener);
        unregisterReceiver(mTimeTickReceiver);
        unregisterReceiver(mUsbReceiver);
        mUiModeManager.disableCarMode(0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mAccessoryConnection = createConnection(intent, this);
        if (mAccessoryConnection == null) {
            AppLog.e("Cannot create connection "+intent);
            stopSelf();
            return START_NOT_STICKY;
        }

        mUiModeManager.enableCarMode(0);

        Intent aapIntent = new Intent(this, AapProjectionActivity.class);
        aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification noty = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_aa)
                .setTicker("Headunit is running")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Headunit is running")
                .setContentText("...")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, aapIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        startService(GpsLocationService.intent(this));

        mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build());

        mMediaSession.setActive(true);

        startForeground(1, noty);

        mAccessoryConnection.connect(this);

        return START_STICKY;
    }

    @Override
    public void onConnectionResult(boolean success) {
        if (success) {
            reset();
            App.get(this).transport().connectAndStart(mAccessoryConnection);
        }
        else
        {
            AppLog.e("Cannot connect to device");
            Toast.makeText(this, "Cannot connect to the device", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    private static AccessoryConnection createConnection(Intent intent, Context context) {

        int connectionType = intent.getIntExtra(EXTRA_CONNECTION_TYPE, 0);

        if (connectionType == TYPE_USB) {
            UsbDevice device = LocalIntent.extractDevice(intent);
            if (device == null) {
                AppLog.e("No device in " + intent);
                return null;
            }
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            return new UsbAccessoryConnection(usbManager, device);
        } else if (connectionType == TYPE_WIFI) {
            String ip = intent.getStringExtra(EXTRA_IP);
            return new SocketAccessoryConnection(ip);
        }

        return null;
    }

    private void onDisconnect() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(LocalIntent.ACTION_DISCONNECT);
        reset();
        mAccessoryConnection.disconnect();
        mAccessoryConnection = null;
    }

    private void reset()
    {
        App.get(this).transport().quit();
        mAudioDecoder.stop();
        App.get(this).videoDecoder().stop("AapService::reset");
        App.get(this).reset();
    }



    private static class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        private Context mContext;

        MediaSessionCallback(Context context) {
            mContext = context;
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            AppLog.i(command);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            AppLog.i(action);
        }

        @Override
        public void onSkipToNext() {
            AppLog.i("onSkipToNext");

            App.get(mContext).transport().sendButton(KeyEvent.KEYCODE_MEDIA_NEXT, true);
            Utils.ms_sleep(10);
            App.get(mContext).transport().sendButton(KeyEvent.KEYCODE_MEDIA_NEXT, false);
        }

        @Override
        public void onSkipToPrevious() {
            AppLog.i("onSkipToPrevious");

            App.get(mContext).transport().sendButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS, true);
            Utils.ms_sleep(10);
            App.get(mContext).transport().sendButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS, false);
        }

        @Override
        public void onPlay() {
            AppLog.i("PLAY");

            App.get(mContext).transport().sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true);
            Utils.ms_sleep(10);
            App.get(mContext).transport().sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            AppLog.i(mediaButtonEvent.toString());
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    }


     @Override
    public void onUsbDetach(UsbDevice device) {
         if (mAccessoryConnection instanceof UsbAccessoryConnection) {
             if (((UsbAccessoryConnection)mAccessoryConnection).isDeviceRunning(device)) {
                 stopSelf();
             }
         }
    }

    @Override
    public void onUsbAttach(UsbDevice device) {

    }

    @Override
    public void onUsbPermission(boolean granted, boolean connect, UsbDevice device) {

    }

    private static class TimeTickReceiver extends BroadcastReceiver {
        private final UiModeManager mUiModeManager;
        private final NightMode mNightMode;
        private boolean mLastNightMode = false;

        public TimeTickReceiver(Settings settings, UiModeManager uiModeManager) {
            mUiModeManager = uiModeManager;
            mNightMode = new NightMode(settings);
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean isCurrent = mNightMode.current();
            if (mLastNightMode != isCurrent) {
                mLastNightMode = isCurrent;

                mUiModeManager.setNightMode(isCurrent ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
                AppLog.i(mNightMode.toString());
                App.get(context).transport().send(new NightModeEvent(isCurrent));
            }
        }
    }
}
