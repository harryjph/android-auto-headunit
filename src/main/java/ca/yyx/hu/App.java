package ca.yyx.hu;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import ca.yyx.hu.aap.AapProjectionActivity;
import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.location.GpsLocation;
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
    private GpsLocation mGpsLocation;

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
        mGpsLocation = new GpsLocation(this);
        mGpsLocation.start();
    }

    public AapTransport transport()
    {
        if (mTransport == null) {
            String btMacAddress = mSettings.getBluetoothAddress();
            mTransport = new AapTransport(mAudioDecoder, mVideoDecoder, (AudioManager) getSystemService(AUDIO_SERVICE), btMacAddress, this);
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
