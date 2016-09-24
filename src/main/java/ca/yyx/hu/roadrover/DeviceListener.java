package ca.yyx.hu.roadrover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;

import ca.yyx.hu.App;
import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.aap.Protocol;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 24/09/2016.
 */

public class DeviceListener extends BroadcastReceiver {
    private static final String ACTION_AUDIO = "com.roadrover.frontpane.audio";
    private static final String ACTION_KEYEVENT = "com.roadrover.frontpane.keyevent";
    private static final String ACTION_STARTMUSIC = "com.roadrover.startmusic";
    private final AapTransport mTransport;

    public static IntentFilter createIntentFilter()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_AUDIO);
        filter.addAction(ACTION_KEYEVENT);
        filter.addAction(ACTION_STARTMUSIC);
        return filter;
    }

    public DeviceListener(Context context)
    {
        mTransport = App.get(context).transport();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.logd(intent.toString());

        if (ACTION_KEYEVENT.equals(intent.getAction())) {
            int keyCode = intent.getIntExtra("keyvalue", 0);
            handleKeyEvent(keyCode);
        } else if (ACTION_STARTMUSIC.equals(intent.getAction()))
        {
            mTransport.sendButton(Protocol.BTN_PLAYPAUSE, true);
        }
    }

    private void handleKeyEvent(int keyCode) {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                mTransport.sendButton(Protocol.BTN_PLAYPAUSE, true);
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                mTransport.sendButton(Protocol.BTN_STOP, true);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                mTransport.sendButton(Protocol.BTN_NEXT, true);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                mTransport.sendButton(Protocol.BTN_PREV, true);
                break;
            default:
                Utils.logd("Unknown keyCode: "+keyCode);
        }
    }
}
