package info.anodsplace.headunit.connection

import android.hardware.usb.UsbDevice

import java.util.Locale

import info.anodsplace.headunit.aap.toHexString

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

    override fun toString() = String.format(Locale.US, "%s - %s", uniqueName, wrappedDevice.toString())

    val isInAccessoryMode: Boolean
        get() = isInAccessoryMode(wrappedDevice)

    companion object {
        private const val USB_VID_GOO: Short = 0x18D1   // 6353   Nexus or ACC mode, see PID to distinguish
        private const val USB_VID_HTC: Short = 0x0bb4   // 2996
        private const val USB_VID_SAM: Short = 0x04e8   // 1256
        private const val USB_VID_O1A: Short = 0xfff6.toShort()   // 65526    Samsung ?
        private const val USB_VID_SON: Short = 0x0fce   // 4046
        private const val USB_VID_LGE: Short = 0x1004   // 65525
        private const val USB_VID_MOT: Short = 0x22b8   // 8888
        private const val USB_VID_ACE: Short = 0x0502
        private const val USB_VID_HUA: Short = 0x12d1
        private const val USB_VID_ZTE: Short = 0x19d2
        private const val USB_VID_XIA: Short = 0x2717
        private const val USB_VID_ASU: Short = 0x0b05
        private const val USB_VID_MEI: Short = 0x2a45
        private const val USB_VID_WIL: Short = 0x4ee7

        private const val USB_PID_ACC: Short = 0x2D00      // Accessory                  100
        private const val USB_PID_ACC_ADB: Short = 0x2D01      // Accessory + ADB            110

        private val VENDOR_NAMES = mapOf(
            USB_VID_GOO to "Google",
            USB_VID_HTC to "HTC",
            USB_VID_SAM to "Samsung",
            USB_VID_SON to "Sony",
            USB_VID_MOT to "Motorola",
            USB_VID_LGE to "LG",
            USB_VID_O1A to "O1A",
            USB_VID_HUA to "Huawei",
            USB_VID_ACE to "Acer",
            USB_VID_ZTE to "ZTE",
            USB_VID_XIA to "Xiaomi",
            USB_VID_ASU to "Asus",
            USB_VID_MEI to "Meizu",
            USB_VID_WIL to "Wileyfox"
        )

        fun getUniqueName(device: UsbDevice): String {
            val vendorId = device.vendorId.toShort()
            val productId = device.productId.toShort()
            return "${VENDOR_NAMES[vendorId] ?: "$vendorId"} ${vendorId.toHexString()}:${productId.toHexString()}"
        }

        fun isInAccessoryMode(device: UsbDevice): Boolean {
            val productId = device.productId.toShort()
            return device.vendorId.toShort() == USB_VID_GOO && (productId == USB_PID_ACC || productId == USB_PID_ACC_ADB)
        }
    }
}
