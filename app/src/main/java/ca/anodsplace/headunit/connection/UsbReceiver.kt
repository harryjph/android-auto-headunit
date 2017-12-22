package ca.anodsplace.headunit.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

import ca.anodsplace.headunit.utils.AppLog

class UsbReceiver(private val mListener: UsbReceiver.Listener)          // USB Broadcast Receiver enabled by start() & disabled by stop()
    : BroadcastReceiver() {


    interface Listener {
        fun onUsbDetach(device: UsbDevice)
        fun onUsbAttach(device: UsbDevice)
        fun onUsbPermission(granted: Boolean, connect: Boolean, device: UsbDevice)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val device: UsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: return
        AppLog.i("USB Intent: " + intent)

        val action = intent.action
        if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {    // If detach...
            mListener.onUsbDetach(device)                                  // Handle detached device
        } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {// If attach...
            mListener.onUsbAttach(device)
        } else if (action == ACTION_USB_DEVICE_PERMISSION) {                 // If Our App specific Intent for permission request...
            val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            val connect = intent.getBooleanExtra(EXTRA_CONNECT, false)
            mListener.onUsbPermission(permissionGranted, connect, device)
        }
    }

    companion object {
        const val ACTION_USB_DEVICE_PERMISSION = "ca.yyx.hu.ACTION_USB_DEVICE_PERMISSION"
        const val EXTRA_CONNECT = "EXTRA_CONNECT"

        fun createFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            filter.addAction(ACTION_USB_DEVICE_PERMISSION)
            return filter
        }

        fun match(action: String): Boolean {
            if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                return true
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                return true
            } else if (action == ACTION_USB_DEVICE_PERMISSION) {
                return true
            }
            return false
        }
    }
}