package info.anodsplace.headunit.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Toast

import info.anodsplace.headunit.App
import info.anodsplace.headunit.aap.AapService
import info.anodsplace.headunit.connection.UsbAccessoryMode
import info.anodsplace.headunit.connection.isInAccessoryMode
import info.anodsplace.headunit.connection.uniqueName
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.Settings
import info.anodsplace.headunit.utils.usbDevice

class UsbAttachedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLog.i { "USB Intent: $intent" }

        val device = intent.usbDevice
        if (device == null) {
            AppLog.e { "No USB device" }
            finish()
            return
        }

        if (App.provide(this).transport.isAlive) {
            AppLog.e { "Thread already running" }
            finish()
            return
        }

        if (device.isInAccessoryMode) {
            AppLog.e { "Usb in accessory mode" }
            startService(AapService.createIntent(device, this))
            finish()
            return
        }

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val usbMode = UsbAccessoryMode(usbManager)
        AppLog.i { "Switching USB device to accessory mode ${device.uniqueName}" }
        if (!usbMode.connectAndSwitch(device)) {
            Toast.makeText(this, "Failed to connect", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val device = intent.usbDevice
        if (device == null) {
            AppLog.e { "No USB device" }
            finish()
            return
        }

        AppLog.i { device.uniqueName }

        if (!App.provide(this).transport.isAlive) {
            if (device.isInAccessoryMode) {
                AppLog.e { "Usb in accessory mode" }
                startService(AapService.createIntent(device, this))
            }
        } else {
            AppLog.e { "Thread already running" }
        }

        finish()
    }
}
