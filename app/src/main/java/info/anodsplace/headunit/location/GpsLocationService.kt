package info.anodsplace.headunit.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class GpsLocationService : Service() {
    private var gpsLocationListener: GpsLocationListener? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (gpsLocationListener == null) {
            gpsLocationListener = GpsLocationListener(this)
        }

        gpsLocationListener?.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        gpsLocationListener?.stop()
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
