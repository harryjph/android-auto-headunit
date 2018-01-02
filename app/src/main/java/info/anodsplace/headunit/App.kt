package info.anodsplace.headunit

import android.app.Application
import android.content.Context
import info.anodsplace.headunit.aap.AapProjectionActivity
import info.anodsplace.headunit.aap.AapTransport
import info.anodsplace.headunit.utils.IntentFilters

/**
 * @author algavris
 * *
 * @date 30/05/2016.
 */

class App : Application(), AapTransport.Listener {

    private val component: AppComponent by lazy {
        AppComponent(this)
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(AapBroadcastReceiver(), IntentFilters.mediaKeyEvent)
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
