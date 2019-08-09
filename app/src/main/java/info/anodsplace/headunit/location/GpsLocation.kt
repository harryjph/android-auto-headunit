package info.anodsplace.headunit.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import info.anodsplace.headunit.App

import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.contract.LocationUpdateIntent

/**
 * @author algavris
 * *
 * @date 06/12/2016.
 */

class GpsLocation constructor(private val context: Context): LocationListener {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val broadcastManager = App.provide(context).localBroadcastManager
    private var requested: Boolean = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (requested) {
            return
        }
        AppLog.i("Request location updates")
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.0f, this)
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            AppLog.i("Last known location:  ${location?.toString() ?: "Unknown"}")
            requested = true
        }
    }

    override fun onLocationChanged(location: Location) {
        broadcastManager.sendBroadcast(LocationUpdateIntent(location))
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        AppLog.i("$provider: $status")
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
