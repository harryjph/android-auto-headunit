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

/**
 * @author algavris
 * @date 30/05/2016.
 */

public class App extends Application implements AapTransport.Listener {
    public static final boolean IS_LOLLIPOP = Build.VERSION.SDK_INT >= 21;

    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;
    private AapTransport mTransport;

    public static App get(Context context)
    {
        return (App)context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioDecoder = new AudioDecoder();
        mVideoDecoder = new VideoDecoder();

    }

    public AapTransport transport()
    {
        if (mTransport == null) {
            String btMacAddress =  BluetoothAdapter.getDefaultAdapter().getAddress();
            if ("02:00:00:00:00:00".equals(btMacAddress)) {
                btMacAddress = android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address");
            }
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
