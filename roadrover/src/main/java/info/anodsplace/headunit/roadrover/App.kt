package info.anodsplace.headunit.roadrover

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import ca.yyx.hu.aap.AapProjectionActivity
import ca.yyx.hu.aap.AapTransport
import ca.yyx.hu.aap.protocol.messages.LocationUpdateEvent
import ca.yyx.hu.roadrover.DeviceListener
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.LocalIntent.extractLocation

/**
 * @author algavris
 * *
 * @date 30/05/2016.
 */

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        registerReceiver(DeviceListener(), DeviceListener.createIntentFilter())
    }

}
