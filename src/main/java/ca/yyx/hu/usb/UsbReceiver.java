package ca.yyx.hu.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import ca.yyx.hu.utils.Utils;

public class UsbReceiver extends BroadcastReceiver {
    public static String ACTION_USB_DEVICE_PERMISSION = "ca.yyx.hu.ACTION_USB_DEVICE_PERMISSION";
    public static final String EXTRA_CONNECT = "EXTRA_CONNECT";


    public interface Listener {
        void onUsbDetach(UsbDevice device);
        void onUsbAttach(UsbDevice device);
        void onUsbPermission(boolean granted, boolean connect, UsbDevice device);
    }

    private Listener mListener;

    public static IntentFilter createFilter()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_DEVICE_PERMISSION);
        return filter;
    }

    public static boolean match(String action)
    {
        if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            return true;
        } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            return true;
        } else if (action.equals(ACTION_USB_DEVICE_PERMISSION)) {
            return true;
        }
        return false;
    }

    public UsbReceiver(Listener mListener) {
        this.mListener = mListener;
    }          // USB Broadcast Receiver enabled by start() & disabled by stop()

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE);
        Utils.logd("USB Intent: " + intent);

        if (device != null) {
            String action = intent.getAction();

            if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {    // If detach...
                mListener.onUsbDetach(device);                                  // Handle detached device
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {// If attach...
                mListener.onUsbAttach(device);
            } else if (action.equals(ACTION_USB_DEVICE_PERMISSION)) {                 // If Our App specific Intent for permission request...
                boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                boolean connect = intent.getBooleanExtra(EXTRA_CONNECT, false);
                mListener.onUsbPermission(permissionGranted, connect, device);
            }

        }
    }
}