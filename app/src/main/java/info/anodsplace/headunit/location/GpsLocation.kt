package info.anodsplace.headunit.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager

import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.DeviceIntent
import info.anodsplace.headunit.contract.LocationUpdateIntent

/**
 * @author algavris
 * *
 * @date 06/12/2016.
 */

class GpsLocation internal constructor(context: Context): LocationListener {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val broadcastManager = LocalBroadcastManager.getInstance(context)
    private var requested: Boolean = false

    fun start() {
        if (requested) {
            return
        }
        AppLog.i("Request location updates")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.0f, this)
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        AppLog.i("Last known location:  ${location?.toString() ?: "Unknown"}")
        requested = true
    }

    override fun onLocationChanged(location: Location) {
        broadcastManager.sendBroadcast(LocationUpdateIntent(location))
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
