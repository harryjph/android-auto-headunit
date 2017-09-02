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

/**
 * @author algavris
 * *
 * @date 06/12/2016.
 */

class GpsLocation internal constructor(context: Context) : GpsStatus.Listener, LocationListener {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val broadcastManager = LocalBroadcastManager.getInstance(context)
    private var gpsStatus: GpsStatus? = null
    private var requested: Boolean = false

    init {
        // Acquire a reference to the system Location Manager
        locationManager.addGpsStatusListener(this)
    }

    fun start() {
        if (requested) {
            return
        }
        AppLog.i("Request location updates")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.0f, this)
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        AppLog.i("Last known location:" + location.toString())
        requested = true
    }

    override fun onGpsStatusChanged(event: Int) {
        gpsStatus = locationManager.getGpsStatus(gpsStatus)
        when (event) {
            GpsStatus.GPS_EVENT_STARTED -> {
                AppLog.i("Started")
            }

            GpsStatus.GPS_EVENT_STOPPED -> {
                AppLog.i("Started")
            }

            GpsStatus.GPS_EVENT_FIRST_FIX -> {
                AppLog.i("First fix")
            }

            GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                AppLog.i("Satellite status")
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        broadcastManager.sendBroadcast(LocalIntent.createLocationUpdate(location))
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
        requested = false
        locationManager.removeUpdates(this)
    }
}
