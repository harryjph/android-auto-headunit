package info.anodsplace.roadrover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.KeyEvent
import info.anodsplace.headunit.contract.KeyIntent
import info.anodsplace.headunit.contract.ProjectionActivityRequest

/**
 * @author algavris
 * *
 * @date 24/09/2016.
 */

class DeviceListener : BroadcastReceiver() {

    private val tag = "HEADUNIT"

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(tag, intent.toString())

        if (ACTION_KEYEVENT == intent.action) {
            val keyCode = intent.getIntExtra("keyvalue", 0)
            sendButton(keyCode, context)
        } else if (ACTION_CAR_KEY == intent.action) {
            val keyCode = intent.getStringExtra("keyvalue")
            if (keyCode != null) {
                sendButton(Integer.valueOf(keyCode), context)
            }
        } else if (ACTION_STARTMUSIC == intent.action) {
            sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, context)
        } else if (CALL_INCOME == intent.action || CALL_PHONE == intent.action) {
            context.sendBroadcast(ProjectionActivityRequest())
        }
    }

    private fun sendButton(btnCode: Int, context: Context) {
        val down = KeyEvent(KeyEvent.ACTION_DOWN, btnCode)
        context.sendBroadcast(KeyIntent(down))
        sleep(100)
        val up = KeyEvent(KeyEvent.ACTION_UP, btnCode)
        context.sendBroadcast(KeyIntent(up))
    }

    private fun sleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            Log.e(tag, "sleep", e)
        }
    }

    companion object {
        private const val ACTION_AUDIO = "com.roadrover.frontpane.audio"
        private const val ACTION_KEYEVENT = "com.roadrover.frontpane.keyevent"
        private const val ACTION_STARTMUSIC = "com.roadrover.startmusic"
        private const val ACTION_RADIO_AREA_CHANGE = "com.roadrover.area.change"
        private const val ACTION_RADIO_SWITCH_POINT = "com.roadrover.radio.switch.point"
        private const val ACTION_CAR_KEY = "com.roadrover.carkey"

        const val AIRCON_CHANGED = "com.roadrover.car.aircon"
        const val CAR_AIRCON_WINDOW_HIDE = "com.roadrover.car.aircon.hide"
        const val CAR_FUEL_MINUTE_CHANGED = "com.roadrover.car.fuel.minute"
        const val CAR_FUEL_REALTIME_CHANGED = "com.roadrover.car.fuel.realtime"
        const val CAR_LIGHT_CHANGED = "com.roadrover.car.light"
        const val CAR_ODD_CHANGED = "com.roadrover.car.odd"
        const val CAR_OUT_TEMP_CHANGED = "com.roadrover.car.outtemp"
        const val CAR_WINDESCREEWIPER_CHANGED = "com.roadrover.car.windescreewiper"
        const val CCD_DISABLE = "com.roadrover.ccd.disable"
        const val CCD_ENABLE = "com.roadrover.ccd.enable"
        const val DOOR_CHANGED = "com.roadrover.car.door"
        const val FAULT_MSG_CHANGED = "com.roadrover.car.fault.code"
        const val RADAR_DATA_CHANGED = "com.roadrover.radar"
        const val CAR_ODD_DATA = "com.roadrover.odddata"
        const val CAR_SET_DATA = "com.roadrover.carsetdata.changed"

        const val BLUETOOTH_CALL = "com.iflytek.testbluetoothcall"
        const val CALL_INCOME = "com.roadrover.incomecall"
        const val CALL_WAIT = "com.roadrover.waitcall"
        const val CALL_PHONE = "com.roadrover.phone"
        const val CALL_DEVICE_LINK = "com.roadrover.devicelinkupdate"
        const val CALL_AUDIO = "com.roadrover.AUDIO"
        const val CALL_PHONE_NUMBER = "CZY_PHONE_NUMBER"

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

            filter.addAction(BLUETOOTH_CALL)
            filter.addAction(CALL_INCOME)
            filter.addAction(CALL_WAIT)
            filter.addAction(CALL_PHONE)
            filter.addAction(CALL_DEVICE_LINK)
            filter.addAction(CALL_AUDIO)
            filter.addAction(CALL_PHONE_NUMBER)
            return filter
        }
    }
}
