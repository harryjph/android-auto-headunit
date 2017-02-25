package ca.yyx.hu.location;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.LocalIntent;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 06/12/2016.
 */

public class GpsLocation implements GpsStatus.Listener, LocationListener {
    private final LocationManager mLocationManager;
    private final LocalBroadcastManager mBroadcastManager;
    private GpsStatus mStatus = null;
    private boolean mRequested;

    GpsLocation(Context context)
    {
        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.addGpsStatusListener(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public void start()
    {
        if (mRequested){
            return;
        }
        AppLog.i("Request location updates");
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        mLocationManager.requestLocationUpdates(500, 0, criteria, this, null);
        mRequested = true;
    }

    @Override
    public void onGpsStatusChanged(int event) {
        mStatus = mLocationManager.getGpsStatus(mStatus);
        AppLog.i(":" + mStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
//        AppLog.i(":" + location);
        mBroadcastManager.sendBroadcast(LocalIntent.createLocationUpdate(location));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        AppLog.i(provider + ": " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        AppLog.i(provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        AppLog.i(provider);
    }

    public void stop() {
        AppLog.i("Remove location updates");
        mRequested = false;
        mLocationManager.removeUpdates(this);
    }
}
