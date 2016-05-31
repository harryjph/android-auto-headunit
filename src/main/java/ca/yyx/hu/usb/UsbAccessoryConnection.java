package ca.yyx.hu.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 29/05/2016.
 */

public class UsbAccessoryConnection {

    private static String MAN = "Android";//"Mike";                    // Manufacturer
    private static String MOD = "Android Auto";//"Android Open Automotive Protocol"  // Model
//    private static String str_DES = "Head Unit";                           // Description
//    private static String str_VER = "1.0";                                 // Version
//    private static String str_URI = "http://www.android.com/";             // URI
//    private static String str_SER = "0";//000000012345678";                // Serial #

    // "Android", "Android Open Automotive Protocol", "Description", "VersionName", "https://developer.android.com/auto/index.html", "62skidoo"
    // "Android", "Android Auto", "Description", "VersionName", "https://developer.android.com/auto/index.html", "62skidoo"

    // Indexes for strings sent by the host via ACC_REQ_SEND_STRING:
    private static final int ACC_IDX_MAN = 0;
    private static final int ACC_IDX_MOD = 1;
    //private static final int ACC_IDX_DES = 2;
    //private static final int ACC_IDX_VER = 3;
    //private static final int ACC_IDX_URI = 4;
    //private static final int ACC_IDX_SER = 5;
    // OAP Control requests:
    private static final int ACC_REQ_GET_PROTOCOL = 51;
    private static final int ACC_REQ_SEND_STRING = 52;
    private static final int ACC_REQ_START = 53;
    //  private static final int ACC_REQ_REGISTER_HID       = 54;
    //  private static final int ACC_REQ_UNREGISTER_HID     = 55;
    //  private static final int ACC_REQ_SET_HID_REPORT_DESC= 56;
    //  private static final int ACC_REQ_SEND_HID_EVENT     = 57;
    //  private static final int ACC_REQ_AUDIO              = 58;

    private UsbManager mUsbMgr;
    private UsbDeviceCompat mUsbDeviceConnected;
    private UsbDeviceConnection mUsbDeviceConnection = null;                   // USB Device connection
    private UsbInterface mUsbInterface = null;                   // USB Interface
    private UsbEndpoint m_usb_ep_in = null;                   // USB Input  endpoint
    private UsbEndpoint m_usb_ep_out = null;                   // USB Output endpoint
    private int m_ep_in_addr = -1;                                      // Input  endpoint Value  129
    private int m_ep_out_addr = -1;                                      // Output endpoint Value    2

    public UsbAccessoryConnection(UsbManager usbMgr) {
        mUsbMgr = usbMgr;
    }

    public boolean isDeviceRunning(UsbDeviceCompat deviceCompat) {
        return deviceCompat.equals(mUsbDeviceConnected);
    }

    public boolean connect(UsbDeviceCompat device) throws UsbOpenException {                         // Attempt to connect. Called only by usb_attach_handler() & presets_select()
        if (mUsbDeviceConnection != null) {
            disconnect();
        }
        try {
            usb_open(device.getWrappedDevice());                                        // Open USB device & claim interface
        } catch (UsbOpenException e) {
            disconnect();                                                // Ensure state is disconnected
            throw e;
        }

        int ret = acc_mode_endpoints_set();                                  // Set Accessory mode Endpoints
        if (ret < 0) {                                                    // If error...
            disconnect();                                              // Ensure state is disconnected
            return false;
        }
        mUsbDeviceConnected = device;
        return true;
    }

    public boolean switchMode(UsbDeviceCompat device) throws UsbOpenException {
        UsbDeviceConnection connection;
        try {
            connection = mUsbMgr.openDevice(device.getWrappedDevice());                 // Open device for connection
        } catch (Throwable e) {
            Utils.loge(e);                                  // java.lang.IllegalArgumentException: device /dev/bus/usb/001/019 does not exist or is restricted
            throw new UsbOpenException(e);
        }
        return acc_mode_switch(connection);
    }

    private void usb_open(UsbDevice device) throws UsbOpenException {                             // Open USB device connection & claim interface. Called only by usb_connect()
        try {
            mUsbDeviceConnection = mUsbMgr.openDevice(device);                 // Open device for connection
        } catch (Throwable e) {
            Utils.loge(e);                                  // java.lang.IllegalArgumentException: device /dev/bus/usb/001/019 does not exist or is restricted
            throw new UsbOpenException(e);
        }

        Utils.logd("Established connection: " + mUsbDeviceConnection);

        try {
            int iface_cnt = device.getInterfaceCount();
            if (iface_cnt <= 0) {
                Utils.loge("iface_cnt: " + iface_cnt);
                throw new UsbOpenException("No usb interfaces");
            }
            Utils.logd("iface_cnt: " + iface_cnt);
            mUsbInterface = device.getInterface(0);                            // java.lang.ArrayIndexOutOfBoundsException: length=0; index=0

            if (!mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {        // Claim interface, if error...   true = take from kernel
                throw new UsbOpenException("Error claiming interface");
            }
        } catch (Throwable e) {
            Utils.loge(e);           // Nexus 7 2013:    Throwable: java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
            throw new UsbOpenException(e);
        }
    }

    private int acc_mode_endpoints_set() {                               // Set Accessory mode Endpoints. Called only by usb_connect()
        Utils.logd("Check accessory endpoints");
        m_usb_ep_in = null;                                               // Setup bulk endpoints.
        m_usb_ep_out = null;
        m_ep_in_addr = -1;     // 129
        m_ep_out_addr = -1;     // 2

        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {        // For all USB endpoints...
            UsbEndpoint ep = mUsbInterface.getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {              // If IN
                if (m_usb_ep_in == null) {                                      // If Bulk In not set yet...
                    m_ep_in_addr = ep.getAddress();
                    Utils.logd("Bulk IN m_ep_in_addr: %d  %d", m_ep_in_addr, i);
                    m_usb_ep_in = ep;                                             // Set Bulk In
                }
            } else {                                                            // Else if OUT...
                if (m_usb_ep_out == null) {                                     // If Bulk Out not set yet...
                    m_ep_out_addr = ep.getAddress();
                    Utils.logd("Bulk OUT m_ep_out_addr: %d  %d", m_ep_out_addr, i);
                    m_usb_ep_out = ep;                                            // Set Bulk Out
                }
            }
        }
        if (m_usb_ep_in == null || m_usb_ep_out == null) {
            Utils.loge("Unable to find bulk endpoints");
            return (-1);                                                      // Done error
        }

        Utils.logd("Connected have EPs");
        return (0);                                                         // Done success
    }

    private boolean acc_mode_switch(UsbDeviceConnection conn) {
        // Do accessory negotiation and attempt to switch to accessory mode. Called only by usb_connect()
        byte buffer[] = new byte[2];
        int len = conn.controlTransfer(UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_VENDOR, ACC_REQ_GET_PROTOCOL, 0, 0, buffer, 2, 10000);
        if (len != 2) {
            Utils.loge("Error controlTransfer len: " + len);
            return false;
        }
        int acc_ver = (buffer[1] << 8) | buffer[0];                      // Get OAP / ACC protocol version
        Utils.logd("Success controlTransfer len: " + len + "  acc_ver: " + acc_ver);
        if (acc_ver < 1) {                                                  // If error or version too low...
            Utils.loge("No support acc");
            return false;
        }
        Utils.logd("acc_ver: " + acc_ver);

        // Send all accessory identification strings
        usb_acc_string_send(conn, ACC_IDX_MAN, MAN);            // Manufacturer
        usb_acc_string_send(conn, ACC_IDX_MOD, MOD);            // Model
        //usb_acc_string_send (conn, ACC_IDX_DES, Utils.str_DES);
        //usb_acc_string_send (conn, ACC_IDX_VER, Utils.str_VER);
        //usb_acc_string_send (conn, ACC_IDX_URI, Utils.str_URI);
        //usb_acc_string_send (conn, ACC_IDX_SER, Utils.str_SER);

        Utils.logd("Sending acc start");
        // Send accessory start request. Device should re-enumerate as an accessory.
        len = conn.controlTransfer(UsbConstants.USB_DIR_OUT | UsbConstants.USB_TYPE_VENDOR, ACC_REQ_START, 0, 0, null, 0, 10000);
        if (len != 0) {
            Utils.loge("Error acc start");
            return false;
        }
        Utils.logd("OK acc start. Wait to re-enumerate...");
        return true;
    }

    public void disconnect() {                                           // Release interface and close USB device connection. Called only by usb_disconnect()
        if (mUsbDeviceConnected != null) {
            Utils.logd(mUsbDeviceConnected.toString());
        }
        m_usb_ep_in = null;                                               // Input  EP
        m_usb_ep_out = null;                                               // Output EP
        m_ep_in_addr = -1;                                                 // Input  endpoint Value
        m_ep_out_addr = -1;                                                 // Output endpoint Value

        if (mUsbDeviceConnection != null) {
            boolean bret = false;
            if (mUsbInterface != null) {
                bret = mUsbDeviceConnection.releaseInterface(mUsbInterface);
            }
            if (bret) {
                Utils.logd("OK releaseInterface()");
            } else {
                Utils.loge("Error releaseInterface()");
            }

            mUsbDeviceConnection.close();                                        //
        }
        mUsbDeviceConnection = null;
        mUsbInterface = null;
        mUsbDeviceConnected = null;
    }

    public void usb_force() {
        if (mUsbDeviceConnected != null) {
            disconnect();
        }

        if (Utils.su_installed_get()) {
            String cmd = "setenforce 0 ; chmod -R 777 /dev/bus 1>/dev/null 2>/dev/null";
            Utils.sys_run(cmd, true);
        }

        m_ep_in_addr = 255;
        m_ep_out_addr = 0;  // USB Force
    }

    // Send one accessory identification string.    Called only by acc_mode_switch()
    private void usb_acc_string_send(UsbDeviceConnection conn, int index, String string) {
        byte[] buffer = (string + "\0").getBytes();
        int len = conn.controlTransfer(UsbConstants.USB_TYPE_VENDOR, ACC_REQ_SEND_STRING, 0, index, buffer, buffer.length, 10000);
        if (len != buffer.length) {
            Utils.loge("Error controlTransfer len: " + len + "  index: " + index + "  string: \"" + string + "\"");
        } else {
            Utils.logd("Success controlTransfer len: " + len + "  index: " + index + "  string: \"" + string + "\"");
        }
    }

    public int getEndpointInAddr() {
        return m_ep_in_addr;
    }

    public int getEndpointOutAddr() {
        return m_ep_out_addr;
    }

    public boolean isConnected() {
        return mUsbDeviceConnected != null;
    }

    public class UsbOpenException extends Throwable {
        public UsbOpenException(Throwable e) {
            super(e);
        }

        public UsbOpenException(String error) {
            super(error);
        }
    }
}
