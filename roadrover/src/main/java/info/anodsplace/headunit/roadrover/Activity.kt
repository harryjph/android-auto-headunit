package info.anodsplace.headunit.roadrover

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import info.anodsplace.headunit.contract.KeyIntent
import info.anodsplace.headunit.contract.ProjectionActivityRequest
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem


/**
 * @author algavris
 * @date 02/01/2018
 */
class Activity: Activity() {
    val textBox: TextView by lazy {
        val view = TextView(this)
        view.movementMethod = ScrollingMovementMethod();
        view
    }
    private val carReceived: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            textBox.append("[CAR]: ${intent.toString()}\n")
        }
    }

    private val huSent: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            textBox.append("[HU]: ${intent.toString()}\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(this.textBox)

        registerReceiver(carReceived, DeviceListener.createIntentFilter())
        val hu = IntentFilter()
        hu.addAction(ProjectionActivityRequest.action)
        hu.addAction(KeyIntent.action)
        registerReceiver(huSent, hu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.title == "Clear") {
            textBox.text = ""
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 0, 0, "Clear")
                .setIcon(android.R.drawable.ic_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        return true
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(carReceived)
        unregisterReceiver(huSent)
    }
}