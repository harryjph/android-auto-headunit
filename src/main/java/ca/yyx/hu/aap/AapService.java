package ca.yyx.hu.aap;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;

import ca.yyx.hu.App;
import ca.yyx.hu.R;
import ca.yyx.hu.RemoteControlReceiver;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.Utils;

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
        mAudioDecoder = App.get(this).audioDecoder();
        mTransport = App.get(this).transport();

        mMediaSession = new MediaSessionCompat(this, "MediaSession", new ComponentName(this, RemoteControlReceiver.class), null);
        mMediaSession.setCallback(new MediaSessionCallback(mTransport));

        mUsbReceiver = new UsbReceiver(this);
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onDisconnect();
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
        mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);

        Intent aapIntent = new Intent(this, AapActivity.class);
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

        startForeground(1, noty);

        try {
            if (connect(device))
            {
                onConnect();
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

    private void onConnect() {
        mTransport.connectAndStart(mUsbAccessoryConnection);
        Intent aapIntent = new Intent(this, AapActivity.class);
        aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(aapIntent);
    }

    private void onDisconnect() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(IntentUtils.ACTION_DISCONNECT);
        mUsbAccessoryConnection.disconnect();
        mTransport.quit();
        mAudioDecoder.stop();
    }

    private static class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        private AapTransport mTransport;

        public MediaSessionCallback(AapTransport transport) {
            mTransport = transport;
        }

        @Override
        public void onSkipToNext() {
        }

        @Override
        public void onSkipToPrevious() {
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Utils.logd(""+mediaButtonEvent);
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
            onDisconnect();
        }
    }

    @Override
    public void onUsbAttach(UsbDevice device) {

    }

    @Override
    public void onUsbPermission(boolean granted, boolean connect, UsbDevice device) {

    }

}
