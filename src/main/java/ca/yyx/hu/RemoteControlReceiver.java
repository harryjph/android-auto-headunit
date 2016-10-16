package ca.yyx.hu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.aap.Protocol;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 03/06/2016.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            AppLog.logd("ACTION_MEDIA_BUTTON: "+event.getKeyCode());

            AapTransport transport = App.get(context).transport();

            switch (event.getKeyCode())
            {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    transport.sendButton(Protocol.BTN_PLAYPAUSE, event.getAction() == KeyEvent.ACTION_DOWN);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    transport.sendButton(Protocol.BTN_NEXT, event.getAction() == KeyEvent.ACTION_DOWN);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    transport.sendButton(Protocol.BTN_PREV, event.getAction() == KeyEvent.ACTION_DOWN);
                    break;
            }
        }
    }
}
