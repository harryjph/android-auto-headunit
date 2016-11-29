package ca.yyx.hu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.yyx.hu.connection.UsbDeviceCompat;

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

    public Set<String> getNetworkAddresses() {
        return mPrefs.getStringSet("network-addresses", new HashSet<String>());
    }

    public void setNetworkAddresses(Set<String> addrs) {
        mPrefs.edit().putStringSet("network-addresses", addrs).apply();
    }

    public String getBluetoothAddress() {
        return mPrefs.getString("bt-address", "40:EF:4C:A3:CB:A5");
    }
}
