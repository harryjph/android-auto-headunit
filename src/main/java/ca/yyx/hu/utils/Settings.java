package ca.yyx.hu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import ca.yyx.hu.usb.UsbDeviceCompat;

/**
 * @author algavris
 * @date 21/05/2016.
 */

public class Settings {

    private final SharedPreferences mPrefs;

    public Settings(Context context)
    {
        mPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public void allowDevices(@NonNull Set<String> allowDevices)
    {
        mPrefs.edit().putStringSet("allow-devices", allowDevices).apply();
    }

    public boolean isConnectingDevice(UsbDeviceCompat deviceCompat) {
        Set<String> allowDevices = mPrefs.getStringSet("allow-devices", null);
        if (allowDevices == null) {
            return false;
        }
        return allowDevices.contains(deviceCompat.getUniqueName());
    }

    public Set<String> getAllowedDevices() {
        return mPrefs.getStringSet("allow-devices", new HashSet<String>());
    }
}
