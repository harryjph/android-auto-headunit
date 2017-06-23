package ca.yyx.hu.utils

import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import android.location.LocationManager
import android.support.v4.view.KeyEventCompat
import android.view.KeyEvent

/**
 * @author algavris
 * *
 * @date 30/05/2016.
 */

object LocalIntent {

    const val ACTION_DISCONNECT = "ca.yyx.hu.ACTION_DISCONNECT"
    const val ACTION_LOCATION_UPDATE = "ca.yyx.hu.LOCATION_UPDATE"

    val FILTER_DISCONNECT = IntentFilter("ca.yyx.hu.ACTION_DISCONNECT")
    val FILTER_LOCATION_UPDATE = IntentFilter("ca.yyx.hu.LOCATION_UPDATE")
    val FILTER_KEY_EVENT = IntentFilter("ca.yyx.hu.ACTION_KEYPRESS")

    const val EXTRA_EVENT = "event"

    fun createKeyEvent(event: KeyEvent): Intent {
        val intent = Intent("ca.yyx.hu.ACTION_KEYPRESS")
        intent.putExtra(EXTRA_EVENT, event)
        return intent
    }

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
