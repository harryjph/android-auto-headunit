package ca.yyx.hu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
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

    public Location getLastKnownLocation() {
        long latitude = mPrefs.getLong("last-loc-latitude", Double.doubleToLongBits(32.0864169));
        long longitude = mPrefs.getLong("last-loc-longitude", Double.doubleToLongBits(34.7557871));

        Location location = new Location("");
        location.setLatitude(Double.longBitsToDouble(latitude));
        location.setLongitude(Double.longBitsToDouble(longitude));
        return location;
    }

    public void setLastKnownLocation(Location location) {
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putLong("last-loc-latitude", Double.doubleToLongBits(location.getLatitude()));
        edit.putLong("last-loc-longitude", Double.doubleToLongBits(location.getLongitude()));
        edit.apply();
    }

}
