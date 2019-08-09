package info.anodsplace.headunit.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * @author algavris
 * @date 18/12/2016.
 */
class GpsLocationService : Service() {
    private var gpsLocation: GpsLocation? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (gpsLocation == null) {
            gpsLocation = GpsLocation(this)
        }

        gpsLocation?.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        gpsLocation?.stop()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, GpsLocationService::class.java)
        }
    }

}
