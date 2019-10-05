package info.anodsplace.headunit.connection

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

import info.anodsplace.headunit.utils.AppLog

class UsbAccessoryConnection(private val usbMgr: UsbManager, private val device: UsbDevice) : AccessoryConnection {
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null

    fun isDeviceRunning(device: UsbDevice): Boolean {
        synchronized(device) {
            if (usbDeviceConnection == null) return false
            return this.device.uniqueName == device.uniqueName
        }
    }

    override fun connect(listener: AccessoryConnection.Listener) {
        try {
            connect(device)
            listener.onConnectionResult(true)
        } catch (e: UsbOpenException) {
            AppLog.e(e)
            listener.onConnectionResult(false)
        }
    }


    @Throws(UsbOpenException::class)
    private fun connect(device: UsbDevice) {
        synchronized(device) {
            disconnect()
            // Open USB device connection
            val newUsbDeviceConnection = usbMgr.openDevice(device) ?: throw UsbOpenException("openDevice: connection is null")
            var newUsbInterface: UsbInterface? = null
            try {
                // Open USB interface
                val interfaceCount = device.interfaceCount
                if (interfaceCount <= 0) {
                    AppLog.e { "interfaceCount: $interfaceCount" }
                    throw UsbOpenException("No usb interfaces")
                }
                newUsbInterface = device.getInterface(0)
                if (!newUsbDeviceConnection.claimInterface(newUsbInterface, true)) {
                    throw UsbOpenException("Error claiming interface")
                }

                // Get endpoints
                var newInEndpoint: UsbEndpoint? = null
                var newOutEndpoint: UsbEndpoint? = null
                for (i in 0 until newUsbInterface.endpointCount) {
                    val ep = newUsbInterface.getEndpoint(i)
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        if (newInEndpoint == null) {
                            newInEndpoint = ep
                        }
                    } else {
                        if (newOutEndpoint == null) {
                            newOutEndpoint = ep
                        }
                    }
                }
                if (newInEndpoint == null) throw UsbOpenException("Could not find inbound endpoint")
                if (newOutEndpoint == null) throw UsbOpenException("Could not find outbound endpoint")

                // We are done! Assign variables
                usbDeviceConnection = newUsbDeviceConnection
                usbInterface = newUsbInterface
                inEndpoint = newInEndpoint
                outEndpoint = newOutEndpoint
            } catch (e: Exception) {
                if (newUsbInterface != null) {
                    newUsbDeviceConnection.releaseInterface(newUsbInterface)
                }
                newUsbDeviceConnection.close()
                throw if (e is UsbOpenException) e else UsbOpenException(e)
            }
        }
    }

    override fun disconnect() {
        synchronized(device) {
            if (usbDeviceConnection != null) {
                if (usbInterface != null) {
                    usbDeviceConnection!!.releaseInterface(usbInterface)
                }
                usbDeviceConnection!!.close()
            }
            usbDeviceConnection = null
            usbInterface = null
            inEndpoint = null
            outEndpoint = null
        }
    }

    private inline fun useUsbDeviceConnection(action: (UsbDeviceConnection) -> Int): Int {
        synchronized(device) {
            return usbDeviceConnection.let {
                if (it == null) {
                    AppLog.e { "Not connected" }
                    -1
                } else {
                    action(it)
                }
            }
        }
    }

    override val isConnected: Boolean
        get() {
            synchronized(device) {
                return usbDeviceConnection == null
            }
        }

    override val isSingleMessage = false

    /**
     * @return length of data transferred (or zero) for success, or negative value for failure
     */
    override fun write(buf: ByteArray, length: Int, timeout: Int) = useUsbDeviceConnection {
        try {
            it.bulkTransfer(outEndpoint, buf, length, timeout)
        } catch (e: NullPointerException) {
            disconnect()
            AppLog.e(e)
            -1
        }
    }

    override fun read(buf: ByteArray, length: Int, timeout: Int) = useUsbDeviceConnection {
        try {
            it.bulkTransfer(inEndpoint, buf, length, timeout)
        } catch (e: NullPointerException) {
            disconnect()
            AppLog.e(e)
            -1
        }
    }

    private inner class UsbOpenException : Exception {
        internal constructor(message: String) : super(message)
        internal constructor(throwable: Throwable) : super(throwable)
    }
}
