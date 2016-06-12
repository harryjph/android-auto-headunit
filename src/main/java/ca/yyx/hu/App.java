package ca.yyx.hu;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;

import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.usb.UsbDeviceCompat;
import ca.yyx.hu.usb.UsbReceiver;
import ca.yyx.hu.utils.IntentUtils;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 30/05/2016.
 */

public class App extends Application {

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

        mAudioDecoder = new AudioDecoder(this);
        mVideoDecoder = new VideoDecoder(this);

    }

    public AapTransport transport()
    {
        if (mTransport == null || mTransport.isStopped()) {
            mTransport = new AapTransport(mAudioDecoder, mVideoDecoder);
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
}
