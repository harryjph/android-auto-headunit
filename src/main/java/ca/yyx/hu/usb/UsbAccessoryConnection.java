package ca.yyx.hu.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 29/05/2016.
 */

public class UsbAccessoryConnection
{
    private UsbManager mUsbMgr;
    private UsbDeviceCompat mUsbDeviceConnected;
    private UsbDeviceConnection mUsbDeviceConnection = null;                   // USB Device connection
    private UsbInterface mUsbInterface = null;                   // USB Interface
    private UsbEndpoint mEndpointIn = null;                   // USB Input  endpoint
    private UsbEndpoint mEndpointOut = null;                   // USB Output endpoint
    private int m_ep_in_addr = -1;                                      // Input  endpoint Value  129
    private int m_ep_out_addr = -1;                                      // Output endpoint Value    2

    private static final Object sLock = new Object();

    public UsbAccessoryConnection(UsbManager usbMgr) {
        mUsbMgr = usbMgr;
    }

    public boolean isDeviceRunning(UsbDevice device) {
        synchronized (sLock) {
            if (mUsbDeviceConnected == null) {
                return false;
            }
            return UsbDeviceCompat.getUniqueName(device).equals(mUsbDeviceConnected.getUniqueName());
        }
    }

    public boolean connect(UsbDevice device) throws UsbOpenException {                         // Attempt to connect. Called only by usb_attach_handler() & presets_select()
        if (mUsbDeviceConnection != null) {
            disconnect();
        }
        synchronized (sLock) {
            try {
                usb_open(device);                                        // Open USB device & claim interface
            } catch (UsbOpenException e) {
                disconnect();                                                // Ensure state is disconnected
                throw e;
            }

            int ret = acc_mode_endpoints_set();                                  // Set Accessory mode Endpoints
            if (ret < 0) {                                                    // If error...
                disconnect();                                              // Ensure state is disconnected
                return false;
            }

            mUsbDeviceConnected = new UsbDeviceCompat(device);
            return true;
        }
    }

    private void usb_open(UsbDevice device) throws UsbOpenException {                             // Open USB device connection & claim interface. Called only by usb_connect()
        try {
            mUsbDeviceConnection = mUsbMgr.openDevice(device);                 // Open device for connection
        } catch (Throwable e) {
            AppLog.loge(e);                                  // java.lang.IllegalArgumentException: device /dev/bus/usb/001/019 does not exist or is restricted
            throw new UsbOpenException(e);
        }

        AppLog.logd("Established connection: " + mUsbDeviceConnection);

        try {
            int iface_cnt = device.getInterfaceCount();
            if (iface_cnt <= 0) {
                AppLog.loge("iface_cnt: " + iface_cnt);
                throw new UsbOpenException("No usb interfaces");
            }
            AppLog.logd("iface_cnt: " + iface_cnt);
            mUsbInterface = device.getInterface(0);                            // java.lang.ArrayIndexOutOfBoundsException: length=0; index=0

            if (!mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {        // Claim interface, if error...   true = take from kernel
                throw new UsbOpenException("Error claiming interface");
            }
        } catch (Throwable e) {
            AppLog.loge(e);           // Nexus 7 2013:    Throwable: java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
            throw new UsbOpenException(e);
        }
    }

    private int acc_mode_endpoints_set() {                               // Set Accessory mode Endpoints. Called only by usb_connect()
        AppLog.logd("Check accessory endpoints");
        mEndpointIn = null;                                               // Setup bulk endpoints.
        mEndpointOut = null;
        m_ep_in_addr = -1;     // 129
        m_ep_out_addr = -1;     // 2


        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {        // For all USB endpoints...
            UsbEndpoint ep = mUsbInterface.getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {              // If IN
                if (mEndpointIn == null) {                                      // If Bulk In not set yet...
                    m_ep_in_addr = ep.getAddress();
                    AppLog.logd("Bulk IN m_ep_in_addr: %d  %d", m_ep_in_addr, i);
                    mEndpointIn = ep;                                             // Set Bulk In
                }
            } else {                                                            // Else if OUT...
                if (mEndpointOut == null) {                                     // If Bulk Out not set yet...
                    m_ep_out_addr = ep.getAddress();
                    AppLog.logd("Bulk OUT m_ep_out_addr: %d  %d", m_ep_out_addr, i);
                    mEndpointOut = ep;                                            // Set Bulk Out
                }
            }
        }
        if (mEndpointIn == null || mEndpointOut == null) {
            AppLog.loge("Unable to find bulk endpoints");
            return (-1);                                                      // Done error
        }

        AppLog.logd("Connected have EPs");
        return (0);                                                         // Done success
    }

    public void disconnect() {                                           // Release interface and close USB device connection. Called only by usb_disconnect()
        synchronized (sLock) {
            if (mUsbDeviceConnected != null) {
                AppLog.logd(mUsbDeviceConnected.toString());
            }
            mEndpointIn = null;                                               // Input  EP
            mEndpointOut = null;                                               // Output EP

            if (mUsbDeviceConnection != null) {
                boolean bret = false;
                if (mUsbInterface != null) {
                    bret = mUsbDeviceConnection.releaseInterface(mUsbInterface);
                }
                if (bret) {
                    AppLog.logd("OK releaseInterface()");
                } else {
                    AppLog.loge("Error releaseInterface()");
                }

                mUsbDeviceConnection.close();                                        //
            }
            mUsbDeviceConnection = null;
            mUsbInterface = null;
            mUsbDeviceConnected = null;
        }
    }

    public boolean isConnected() {
        return mUsbDeviceConnected != null;
    }

    /**
     * @return length of data transferred (or zero) for success,
     * or negative value for failure
     */
    public int send(byte[] buf, int length, int timeout) {
        synchronized (sLock) {
            if (mUsbDeviceConnected == null) {
                AppLog.loge("Not connected");
                return -1;
            }
            try {
                return mUsbDeviceConnection.bulkTransfer(mEndpointOut, buf, length, timeout);
            } catch (NullPointerException e) {
                disconnect();
                AppLog.loge(e);
                return -1;
            }
        }
    }

    public int recv(byte[] buf, int timeout) {
        synchronized (sLock) {
            if (mUsbDeviceConnected == null) {
                AppLog.loge("Not connected");
                return -1;
            }
            try {
                return mUsbDeviceConnection.bulkTransfer(mEndpointIn, buf, buf.length, timeout);
            } catch (NullPointerException e) {
                disconnect();
                AppLog.loge(e);
                return -1;
            }
        }
    }

    public class UsbOpenException extends Exception {
        UsbOpenException(String message) {
            super(message);
        }

        UsbOpenException(Throwable tr) {
            super(tr);
        }
    }
}
