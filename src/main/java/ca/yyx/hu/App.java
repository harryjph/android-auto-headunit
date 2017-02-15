package ca.yyx.hu;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import ca.yyx.hu.aap.AapProjectionActivity;
import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.aap.protocol.messages.LocationUpdateEvent;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.location.GpsLocation;
import ca.yyx.hu.utils.LocalIntent;
import ca.yyx.hu.utils.Settings;

/**
 * @author algavris
 * @date 30/05/2016.
 */

public class App extends Application implements AapTransport.Listener {
    public static final boolean IS_LOLLIPOP = Build.VERSION.SDK_INT >= 21;

    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;
    private AapTransport mTransport;
    private Settings mSettings;
    private BroadcastReceiver mLocationUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = LocalIntent.extractLocation(intent);
            App.get(context).transport().send(new LocationUpdateEvent(location));

            if (location.getLongitude() != 0 && location.getLatitude() != 0)
            {
                mSettings.setLastKnownLocation(location);
            }
        }
    };

    public static App get(Context context)
    {
        return (App)context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioDecoder = new AudioDecoder();
        mVideoDecoder = new VideoDecoder();
        mSettings = new Settings(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationUpdatesReceiver, LocalIntent.FILTER_LOCATION_UPDATE);

    }

    public AapTransport transport()
    {
        if (mTransport == null) {
            mTransport = new AapTransport(mAudioDecoder, mVideoDecoder, (AudioManager) getSystemService(AUDIO_SERVICE), mSettings, this);
        }
        return mTransport;
    }

    public AudioDecoder audioDecoder() {
        return mAudioDecoder;
    }

    public VideoDecoder videoDecoder() {
        return mVideoDecoder;
    }

    public void reset() {
        mTransport = null;
    }

    @Override
    public void gainVideoFocus() {
        AapProjectionActivity.start(this);
    }
}
