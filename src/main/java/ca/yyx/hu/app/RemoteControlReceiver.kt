package ca.yyx.hu.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import ca.yyx.hu.App

import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 03/06/2016.
 */
class RemoteControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
            val event: KeyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            AppLog.i("ACTION_MEDIA_BUTTON: " + event.keyCode)
            App.get(context).transport().sendButton(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        }
    }
}
