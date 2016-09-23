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
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.Calendar;

import ca.yyx.hu.App;
import ca.yyx.hu.R;
import ca.yyx.hu.RemoteControlReceiver;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.Utils;

import static android.R.attr.enabled;

/**
 * @author algavris
 * @date 03/06/2016.
 */

public class AapService extends Service implements UsbReceiver.Listener {
    private MediaSessionCompat mMediaSession;
    private AapTransport mTransport;        // Transport API
    private AudioDecoder mAudioDecoder;
    private UiModeManager mUiModeManager = null;
    private UsbAccessoryConnection mUsbAccessoryConnection;
    private UsbReceiver mUsbReceiver;
    private BroadcastReceiver mTimeTickReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent createIntent(UsbDevice device, Context context) {
        Intent intent = new Intent(context, AapService.class);
        intent.putExtra(UsbManager.EXTRA_DEVICE, device);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbAccessoryConnection = new UsbAccessoryConnection(usbManager);

        mUiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);

        mAudioDecoder = App.get(this).audioDecoder();
        mTransport = App.get(this).transport();

        mMediaSession = new MediaSessionCompat(this, "MediaSession", new ComponentName(this, RemoteControlReceiver.class), null);
        mMediaSession.setCallback(new MediaSessionCallback(mTransport));
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mUsbReceiver = new UsbReceiver(this);
        mTimeTickReceiver = new TimeTickReceiver(mTransport, mUiModeManager);

        registerReceiver(mTimeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onDisconnect();
        unregisterReceiver(mTimeTickReceiver);
        unregisterReceiver(mUsbReceiver);
        mUiModeManager.disableCarMode(0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UsbDevice device = IntentUtils.getDevice(intent);
        if (device == null) {
            Utils.loge("No device in "+intent);
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


        mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build());

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                // Ignore
            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mMediaSession.setActive(true);

        startForeground(1, noty);

        try {
            if (mUsbAccessoryConnection.isDeviceRunning(device))
            {
                if (!mTransport.isAlive())
                {
                    mTransport.connectAndStart(mUsbAccessoryConnection);
                }
                startActivity();
            } else if (connect(device)) {
                if (mTransport.isAlive())
                {
                    reset();
                    mTransport = App.get(this).transport();
                }
                mTransport.connectAndStart(mUsbAccessoryConnection);
                startActivity();
            }
            else
            {
                Utils.loge("Cannot connect to device " + UsbDeviceCompat.getUniqueName(device));
            }
        } catch (UsbAccessoryConnection.UsbOpenException e) {
            Utils.loge(e);
        }

        return START_STICKY;
    }

    private void startActivity()
    {
        Intent aapIntent = new Intent(this, AapProjectionActivity.class);
        aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(aapIntent);
    }

    private void onDisconnect() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(IntentUtils.ACTION_DISCONNECT);
        mUsbAccessoryConnection.disconnect();
        reset();
    }

    private void reset()
    {
        mTransport.quit();
        mAudioDecoder.stop();
        App.get(this).videoDecoder().stop();
        App.get(this).reset();
    }

    private static class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        private AapTransport mTransport;

        MediaSessionCallback(AapTransport transport) {
            mTransport = transport;
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            Utils.logd(command);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Utils.logd(action);
        }

        @Override
        public void onSkipToNext() {
            Utils.logd("onSkipToNext");

            mTransport.sendButton(Protocol.BTN_NEXT, true);
            Utils.ms_sleep(10);
            mTransport.sendButton(Protocol.BTN_NEXT, false);
        }

        @Override
        public void onSkipToPrevious() {
            Utils.logd("onSkipToPrevious");

            mTransport.sendButton(Protocol.BTN_PREV, true);
            Utils.ms_sleep(10);
            mTransport.sendButton(Protocol.BTN_PREV, false);
        }

        @Override
        public void onPlay() {
            Utils.logd("PLAY");

            mTransport.sendButton(Protocol.BTN_PLAYPAUSE, true);
            Utils.ms_sleep(10);
            mTransport.sendButton(Protocol.BTN_PLAYPAUSE, false);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Utils.logd(mediaButtonEvent.toString());
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    }


    public boolean connect(UsbDevice device) throws UsbAccessoryConnection.UsbOpenException {
        if (mUsbAccessoryConnection.isConnected())
        {
            if (mUsbAccessoryConnection.isDeviceRunning(device)) {
                Utils.logd("Device already connected");
                return true;
            }
        }
        return mUsbAccessoryConnection.connect(device);
    }

    @Override
    public void onUsbDetach(UsbDevice device) {
        if (mUsbAccessoryConnection.isDeviceRunning(device)) {
            stopSelf();
        }
    }

    @Override
    public void onUsbAttach(UsbDevice device) {

    }

    @Override
    public void onUsbPermission(boolean granted, boolean connect, UsbDevice device) {

    }

    private static class TimeTickReceiver extends BroadcastReceiver {
        private final AapTransport mTransport;
        private final UiModeManager mUiModeManager;
        private int mNightMode = -1;

        public TimeTickReceiver(AapTransport transport, UiModeManager uiModeManager) {
            mTransport = transport;
            mUiModeManager = uiModeManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            int nightmodenow = 1;
            if (hour >= 6 && hour <= 18)
            {
                nightmodenow = 0;
            }
            Utils.logd("NightMode: %d != %d", mNightMode, nightmodenow);
            if (mNightMode != nightmodenow) {
                mNightMode = nightmodenow;

                boolean enabled = nightmodenow == 1;
                mUiModeManager.setNightMode(enabled ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
                mTransport.sendNightMode(enabled);
            }
        }
    }
}
