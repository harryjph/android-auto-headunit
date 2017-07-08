package ca.yyx.hu.connection

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

import junit.framework.Assert

import java.io.IOException

import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 29/05/2016.
 */

class UsbAccessoryConnection(private val mUsbMgr: UsbManager, private val mDevice: UsbDevice) : AccessoryConnection {
    private var mUsbDeviceConnected: UsbDeviceCompat? = null
    private var mUsbDeviceConnection: UsbDeviceConnection? = null                   // USB Device connection
    private var mUsbInterface: UsbInterface? = null                   // USB Interface
    private var mEndpointIn: UsbEndpoint? = null                   // USB Input  endpoint
    private var mEndpointOut: UsbEndpoint? = null                   // USB Output endpoint

    fun isDeviceRunning(device: UsbDevice): Boolean {
        synchronized(sLock) {
            val connected = mUsbDeviceConnected ?: return false
            return UsbDeviceCompat.getUniqueName(device) == connected.uniqueName
        }
    }

    override fun connect(listener: AccessoryConnection.Listener) {
        try {
            val result = connect(mDevice)
            listener.onConnectionResult(result)
        } catch (e: UsbOpenException) {
            AppLog.e(e)
            listener.onConnectionResult(false)
        }
    }


    @Throws(UsbOpenException::class)
    private fun connect(device: UsbDevice): Boolean {                         // Attempt to connect. Called only by usb_attach_handler() & presets_select()
        if (mUsbDeviceConnection != null) {
            disconnect()
        }
        synchronized(sLock) {
            try {
                usb_open(device)                                        // Open USB device & claim interface
            } catch (e: UsbOpenException) {
                disconnect()                                                // Ensure state is disconnected
                throw e
            }

            val ret = acc_mode_endpoints_set()                                  // Set Accessory mode Endpoints
            if (ret < 0) {                                                    // If error...
                disconnect()                                              // Ensure state is disconnected
                return false
            }

            mUsbDeviceConnected = UsbDeviceCompat(device)
            return true
        }
    }

    @Throws(UsbOpenException::class)
    private fun usb_open(device: UsbDevice) {                             // Open USB device connection & claim interface. Called only by usb_connect()
        try {
            mUsbDeviceConnection = mUsbMgr.openDevice(device)                 // Open device for connection
        } catch (e: Throwable) {
            AppLog.e(e)                                  // java.lang.IllegalArgumentException: device /dev/bus/usb/001/019 does not exist or is restricted
            throw UsbOpenException(e)
        }

        AppLog.i("Established connection: " + mUsbDeviceConnection!!)

        try {
            val iface_cnt = device.interfaceCount
            if (iface_cnt <= 0) {
                AppLog.e("iface_cnt: " + iface_cnt)
                throw UsbOpenException("No usb interfaces")
            }
            AppLog.i("iface_cnt: " + iface_cnt)
            mUsbInterface = device.getInterface(0)                            // java.lang.ArrayIndexOutOfBoundsException: length=0; index=0

            if (!mUsbDeviceConnection!!.claimInterface(mUsbInterface, true)) {        // Claim interface, if error...   true = take from kernel
                throw UsbOpenException("Error claiming interface")
            }
        } catch (e: Throwable) {
            AppLog.e(e)           // Nexus 7 2013:    Throwable: java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
            throw UsbOpenException(e)
        }

    }

    private fun acc_mode_endpoints_set(): Int {                               // Set Accessory mode Endpoints. Called only by usb_connect()
        AppLog.i("Check accessory endpoints")
        mEndpointIn = null                                               // Setup bulk endpoints.
        mEndpointOut = null


        for (i in 0..mUsbInterface!!.endpointCount - 1) {        // For all USB endpoints...
            val ep = mUsbInterface!!.getEndpoint(i)
            if (ep.direction == UsbConstants.USB_DIR_IN) {              // If IN
                if (mEndpointIn == null) {                                      // If Bulk In not set yet...
                    mEndpointIn = ep                                             // Set Bulk In
                }
            } else {                                                            // Else if OUT...
                if (mEndpointOut == null) {                                     // If Bulk Out not set yet...
                    mEndpointOut = ep                                            // Set Bulk Out
                }
            }
        }
        if (mEndpointIn == null || mEndpointOut == null) {
            AppLog.e("Unable to find bulk endpoints")
            return -1                                                      // Done error
        }

        AppLog.i("Connected have EPs")
        return 0                                                         // Done success
    }

    override fun disconnect() {                                           // Release interface and close USB device connection. Called only by usb_disconnect()
        synchronized(sLock) {
            if (mUsbDeviceConnected != null) {
                AppLog.i(mUsbDeviceConnected!!.toString())
            }
            mEndpointIn = null                                               // Input  EP
            mEndpointOut = null                                               // Output EP

            if (mUsbDeviceConnection != null) {
                var bret = false
                if (mUsbInterface != null) {
                    bret = mUsbDeviceConnection!!.releaseInterface(mUsbInterface)
                }
                if (bret) {
                    AppLog.i("OK releaseInterface()")
                } else {
                    AppLog.e("Error releaseInterface()")
                }

                mUsbDeviceConnection!!.close()                                        //
            }
            mUsbDeviceConnection = null
            mUsbInterface = null
            mUsbDeviceConnected = null
        }
    }

    override val isConnected: Boolean
        get() = mUsbDeviceConnected != null

    override val isSingleMessage: Boolean
        get() = false

    /**
     * @return length of data transferred (or zero) for success,
     * * or negative value for failure
     */
    override fun send(buf: ByteArray, length: Int, timeout: Int): Int {
        synchronized(sLock) {
            if (mUsbDeviceConnected == null) {
                AppLog.e("Not connected")
                return -1
            }
            try {
                return mUsbDeviceConnection!!.bulkTransfer(mEndpointOut, buf, length, timeout)
            } catch (e: NullPointerException) {
                disconnect()
                AppLog.e(e)
                return -1
            }

        }
    }

    override fun recv(buf: ByteArray, length: Int, timeout: Int): Int {
        synchronized(sLock) {
            if (mUsbDeviceConnected == null) {
                AppLog.e("Not connected")
                return -1
            }
            try {
                return mUsbDeviceConnection!!.bulkTransfer(mEndpointIn, buf, buf.size, timeout)
            } catch (e: NullPointerException) {
                disconnect()
                AppLog.e(e)
                return -1
            }

        }
    }

    private inner class UsbOpenException : Exception {
        internal constructor(message: String) : super(message)
        internal constructor(tr: Throwable) : super(tr)
    }

    companion object {
        private val sLock = Object()
    }
}
