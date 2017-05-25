package ca.yyx.hu.roadrover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent

import ca.yyx.hu.App
import ca.yyx.hu.aap.AapTransport
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Utils

/**
 * @author algavris
 * *
 * @date 24/09/2016.
 */

class DeviceListener : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AppLog.i(intent)

        val transport = App.get(context).transport()

        if (ACTION_KEYEVENT == intent.action) {
            val keyCode = intent.getIntExtra("keyvalue", 0)
            sendButton(keyCode, transport)
        } else if (ACTION_CAR_KEY == intent.action) {
            val keyCode = intent.getStringExtra("keyvalue")
            if (keyCode != null) {
                sendButton(Integer.valueOf(keyCode), transport)
            }
        } else if (ACTION_STARTMUSIC == intent.action) {
            sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, transport)
        }
    }

    private fun sendButton(btnCode: Int, transport: AapTransport) {
        transport.sendButton(btnCode, true)
        Utils.ms_sleep(100)
        transport.sendButton(btnCode, false)
    }

    companion object {
        private val ACTION_AUDIO = "com.roadrover.frontpane.audio"
        private val ACTION_KEYEVENT = "com.roadrover.frontpane.keyevent"
        private val ACTION_STARTMUSIC = "com.roadrover.startmusic"
        private val ACTION_RADIO_AREA_CHANGE = "com.roadrover.area.change"
        private val ACTION_RADIO_SWITCH_POINT = "com.roadrover.radio.switch.point"
        private val ACTION_CAR_KEY = "com.roadrover.carkey"

        val AIRCON_CHANGED = "com.roadrover.car.aircon"
        val CAR_AIRCON_WINDOW_HIDE = "com.roadrover.car.aircon.hide"
        val CAR_FUEL_MINUTE_CHANGED = "com.roadrover.car.fuel.minute"
        val CAR_FUEL_REALTIME_CHANGED = "com.roadrover.car.fuel.realtime"
        val CAR_LIGHT_CHANGED = "com.roadrover.car.light"
        val CAR_ODD_CHANGED = "com.roadrover.car.odd"
        val CAR_OUT_TEMP_CHANGED = "com.roadrover.car.outtemp"
        val CAR_WINDESCREEWIPER_CHANGED = "com.roadrover.car.windescreewiper"
        val CCD_DISABLE = "com.roadrover.ccd.disable"
        val CCD_ENABLE = "com.roadrover.ccd.enable"
        val DOOR_CHANGED = "com.roadrover.car.door"
        val FAULT_MSG_CHANGED = "com.roadrover.car.fault.code"
        val RADAR_DATA_CHANGED = "com.roadrover.radar"
        val CAR_ODD_DATA = "com.roadrover.odddata"
        val CAR_SET_DATA = "com.roadrover.carsetdata.changed"

        fun createIntentFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(ACTION_AUDIO)
            filter.addAction(ACTION_KEYEVENT)
            filter.addAction(ACTION_STARTMUSIC)
            filter.addAction(ACTION_RADIO_AREA_CHANGE)
            filter.addAction(ACTION_RADIO_SWITCH_POINT)
            filter.addAction(ACTION_CAR_KEY)

            filter.addAction(AIRCON_CHANGED)
            filter.addAction(CAR_AIRCON_WINDOW_HIDE)
            filter.addAction(CAR_FUEL_MINUTE_CHANGED)
            filter.addAction(CAR_FUEL_REALTIME_CHANGED)
            filter.addAction(CAR_LIGHT_CHANGED)
            filter.addAction(CAR_ODD_CHANGED)
            filter.addAction(CAR_OUT_TEMP_CHANGED)
            filter.addAction(CAR_WINDESCREEWIPER_CHANGED)
            filter.addAction(CCD_DISABLE)
            filter.addAction(CCD_ENABLE)
            filter.addAction(DOOR_CHANGED)
            filter.addAction(FAULT_MSG_CHANGED)
            filter.addAction(RADAR_DATA_CHANGED)
            filter.addAction(CAR_ODD_DATA)
            filter.addAction(CAR_SET_DATA)
            return filter
        }
    }
}
