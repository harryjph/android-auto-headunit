package ca.yyx.hu.utils;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationManager;

/**
 * @author algavris
 * @date 30/05/2016.
 */

public class LocalIntent {

    public static final Intent ACTION_DISCONNECT = new Intent("ca.yyx.hu.ACTION_DISCONNECT");
    public static final IntentFilter FILTER_DISCONNECT = new IntentFilter("ca.yyx.hu.ACTION_DISCONNECT");
    public static final IntentFilter FILTER_LOCATION_UPDATE = new IntentFilter("ca.yyx.hu.LOCATION_UPDATE");

    public static Intent createLocationUpdate(Location location)
    {
        Intent intent = new Intent("ca.yyx.hu.LOCATION_UPDATED");
        intent.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        return intent;
    }

    public static Location extractLocation(Intent intent)
    {
        return intent.<Location>getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
    }

    public static UsbDevice extractDevice(Intent intent)
    {
        if (intent == null) {
            return null;
        }
        return intent.<UsbDevice>getParcelableExtra(UsbManager.EXTRA_DEVICE);
    }

}
