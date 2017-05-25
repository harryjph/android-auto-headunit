package ca.yyx.hu.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location

import java.util.HashSet

import ca.yyx.hu.connection.UsbDeviceCompat

/**
 * @author algavris
 * *
 * @date 21/05/2016.
 */

class Settings(context: Context) {

    private val mPrefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun isConnectingDevice(deviceCompat: UsbDeviceCompat): Boolean {
        val allowDevices = mPrefs.getStringSet("allow-devices", null) ?: return false
        return allowDevices.contains(deviceCompat.uniqueName)
    }

    var allowedDevices: Set<String>
        get() = mPrefs.getStringSet("allow-devices", HashSet<String>())
        set(devices) {
            mPrefs.edit().putStringSet("allow-devices", devices).apply()
        }

    var networkAddresses: Set<String>
        get() = mPrefs.getStringSet("network-addresses", HashSet<String>())
        set(addrs) {
            mPrefs.edit().putStringSet("network-addresses", addrs).apply()
        }

    val bluetoothAddress: String
        get() = mPrefs.getString("bt-address", "40:EF:4C:A3:CB:A5")

    var lastKnownLocation: Location
        get() {
            val latitude = mPrefs.getLong("last-loc-latitude", (32.0864169).toLong())
            val longitude = mPrefs.getLong("last-loc-longitude", (34.7557871).toLong())

            val location = Location("")
            location.latitude = latitude.toDouble()
            location.longitude = longitude.toDouble()
            return location
        }
        set(location) {
            mPrefs.edit()
                .putLong("last-loc-latitude", location.latitude.toLong())
                .putLong("last-loc-longitude", location.longitude.toLong())
                .apply()
        }

    @SuppressLint("ApplySharedPref")
    fun commit() {
        mPrefs.edit().commit()
    }
}
