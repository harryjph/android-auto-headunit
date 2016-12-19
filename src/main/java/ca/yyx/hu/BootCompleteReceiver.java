package ca.yyx.hu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import ca.yyx.hu.location.GpsLocationService;

/**
 * @author algavris
 * @date 18/12/2016.
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                App.get(context).startService(GpsLocationService.intent(context));
            }
        }, 10000);
    }
}
