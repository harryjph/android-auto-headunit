package ca.anodsplace.headunit.utils

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

/**
 * @author algavris
 * @date 22/12/2017
 */
class DeviceIntent(private val intent: Intent?) {
    val device: UsbDevice?
        get() = intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
}