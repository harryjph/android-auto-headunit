package ca.anodsplace.headunit.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location

import java.util.HashSet

import ca.anodsplace.headunit.connection.UsbDeviceCompat

/**
 * @author algavris
 * *
 * @date 21/05/2016.
 */

class Settings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun isConnectingDevice(deviceCompat: UsbDeviceCompat): Boolean {
        val allowDevices = prefs.getStringSet("allow-devices", null) ?: return false
        return allowDevices.contains(deviceCompat.uniqueName)
    }

    var allowedDevices: Set<String>
        get() = prefs.getStringSet("allow-devices", HashSet<String>())
        set(devices) {
            prefs.edit().putStringSet("allow-devices", devices).apply()
        }

    var networkAddresses: Set<String>
        get() = prefs.getStringSet("network-addresses", HashSet<String>())
        set(addrs) {
            prefs.edit().putStringSet("network-addresses", addrs).apply()
        }

    var bluetoothAddress: String
        get() = prefs.getString("bt-address", "40:EF:4C:A3:CB:A5")
        set(value) = prefs.edit().putString("bt-address", value).apply()

    var lastKnownLocation: Location
        get() {
            val latitude = prefs.getLong("last-loc-latitude", (32.0864169).toLong())
            val longitude = prefs.getLong("last-loc-longitude", (34.7557871).toLong())

            val location = Location("")
            location.latitude = latitude.toDouble()
            location.longitude = longitude.toDouble()
            return location
        }
        set(location) {
            prefs.edit()
                .putLong("last-loc-latitude", location.latitude.toLong())
                .putLong("last-loc-longitude", location.longitude.toLong())
                .apply()
        }

    var micSampleRate: Int
        get() = prefs.getInt("mic-sample-rate", 8000)
        set(sampleRate) {
            prefs.edit().putInt("mic-sample-rate", sampleRate).apply()
        }

    var useGpsForNavigation: Boolean
        get() = prefs.getBoolean("gps-navigation", true)
        set(value) {
            prefs.edit().putBoolean("gps-navigation", value).apply()
        }

    var nightMode: NightMode
        get() {
            val value = prefs.getInt("night-mode", 0)
            val mode = NightMode.fromInt(value)
            return mode!!
        }
        set(nightMode) {
            prefs.edit().putInt("night-mode", nightMode.value).apply()
        }

    var keyCodes: MutableMap<Int, Int>
        get() {
            val set = prefs.getStringSet("key-codes", mutableSetOf())
            val map = mutableMapOf<Int, Int>()
            set.forEach({
                val codes = it.split("-")
                map.put(codes[0].toInt(), codes[1].toInt())
            })
            return map
        }
        set(codesMap) {
            val list: List<String> = codesMap.map { "${it.key}-${it.value}" }
            prefs.edit().putStringSet("key-codes", list.toSet()).apply()
        }

    @SuppressLint("ApplySharedPref")
    fun commit() {
        prefs.edit().commit()
    }

    enum class NightMode(val value: Int) {
        AUTO(0),
        DAY(1),
        NIGHT(2),
        AUTO_WAIT_GPS(3),
        NONE(4);

        companion object {
            private val map = NightMode.values().associateBy(NightMode::value)
            fun fromInt(value: Int) = map[value]
        }
    }

    companion object {
        val MicSampleRates = hashMapOf(
            8000 to 16000,
            16000 to 8000
        )

        val NightModes = hashMapOf(
            0 to 1,
            1 to 2,
            2 to 3,
            3 to 4,
            4 to 0
        )
    }

}
