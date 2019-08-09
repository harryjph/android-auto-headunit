package info.anodsplace.headunit

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import info.anodsplace.headunit.aap.AapProjectionActivity
import info.anodsplace.headunit.aap.AapTransport
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.IntentFilters
import android.R.attr.path
import java.io.File

/**
 * @author algavris
 * *
 * @date 30/05/2016.
 */

class App : Application() {

    private val component: AppComponent by lazy {
        AppComponent(this)
    }

    override fun onCreate() {
        super.onCreate()

        AppLog.d( "native library dir ${applicationInfo.nativeLibraryDir}")

        File(applicationInfo.nativeLibraryDir).listFiles().forEach { file ->
            AppLog.d( "   ${file.name}")
        }

        registerReceiver(AapBroadcastReceiver(), AapBroadcastReceiver.filter)
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
