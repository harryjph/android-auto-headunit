package ca.yyx.hu.location

import android.content.Context
import android.location.Criteria
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager

import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.Utils

/**
 * @author algavris
 * *
 * @date 06/12/2016.
 */

class GpsLocation internal constructor(context: Context) : GpsStatus.Listener, LocationListener {
    private val mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mBroadcastManager = LocalBroadcastManager.getInstance(context)
    private var mStatus: GpsStatus? = null
    private var mRequested: Boolean = false

    init {
        // Acquire a reference to the system Location Manager
        mLocationManager.addGpsStatusListener(this)
    }

    fun start() {
        if (mRequested) {
            return
        }
        AppLog.i("Request location updates")
        val criteria = Criteria()
        criteria.setPowerRequirement(Criteria.POWER_HIGH)
        mLocationManager.requestLocationUpdates(500, 0.0f, criteria, this, null)
        mRequested = true
    }

    override fun onGpsStatusChanged(event: Int) {
        mStatus = mLocationManager.getGpsStatus(mStatus)
        AppLog.i(":" + mStatus!!)
        when (event) {
            GpsStatus.GPS_EVENT_STARTED -> {
            }

            GpsStatus.GPS_EVENT_STOPPED -> {
            }

            GpsStatus.GPS_EVENT_FIRST_FIX -> {
            }

            GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        mBroadcastManager.sendBroadcast(LocalIntent.createLocationUpdate(location))
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        AppLog.i(provider + ": " + status)
    }

    override fun onProviderEnabled(provider: String) {
        AppLog.i(provider)
    }

    override fun onProviderDisabled(provider: String) {
        AppLog.i(provider)
    }

    fun stop() {
        AppLog.i("Remove location updates")
        mRequested = false
        mLocationManager.removeUpdates(this)
    }
}
