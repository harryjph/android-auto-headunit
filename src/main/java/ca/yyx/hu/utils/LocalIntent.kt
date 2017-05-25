package ca.yyx.hu.utils

import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import android.location.LocationManager

/**
 * @author algavris
 * *
 * @date 30/05/2016.
 */

object LocalIntent {

    val ACTION_DISCONNECT = Intent("ca.yyx.hu.ACTION_DISCONNECT")
    val FILTER_DISCONNECT = IntentFilter("ca.yyx.hu.ACTION_DISCONNECT")
    val FILTER_LOCATION_UPDATE = IntentFilter("ca.yyx.hu.LOCATION_UPDATE")

    fun createLocationUpdate(location: Location): Intent {
        val intent = Intent("ca.yyx.hu.LOCATION_UPDATED")
        intent.putExtra(LocationManager.KEY_LOCATION_CHANGED, location)
        return intent
    }

    fun extractLocation(intent: Intent): Location {
        return intent.getParcelableExtra<Location>(LocationManager.KEY_LOCATION_CHANGED)
    }

    fun extractDevice(intent: Intent?): UsbDevice? {
        if (intent == null) {
            return null
        }
        return intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
    }

}
