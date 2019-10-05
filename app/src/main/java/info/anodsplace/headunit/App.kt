package info.anodsplace.headunit

import android.app.Application
import android.content.Context
import info.anodsplace.headunit.utils.AppLog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import java.io.File

class App : Application() {

    private val component: AppComponent by lazy {
        AppComponent(this)
    }

    override fun onCreate() {
        super.onCreate()

        AppLog.d {  "native library dir ${applicationInfo.nativeLibraryDir}" }

        File(applicationInfo.nativeLibraryDir).listFiles().forEach { file ->
            AppLog.d {  "   ${file.name}" }
        }

        registerReceiver(AapBroadcastReceiver(), AapBroadcastReceiver.filter)
    }

    companion object {
        const val defaultChannel = "default"

        fun get(context: Context): App {
            return context.applicationContext as App
        }
        fun provide(context: Context): AppComponent {
            return get(context).component
        }
    }
}
