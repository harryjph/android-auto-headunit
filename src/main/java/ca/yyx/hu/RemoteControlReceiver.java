package ca.yyx.hu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.aap.Messages;
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
            AppLog.i("ACTION_MEDIA_BUTTON: "+event.getKeyCode());
            App.get(context).transport().sendButton(event.getKeyCode(), event.getAction() == KeyEvent.ACTION_DOWN);
        }
    }
}
