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
import android.util.Log
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

    private val rrUpdates: IRRCtrlListener = object : IRRCtrlListener.Stub() {
        override fun onCmdComplete(p0: Int, p1: Int, p2: Int, p3: ByteArray?): Int {
            Log.d("Roadrover", "onCmdComplete()")
            return 0
        }

        override fun onRRCtrlParamChange(p0: Int, p1: Int, p2: Int, p3: ByteArray?): Int {
            if (p0 == 7) {
                textBox.append("[CTRL] Brake: $p2")
            } else {
                textBox.append("[CTRL] param: $p0, lvalue = $p1, wvalue= p2")
            }
            return 0
        }

    }

    private val carListener = object: IRRCtrlDeviceListener.Stub() {
        override fun onDeviceCmdComplete(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: ByteArray?): Int {
            Log.d("Roadrover", "onDeviceCmdComplete()")
            return 0
        }

        override fun onDeviceInfo(p0: Int, p1: Int, p2: ByteArray?): Int {
            Log.d("Roadrover", "onDeviceInfo($p0, $p1, $p2)")
            return 0
        }

        override fun onDeviceParamChanged(p0: Int, p1: Int, p2: ByteArray?): Int {
            Log.d("Roadrover", "onDeviceParamChanged($p0, $p1, $p2)")
            return 0
        }
    }

    private val ctrlManager: IRRCtrlManager? by lazy {
        IRRCtrlManager.Stub.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(this.textBox)

        registerReceiver(carReceived, DeviceListener.createIntentFilter())
        val hu = IntentFilter()
        hu.addAction(ProjectionActivityRequest.action)
        hu.addAction(KeyIntent.action)
        registerReceiver(huSent, hu)

        Log.d("Roadrover", "Version ${ctrlManager?.version ?: "Unknown"}")
        Log.d("Roadrover", "Brake ${ctrlManager?.getIntParam(7) ?: "Unknown"}")

        ctrlManager?.requestRRUpdates(0, rrUpdates)
        //deviceHandler = this.ctrlManager?.deviceOpen(this.mDeviceType, 0, 0, this.mCtrlDeviceListener) ?: 0
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
        ctrlManager?.unrequestRRUpdates(0, rrUpdates)
    }
}