package ca.yyx.hu.connection

import android.hardware.usb.UsbDevice

import java.util.Locale

import ca.yyx.hu.App
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Utils

/**
 * @author algavris
 * *
 * @date 12/05/2016.
 */
class UsbDeviceCompat(val wrappedDevice: UsbDevice) {

    val deviceName: String
        get() = wrappedDevice.deviceName

    val uniqueName: String
        get() = getUniqueName(wrappedDevice)

    override fun toString(): String {
        return String.format(Locale.US, "%s - %s", uniqueName, wrappedDevice.toString())
    }

    val isInAccessoryMode: Boolean
        get() = isInAccessoryMode(wrappedDevice)

    companion object {
        private val USB_VID_GOO = 0x18D1   // 6353   Nexus or ACC mode, see PID to distinguish
        private val USB_VID_HTC = 0x0bb4   // 2996
        private val USB_VID_SAM = 0x04e8   // 1256
        private val USB_VID_O1A = 0xfff6   // 65526    Samsung ?
        private val USB_VID_SON = 0x0fce   // 4046
        private val USB_VID_LGE = 0xfff5   // 65525
        private val USB_VID_MOT = 0x22b8   // 8888
        private val USB_VID_ACE = 0x0502
        private val USB_VID_HUA = 0x12d1
        private val USB_VID_PAN = 0x10a9
        private val USB_VID_ZTE = 0x19d2
        private val USB_VID_GAR = 0x091e
        private val USB_VID_XIA = 0x2717
        private val USB_VID_ASU = 0x0b05
        private val USB_VID_MEI = 0x2a45
        private val USB_VID_LEN = 0x17ef
        private val USB_VID_LGD = 0x1004
        private val USB_VID_LIN = 0x1d6b
        private val USB_VID_QUA = 0x05c6
        private val USB_VID_ONE = 0x2a70
        private val USB_VID_COM = 0x1519  // Comneon

        private val USB_VID_ASE = 0x0835  // Action Star Enterprise
        private val USB_VID_OPO = 0x22d9  // Oppo


        private val USB_PID_ACC_MIN = 0x2D00      // 11520   Product ID to use when in accessory mode without ADB
        private val USB_PID_ACC_MAX = 0x2D05

        private val USB_PID_ACC = 0x2D00      // Accessory                  100
        private val USB_PID_ACC_ADB = 0x2D01      // Accessory + ADB            110
        private val USB_PID_AUD = 0x2D02      //                   Audio    001
        private val USB_PID_AUD_ADB = 0x2D03      //             ADB + Audio    011
        private val USB_PID_ACC_AUD = 0x2D04      // Accessory       + Audio    101
        private val USB_PID_ACC_AUD_ADB = 0x2D05      // Accessory + ADB + Audio    111

        private fun prod_id_name_get(prod_id: Int): String {
            if (prod_id == USB_PID_ACC)
                return "ACC"
            else if (prod_id == USB_PID_ACC_ADB)
                return "ADB"//ACC_ADB");
            else if (prod_id == USB_PID_AUD)
                return "AUD"
            else if (prod_id == USB_PID_AUD_ADB)
                return "AUA"//AUD_ADB");
            else if (prod_id == USB_PID_ACC_AUD_ADB)
                return "ALL"//ACC_AUD_ADB");
            else
            //return ("0x" + AppLog.hex_get (prod_id));
                return "" + prod_id
        }

        private fun vend_id_name_get(vend_id: Int): String {
            if (vend_id == USB_VID_GOO)
                return "GOO"//GLE");
            else if (vend_id == USB_VID_HTC)
                return "HTC"
            else if (vend_id == USB_VID_SAM)
                return "SAM"//SUNG");
            else if (vend_id == USB_VID_SON)
                return "SON"//Y");
            else if (vend_id == USB_VID_MOT)
                return "MOT"//OROLA");
            else if (vend_id == USB_VID_LGE)
                return "LGE"
            else if (vend_id == USB_VID_O1A)
                return "O1A"
            else if (vend_id == USB_VID_HUA)
                return "HUA"

            return "" + vend_id
        }

        private fun usb_man_get(device: UsbDevice): String {                       // Use reflection to avoid ASUS tablet problem
            var ret = ""
            //ret = device.getManufacturerName ();                              // mManufacturerName=HTC
            try {
                val c = Class.forName("android.hardware.usb.UsbDeviceCompat")
                val get = c.getMethod("getManufacturerName")
                ret = get.invoke(device) as String
                AppLog.i("ret: " + ret)
            } catch (t: Throwable) {
                AppLog.e("Throwable t: " + t)
            }

            return ret
        }

        private fun usb_pro_get(device: UsbDevice): String {
            var ret = ""
            //ret = device.getProductName      ();                              // mProductName=Android Phone
            try {
                val c = Class.forName("android.hardware.usb.UsbDeviceCompat")
                val get = c.getMethod("getProductName")
                ret = get.invoke(device) as String
                AppLog.i("ret: " + ret)
            } catch (t: Throwable) {
                AppLog.e("Throwable t: " + t)
            }

            return ret
        }

        private fun usb_ser_get(device: UsbDevice): String {
            var ret = ""
            //ret = device.getSerialNumber     ();                              // mSerialNumber=FA46RWM22264
            try {
                val c = Class.forName("android.hardware.usb.UsbDeviceCompat")
                val get = c.getMethod("getSerialNumber")
                ret = get.invoke(device) as String
                AppLog.i("ret: " + ret)
            } catch (t: Throwable) {
                AppLog.e("Throwable t: " + t)
            }

            return ret
        }

        fun getUniqueName(device: UsbDevice): String {
            var usb_dev_name = ""
            val dev_name = device.deviceName                          // mName=/dev/bus/usb/003/007
            val dev_vend_id = device.vendorId                            // mVendorId=2996               HTC
            val dev_prod_id = device.productId                           // mProductId=1562              OneM8

//            if (App.IS_LOLLIPOP) {                                 // Android 5.0+ only
//                try {
//                    dev_man = usb_man_get(device).toUpperCase(Locale.getDefault())                             // mManufacturerName=HTC
//                    dev_prod = usb_pro_get(device).toUpperCase(Locale.getDefault())                                // mProductName=Android Phone
//                    dev_ser = usb_ser_get(device).toUpperCase(Locale.getDefault())                              // mSerialNumber=FA46RWM22264
//                } catch (e: Throwable) {
//                    AppLog.e(e)
//                }
//            }

            usb_dev_name += vend_id_name_get(dev_vend_id)
            usb_dev_name += ":"
            usb_dev_name += prod_id_name_get(dev_prod_id)
            usb_dev_name += ":"

            usb_dev_name += Utils.hex_get(dev_vend_id.toShort())
            usb_dev_name += ":"
            usb_dev_name += Utils.hex_get(dev_prod_id.toShort())

            return usb_dev_name
        }

        fun isInAccessoryMode(device: UsbDevice): Boolean {
            val dev_vend_id = device.vendorId                            // mVendorId=2996               HTC
            val dev_prod_id = device.productId                           // mProductId=1562              OneM8

            return dev_vend_id == UsbDeviceCompat.USB_VID_GOO && (dev_prod_id == UsbDeviceCompat.USB_PID_ACC || dev_prod_id == UsbDeviceCompat.USB_PID_ACC_ADB)
        }
    }
}
