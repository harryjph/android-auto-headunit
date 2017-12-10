package ca.yyx.hu.connection

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 29/05/2016.
 */
class UsbAccessoryConnection(private val usbMgr: UsbManager, private val device: UsbDevice) : AccessoryConnection {
    private var usbDeviceConnected: UsbDeviceCompat? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null                   // USB Device connection
    private var usbInterface: UsbInterface? = null                   // USB Interface
    private var endpointIn: UsbEndpoint? = null                   // USB Input  endpoint
    private var endpointOut: UsbEndpoint? = null                   // USB Output endpoint

    fun isDeviceRunning(device: UsbDevice): Boolean {
        synchronized(sLock) {
            val connected = usbDeviceConnected ?: return false
            return UsbDeviceCompat.getUniqueName(device) == connected.uniqueName
        }
    }

    override fun connect(listener: AccessoryConnection.Listener) {
        try {
            val result = connect(device)
            listener.onConnectionResult(result)
        } catch (e: UsbOpenException) {
            AppLog.e(e)
            listener.onConnectionResult(false)
        }
    }


    @Throws(UsbOpenException::class)
    private fun connect(device: UsbDevice): Boolean {
        if (usbDeviceConnection != null) {
            disconnect()
        }
        synchronized(sLock) {
            try {
                usbOpen(device)                                        // Open USB device & claim interface
            } catch (e: UsbOpenException) {
                disconnect()                                                // Ensure state is disconnected
                throw e
            }

            val ret = initEndpoint()                                  // Set Accessory mode Endpoints
            if (ret < 0) {                                                    // If error...
                disconnect()                                              // Ensure state is disconnected
                return false
            }

            usbDeviceConnected = UsbDeviceCompat(device)
            return true
        }
    }

    @Throws(UsbOpenException::class)
    private fun usbOpen(device: UsbDevice) {
        try {
            usbDeviceConnection = usbMgr.openDevice(device)
        } catch (e: Throwable) {
            AppLog.e(e)
            throw UsbOpenException(e)
        }

        if (usbDeviceConnection == null) {
            throw UsbOpenException("openDevice: connection is null")
        }

        AppLog.i("Established connection: " + usbDeviceConnection!!)

        try {
            val interfaceCount = device.interfaceCount
            if (interfaceCount <= 0) {
                AppLog.e("interfaceCount: " + interfaceCount)
                throw UsbOpenException("No usb interfaces")
            }
            AppLog.i("interfaceCount: " + interfaceCount)
            usbInterface = device.getInterface(0)                            // java.lang.ArrayIndexOutOfBoundsException: length=0; index=0

            if (!usbDeviceConnection!!.claimInterface(usbInterface, true)) {        // Claim interface, if error...   true = take from kernel
                throw UsbOpenException("Error claiming interface")
            }
        } catch (e: Throwable) {
            AppLog.e(e)           // Nexus 7 2013:    Throwable: java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
            throw UsbOpenException(e)
        }

    }

    private fun initEndpoint(): Int {                               // Set Accessory mode Endpoints. Called only by usb_connect()
        AppLog.i("Check accessory endpoints")
        endpointIn = null                                               // Setup bulk endpoints.
        endpointOut = null


        for (i in 0 until usbInterface!!.endpointCount) {        // For all USB endpoints...
            val ep = usbInterface!!.getEndpoint(i)
            if (ep.direction == UsbConstants.USB_DIR_IN) {              // If IN
                if (endpointIn == null) {                                      // If Bulk In not set yet...
                    endpointIn = ep                                             // Set Bulk In
                }
            } else {                                                            // Else if OUT...
                if (endpointOut == null) {                                     // If Bulk Out not set yet...
                    endpointOut = ep                                            // Set Bulk Out
                }
            }
        }
        if (endpointIn == null || endpointOut == null) {
            AppLog.e("Unable to find bulk endpoints")
            return -1                                                      // Done error
        }

        AppLog.i("Connected have EPs")
        return 0                                                         // Done success
    }

    override fun disconnect() {                                           // Release interface and close USB device connection. Called only by usb_disconnect()
        synchronized(sLock) {
            if (usbDeviceConnected != null) {
                AppLog.i(usbDeviceConnected!!.toString())
            }
            endpointIn = null                                               // Input  EP
            endpointOut = null                                               // Output EP

            if (usbDeviceConnection != null) {
                var bret = false
                if (usbInterface != null) {
                    bret = usbDeviceConnection!!.releaseInterface(usbInterface)
                }
                if (bret) {
                    AppLog.i("OK releaseInterface()")
                } else {
                    AppLog.e("Error releaseInterface()")
                }

                usbDeviceConnection!!.close()                                        //
            }
            usbDeviceConnection = null
            usbInterface = null
            usbDeviceConnected = null
        }
    }

    override val isConnected: Boolean
        get() = usbDeviceConnected != null

    override val isSingleMessage: Boolean
        get() = false

    /**
     * @return length of data transferred (or zero) for success,
     * * or negative value for failure
     */
    override fun send(buf: ByteArray, length: Int, timeout: Int): Int {
        synchronized(sLock) {
            if (usbDeviceConnected == null) {
                AppLog.e("Not connected")
                return -1
            }
            try {
                return usbDeviceConnection!!.bulkTransfer(endpointOut, buf, length, timeout)
            } catch (e: NullPointerException) {
                disconnect()
                AppLog.e(e)
                return -1
            }

        }
    }

    override fun recv(buf: ByteArray, length: Int, timeout: Int): Int {
        synchronized(sLock) {
            if (usbDeviceConnected == null) {
                AppLog.e("Not connected")
                return -1
            }
            try {
                return usbDeviceConnection!!.bulkTransfer(endpointIn, buf, buf.size, timeout)
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
