package ca.yyx.hu.connection

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager

import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Utils

/**
 * @author algavris
 * *
 * @date 29/05/2016.
 */

//  private static final int ACC_REQ_REGISTER_HID       = 54;
//  private static final int ACC_REQ_UNREGISTER_HID     = 55;
//  private static final int ACC_REQ_SET_HID_REPORT_DESC= 56;
//  private static final int ACC_REQ_SEND_HID_EVENT     = 57;
//  private static final int ACC_REQ_AUDIO              = 58;

class UsbModeSwitch(private val mUsbMgr: UsbManager) {

    fun switchMode(device: UsbDevice): Boolean {
        val connection: UsbDeviceConnection?
        try {
            connection = mUsbMgr.openDevice(device)                 // Open device for connection
        } catch (e: Throwable) {
            AppLog.e(e)
            return false
        }

        if (connection == null) {
            AppLog.e("Cannot open device")
            return false
        }

        val result = switchMode(connection)
        connection.close()

        AppLog.i("Result: " + result)
        return result
    }

    private fun switchMode(connection: UsbDeviceConnection): Boolean {
        // Do accessory negotiation and attempt to switch to accessory mode. Called only by usb_connect()
        val buffer = ByteArray(2)
        var len = connection.controlTransfer(UsbConstants.USB_DIR_IN or UsbConstants.USB_TYPE_VENDOR, ACC_REQ_GET_PROTOCOL, 0, 0, buffer, 2, USB_TIMEOUT_IN_MS)
        if (len != 2) {
            AppLog.e("Error controlTransfer len: " + len)
            return false
        }
        val acc_ver = Utils.getAccVersion(buffer)
        // Get OAP / ACC protocol version
        AppLog.i("Success controlTransfer len: $len  acc_ver: $acc_ver")
        if (acc_ver < 1) {
            // If error or version too low...
            AppLog.e("No support acc")
            return false
        }
        AppLog.i("acc_ver: " + acc_ver)

        // Send all accessory identification strings
        initStringControlTransfer(connection, ACC_IDX_MAN, MANUFACTURER)
        initStringControlTransfer(connection, ACC_IDX_MOD, MODEL)
        //initStringControlTransfer (conn, ACC_IDX_DES, AppLog.str_DES);
        //initStringControlTransfer (conn, ACC_IDX_VER, AppLog.str_VER);
        //initStringControlTransfer (conn, ACC_IDX_URI, AppLog.str_URI);
        //initStringControlTransfer (conn, ACC_IDX_SER, AppLog.str_SER);

        AppLog.i("Sending acc start")
        // Send accessory start request. Device should re-enumerate as an accessory.
        len = connection.controlTransfer(UsbConstants.USB_TYPE_VENDOR, ACC_REQ_START, 0, 0, byteArrayOf(), 0, USB_TIMEOUT_IN_MS)
        return len == 0
    }

    private fun initStringControlTransfer(conn: UsbDeviceConnection, index: Int, string: String) {
        val len = conn.controlTransfer(UsbConstants.USB_TYPE_VENDOR, ACC_REQ_SEND_STRING, 0, index, string.toByteArray(), string.length, USB_TIMEOUT_IN_MS)
        if (len != string.length) {
            AppLog.e("Error controlTransfer len: $len  index: $index  string: \"$string\"")
        } else {
            AppLog.i("Success controlTransfer len: $len  index: $index  string: \"$string\"")
        }
    }

    companion object {

        private val USB_TIMEOUT_IN_MS = 100
        private val MANUFACTURER = "Android"//"Mike";                    // Manufacturer
        private val MODEL = "Android Auto"//"Android Open Automotive Protocol"  // Model
        //    private static String str_DES = "Head Unit";                           // Description
        //    private static String str_VER = "1.0";                                 // Version
        //    private static String str_URI = "http://www.android.com/";             // URI
        //    private static String str_SER = "0";//000000012345678";                // Serial #

        // "Android", "Android Open Automotive Protocol", "Description", "VersionName", "https://developer.android.com/auto/index.html", "62skidoo"
        // "Android", "Android Auto", "Description", "VersionName", "https://developer.android.com/auto/index.html", "62skidoo"

        // Indexes for strings sent by the host via ACC_REQ_SEND_STRING:
        private val ACC_IDX_MAN = 0
        private val ACC_IDX_MOD = 1
        //private static final int ACC_IDX_DES = 2;
        //private static final int ACC_IDX_VER = 3;
        //private static final int ACC_IDX_URI = 4;
        //private static final int ACC_IDX_SER = 5;
        // OAP Control requests:
        private val ACC_REQ_GET_PROTOCOL = 51
        private val ACC_REQ_SEND_STRING = 52
        private val ACC_REQ_START = 53
    }

}
