package info.anodsplace.roadrover

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author algavris
 * @date 04/01/2018
 */
class AppService: Service() {

    private val deviceListener by lazy { DeviceListener() }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerReceiver(deviceListener, DeviceListener.createIntentFilter())

        val notificationIntent = Intent(this, Activity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        // Set the info for the views that show in the notification panel.
        val notification = Notification.Builder(this)
                .setTicker("Roadrover Headunit")
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("Roadrover Headunit")
                .setContentText("Forwards events to headunit")
                .setOngoing(true)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build()

        startForeground(75, notification)

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(deviceListener)
    }

}

