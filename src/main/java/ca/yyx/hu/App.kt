package ca.yyx.hu

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

class App : Application(), AapTransport.Listener {

    private lateinit var component: AppComponent

    private val locationUpdatesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location = extractLocation(intent)
            if (component.settings.useGpsForNavigation) {
                App.provide(context).transport.send(LocationUpdateEvent(location))
            }

            if (location.latitude != 0.0 && location.longitude != 0.0) {
                component.settings.lastKnownLocation = location
            }
        }
    }

    private val mediaKeyCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.getParcelableExtra<KeyEvent>(LocalIntent.EXTRA_EVENT)
            App.provide(context).transport.sendButton(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        }
    }

    private val deviceListener = DeviceListener()

    override fun onCreate() {
        super.onCreate()

        component = AppComponent(this)
        registerReceiver(deviceListener, DeviceListener.createIntentFilter())
        LocalBroadcastManager.getInstance(this).registerReceiver(mediaKeyCodeReceiver, LocalIntent.FILTER_MEDIA_KEY_EVENT)
        LocalBroadcastManager.getInstance(this).registerReceiver(locationUpdatesReceiver, LocalIntent.FILTER_LOCATION_UPDATE)
    }

    override fun gainVideoFocus() {
        startActivity(AapProjectionActivity.intent(this))
    }

    companion object {
        fun get(context: Context): App {
            return context.applicationContext as App
        }
        fun provide(context: Context): AppComponent {
            return get(context).component
        }
    }
}
