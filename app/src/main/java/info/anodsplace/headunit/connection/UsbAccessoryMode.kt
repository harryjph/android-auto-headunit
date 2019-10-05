package info.anodsplace.headunit.connection

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager

import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.aap.Utils
import java.lang.Exception

inline class UsbAccessoryMode(private val usbManager: UsbManager) {
    fun connectAndSwitch(device: UsbDevice): Boolean {
        val connection: UsbDeviceConnection?
        try {
            connection = usbManager.openDevice(device)
        } catch (e: Exception) {
            AppLog.e(e)
            return false
        }

        if (connection == null) {
            AppLog.e { "Failed to open device" }
            return false
        }

        val result = switch(connection)
        connection.close()

        AppLog.i { "Result: $result" }
        return result
    }

    private fun switch(connection: UsbDeviceConnection): Boolean {
        // Do accessory negotiation and attempt to switch to accessory mode
        val buffer = ByteArray(2)
        var len = connection.controlTransfer(UsbConstants.USB_DIR_IN or UsbConstants.USB_TYPE_VENDOR, ACC_REQ_GET_PROTOCOL, 0, 0, buffer, 2, USB_TIMEOUT_IN_MS)
        if (len != 2) {
            AppLog.e { "Error controlTransfer len: $len" }
            return false
        }
        val accessoryVersion = Utils.getAccessoryVersion(buffer)
        // Get OAP / ACC protocol version
        AppLog.i { "Success controlTransfer len: $len acc_ver: $accessoryVersion" }
        if (accessoryVersion < 1) {
            // If error or version too low...
            AppLog.e { "No support acc" }
            return false
        }
        AppLog.i { "acc_ver: $accessoryVersion" }

        // Send all accessory identification strings
        initStringControlTransfer(connection, ACC_IDX_MAN, MANUFACTURER)
        initStringControlTransfer(connection, ACC_IDX_MOD, MODEL)
        initStringControlTransfer(connection, ACC_IDX_DES, DESCRIPTION)
        initStringControlTransfer(connection, ACC_IDX_VER, VERSION)
        initStringControlTransfer(connection, ACC_IDX_URI, URI)
        initStringControlTransfer(connection, ACC_IDX_SER, SERIAL)

        AppLog.i { "Sending acc start" }
        // Send accessory start request. Device should re-enumerate as an accessory.
        len = connection.controlTransfer(UsbConstants.USB_TYPE_VENDOR, ACC_REQ_START, 0, 0, byteArrayOf(), 0, USB_TIMEOUT_IN_MS)
        return len == 0
    }

    private fun initStringControlTransfer(conn: UsbDeviceConnection, index: Int, string: String) {
        val len = conn.controlTransfer(UsbConstants.USB_TYPE_VENDOR, ACC_REQ_SEND_STRING, 0, index, string.toByteArray(), string.length, USB_TIMEOUT_IN_MS)
        if (len != string.length) {
            AppLog.e { "controlTransfer did not transfer whole string. Transferred: $len Index: $index String: \"$string\"" }
        }
    }

    companion object {
        private const val USB_TIMEOUT_IN_MS = 100
        private const val MANUFACTURER = "Android"
        private const val MODEL = "Android Auto"
        private const val DESCRIPTION = "Android Auto" //"Android Open Automotive Protocol"
        private const val VERSION = "2.0.1"
        private const val URI = "https://developer.android.com/auto/index.html"
        private const val SERIAL = "HU-AAAAAA001"

        // Indexes for strings sent by the host via ACC_REQ_SEND_STRING:
        private const val ACC_IDX_MAN = 0
        private const val ACC_IDX_MOD = 1
        private const val ACC_IDX_DES = 2
        private const val ACC_IDX_VER = 3
        private const val ACC_IDX_URI = 4
        private const val ACC_IDX_SER = 5

        // OAP Control requests:
        private const val ACC_REQ_GET_PROTOCOL = 51
        private const val ACC_REQ_SEND_STRING = 52
        private const val ACC_REQ_START = 53
    }
}
