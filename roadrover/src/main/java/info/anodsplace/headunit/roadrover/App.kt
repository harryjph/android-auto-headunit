package info.anodsplace.headunit.roadrover

import android.app.Application
import android.os.IBinder

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
