package ca.yyx.hu.app

import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */

open class BaseFragment : Fragment() {

    fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter): Intent? {
        return activity.registerReceiver(receiver, filter)
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        activity.unregisterReceiver(receiver)
    }
}
