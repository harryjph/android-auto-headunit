package info.anodsplace.headunit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.view.KeyEvent
import info.anodsplace.headunit.aap.AapProjectionActivity
import info.anodsplace.headunit.aap.protocol.messages.LocationUpdateEvent
import info.anodsplace.headunit.contract.KeyIntent
import info.anodsplace.headunit.contract.LocationUpdateIntent
import info.anodsplace.headunit.contract.MediaKeyIntent
import info.anodsplace.headunit.contract.ProjectionActivityRequest

/**
 * @author algavris
 * @date 22/12/2017
 */
class AapBroadcastReceiver : BroadcastReceiver() {

    companion object {
        val filter: IntentFilter by lazy {
            val filter = IntentFilter()
            filter.addAction(LocationUpdateIntent.action)
            filter.addAction(MediaKeyIntent.action)
            filter.addAction(ProjectionActivityRequest.action)
            filter
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val component = App.provide(context)
        if (intent.action == LocationUpdateIntent.action) {
            val location = LocationUpdateIntent.extractLocation(intent)
            if (component.settings.useGpsForNavigation) {
                App.provide(context).transport.send(LocationUpdateEvent(location))
            }

            if (location.latitude != 0.0 && location.longitude != 0.0) {
                component.settings.lastKnownLocation = location
            }
        } else if (intent.action == MediaKeyIntent.action) {
            val event = intent.getParcelableExtra<KeyEvent>(KeyIntent.extraEvent)
            component.transport.send(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        } else if (intent.action == ProjectionActivityRequest.action){
            if (component.transport.isAlive) {
                val aapIntent = Intent(context, AapProjectionActivity::class.java)
                aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
                aapIntent.flags = FLAG_ACTIVITY_NEW_TASK
                context.startActivity(aapIntent)
            }
        }
    }
}

