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

    var bluetoothAddress: String
        get() = mPrefs.getString("bt-address", "40:EF:4C:A3:CB:A5")
        set(value) = mPrefs.edit().putString("bt-address", value).apply()

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

    var micSampleRate: Int
        get() = mPrefs.getInt("mic-sample-rate", 8000)
        set(sampleRate) {
            mPrefs.edit().putInt("mic-sample-rate", sampleRate).apply()
        }

    var nightMode: NightMode
        get() {
            val value = mPrefs.getInt("night-mode", 0)
            val mode = NightMode.fromInt(value)
            return mode!!
        }
        set(nightMode) {
            mPrefs.edit().putInt("night-mode", nightMode.value).apply()
        }

    var keyCodes: MutableMap<Int, Int>
        get() {
            val set = mPrefs.getStringSet("key-codes", mutableSetOf())
            val map = mutableMapOf<Int, Int>()
            set.forEach({
                val codes = it.split("-")
                map.put(codes[0].toInt(), codes[1].toInt())
            })
            return map
        }
        set(codesMap) {
            val list: List<String> = codesMap.map { "${it.key}-${it.value}" }
            mPrefs.edit().putStringSet("key-codes", list.toSet()).apply()
        }

    @SuppressLint("ApplySharedPref")
    fun commit() {
        mPrefs.edit().commit()
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
