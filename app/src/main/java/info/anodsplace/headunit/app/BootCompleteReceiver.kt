package info.anodsplace.headunit.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import info.anodsplace.headunit.App

import info.anodsplace.headunit.location.GpsLocationService
class BootCompleteReceiver : BroadcastReceiver() { // TODO don't always be using gps...
    override fun onReceive(context: Context, intent: Intent) {
        val h = Handler()
        h.postDelayed({
            App.get(context).startService(GpsLocationService.intent(context))
        }, 10000)
    }
}
