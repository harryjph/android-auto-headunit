package ca.yyx.hu.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * @author algavris
 * @date 18/12/2016.
 */
class GpsLocationService : Service() {
    private var mGpsLocation: GpsLocation? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (mGpsLocation == null) {
            mGpsLocation = GpsLocation(this)
        }

        mGpsLocation?.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        mGpsLocation?.stop()
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
