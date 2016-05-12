package ca.yyx.hu.usb;

import android.hardware.usb.UsbDevice;

import java.util.Locale;

import ca.yyx.hu.Utils;

/**
 * @author algavris
 * @date 12/05/2016.
 */
public class UsbDeviceCompat {
    public static final int USB_VID_GOO = 0x18D1;   // 6353   Nexus or ACC mode, see PID to distinguish
    private static final int USB_VID_HTC = 0x0bb4;   // 2996
    private static final int USB_VID_SAM = 0x04e8;   // 1256
    private static final int USB_VID_O1A = 0xfff6;   // 65526    Samsung ?
    private static final int USB_VID_SON = 0x0fce;   // 4046
    private static final int USB_VID_LGE = 0xfff5;   // 65525
    private static final int USB_VID_MOT = 0x22b8;   // 8888


    private static final int USB_PID_ACC_MIN = 0x2D00;      // 11520   Product ID to use when in accessory mode without ADB
    private static final int USB_PID_ACC_MAX = 0x2D05;

    public static final int USB_PID_ACC = 0x2D00;      // Accessory                  100
    public static final int USB_PID_ACC_ADB = 0x2D01;      // Accessory + ADB            110
    private static final int USB_PID_AUD = 0x2D02;      //                   Audio    001
    private static final int USB_PID_AUD_ADB = 0x2D03;      //             ADB + Audio    011
    private static final int USB_PID_ACC_AUD = 0x2D04;      // Accessory       + Audio    101
    private static final int USB_PID_ACC_AUD_ADB = 0x2D05;      // Accessory + ADB + Audio    111


    private UsbDevice mUsbDevice;


    public UsbDeviceCompat(UsbDevice device) {
        mUsbDevice = device;
    }

    private String prod_id_name_get(int prod_id) {
        if (prod_id == USB_PID_ACC)
            return ("ACC");
        else if (prod_id == USB_PID_ACC_ADB)
            return ("ADB");//ACC_ADB");
        else if (prod_id == USB_PID_AUD)
            return ("AUD");
        else if (prod_id == USB_PID_AUD_ADB)
            return ("AUA");//AUD_ADB");
        else if (prod_id == USB_PID_ACC_AUD_ADB)
            return ("ALL");//ACC_AUD_ADB");
        else
            //return ("0x" + Utils.hex_get (prod_id));
            return ("" + prod_id);
    }

    private String vend_id_name_get(int vend_id) {
        if (vend_id == USB_VID_GOO)
            return ("GOO");//GLE");
        else if (vend_id == USB_VID_HTC)
            return ("HTC");
        else if (vend_id == USB_VID_SAM)
            return ("SAM");//SUNG");
        else if (vend_id == USB_VID_SON)
            return ("SON");//Y");
        else if (vend_id == USB_VID_MOT)
            return ("MOT");//OROLA");
        else if (vend_id == USB_VID_LGE)
            return ("LGE");
        else if (vend_id == USB_VID_O1A)
            return ("O1A");
        else
            //return ("0x" + Utils.hex_get (vend_id));
            return ("" + vend_id);
    }

    private String usb_man_get(android.hardware.usb.UsbDevice device) {                       // Use reflection to avoid ASUS tablet problem
        String ret = "";
        //ret = device.getManufacturerName ();                              // mManufacturerName=HTC
        try {
            Class<?> c = Class.forName("android.hardware.usb.UsbDeviceCompat");
            java.lang.reflect.Method get = c.getMethod("getManufacturerName");
            ret = (String) get.invoke(device);
            Utils.logd("ret: " + ret);
        } catch (Throwable t) {
            Utils.loge("Throwable t: " + t);
        }
        return (ret);
    }

    private String usb_pro_get(android.hardware.usb.UsbDevice device) {
        String ret = "";
        //ret = device.getProductName      ();                              // mProductName=Android Phone
        try {
            Class<?> c = Class.forName("android.hardware.usb.UsbDeviceCompat");
            java.lang.reflect.Method get = c.getMethod("getProductName");
            ret = (String) get.invoke(device);
            Utils.logd("ret: " + ret);
        } catch (Throwable t) {
            Utils.loge("Throwable t: " + t);
        }
        return (ret);
    }

    private String usb_ser_get(android.hardware.usb.UsbDevice device) {
        String ret = "";
        //ret = device.getSerialNumber     ();                              // mSerialNumber=FA46RWM22264
        try {
            Class<?> c = Class.forName("android.hardware.usb.UsbDeviceCompat");
            java.lang.reflect.Method get = c.getMethod("getSerialNumber");
            ret = (String) get.invoke(device);
            Utils.logd("ret: " + ret);
        } catch (Throwable t) {
            Utils.loge("Throwable t: " + t);
        }
        return (ret);
    }

    private String usb_dev_name_get(android.hardware.usb.UsbDevice device) {
        String usb_dev_name = "";
        String dev_name = device.getDeviceName();                          // mName=/dev/bus/usb/003/007
        int dev_class = device.getDeviceClass();                         // mClass=255
        int dev_sclass = device.getDeviceSubclass();                      // mSublass=255
        int dev_dev_id = device.getDeviceId();                            // mId=2                        1003 for /dev/bus/usb/001/003
        int dev_proto = device.getDeviceProtocol();                      // mProtocol=0
        int dev_vend_id = device.getVendorId();                            // mVendorId=2996               HTC
        int dev_prod_id = device.getProductId();                           // mProductId=1562              OneM8
        Utils.logd("dev_name: " + dev_name + "  dev_class: " + dev_class + "  dev_sclass: " + dev_sclass + "  dev_dev_id: " + dev_dev_id + "  dev_proto: " + dev_proto + "  dev_vend_id: " + dev_vend_id + "  dev_prod_id: " + dev_prod_id);

        String dev_man = "";
        String dev_prod = "";
        String dev_ser = "";

        if (Utils.SDK_INT >= 21) {                                 // Android 5.0+ only
            try {
                dev_man = usb_man_get(device);                                // mManufacturerName=HTC
                dev_prod = usb_pro_get(device);                                // mProductName=Android Phone
                dev_ser = usb_ser_get(device);                                // mSerialNumber=FA46RWM22264
            } catch (Throwable e) {
                Utils.loge(e);
            }
            if (dev_man == null)
                dev_man = "";
            else
                dev_man = dev_man.toUpperCase(Locale.getDefault());
            if (dev_prod == null)
                dev_prod = "";
            else
                dev_prod = dev_prod.toUpperCase(Locale.getDefault());
            if (dev_ser == null)
                dev_ser = "";
            else
                dev_ser = dev_ser.toUpperCase(Locale.getDefault());

            Utils.logd("dev_man: " + dev_man + "  dev_prod: " + dev_prod + "  dev_ser: " + dev_ser);
        }

        usb_dev_name += vend_id_name_get(dev_vend_id);
        usb_dev_name += ":";
        usb_dev_name += prod_id_name_get(dev_prod_id);
        usb_dev_name += "\n";

        usb_dev_name += Utils.hex_get((short) dev_vend_id);
        usb_dev_name += ":";
        usb_dev_name += Utils.hex_get((short) dev_prod_id);
        usb_dev_name += "\n";

        usb_dev_name += "" + dev_dev_id;

        return (usb_dev_name);
    }

    public String getName() {
        return usb_dev_name_get(mUsbDevice);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s - %s", getName(), mUsbDevice.toString());
    }

    public UsbDevice getWrappedDevice() {
        return mUsbDevice;
    }
}
