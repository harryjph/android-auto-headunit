package info.anodsplace.roadrover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log

/**
 * @author algavris
 * @date 02/01/2018
 */

class ConnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RoadroverHU", "Connected receiver")
        val h = Handler()
        h.postDelayed({
            context?.startService(Intent(context, AppService::class.java))
        }, 1000)
    }
}
