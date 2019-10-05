package info.anodsplace.headunit.utils

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

val Intent?.usbDevice: UsbDevice?
    get() = this?.getParcelableExtra(UsbManager.EXTRA_DEVICE)
