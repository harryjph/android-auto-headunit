package ca.yyx.hu.location;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * @author algavris
 * @date 18/12/2016.
 */
public class GpsLocationService extends Service {
    private GpsLocation mGpsLocation;

    public static Intent intent(Context context) {
        return new Intent(context, GpsLocationService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mGpsLocation == null) {
            mGpsLocation = new GpsLocation(this);
        }

        mGpsLocation.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mGpsLocation.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
