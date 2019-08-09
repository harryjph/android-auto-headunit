package info.anodsplace.headunit

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import info.anodsplace.headunit.aap.AapProjectionActivity
import info.anodsplace.headunit.aap.AapTransport
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.IntentFilters
import android.R.attr.path
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import info.anodsplace.headunit.main.BackgroundNotification
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            component.notificationManager.createNotificationChannel(NotificationChannel(defaultChannel, "Default", NotificationManager.IMPORTANCE_DEFAULT))
            component.notificationManager.createNotificationChannel(NotificationChannel(BackgroundNotification.mediaChannel, "Media channel", NotificationManager.IMPORTANCE_DEFAULT))
        }

        registerReceiver(AapBroadcastReceiver(), AapBroadcastReceiver.filter)
    }

    companion object {
        const val defaultChannel = " default"

        fun get(context: Context): App {
            return context.applicationContext as App
        }
        fun provide(context: Context): AppComponent {
            return get(context).component
        }
    }
}
