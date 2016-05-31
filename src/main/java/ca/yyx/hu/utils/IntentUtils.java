package ca.yyx.hu.utils;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * @author algavris
 * @date 30/05/2016.
 */

public class IntentUtils {

    public static final Intent ACTION_DISCONNECT = new Intent("ca.yyx.hu.ACTION_DISCONNECT");
    public static final IntentFilter DISCONNECT_FILTER = new IntentFilter("ca.yyx.hu.ACTION_DISCONNECT");

    public static UsbDevice getDevice(Intent intent)
    {
        if (intent == null) {
            return null;
        }
        return intent.<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE);
    }

}
