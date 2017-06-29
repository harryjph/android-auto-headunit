package ca.yyx.hu.roadrover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import ca.yyx.hu.App
import ca.yyx.hu.aap.AapProjectionActivity

import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.Utils

/**
 * @author algavris
 * *
 * @date 24/09/2016.
 */

class DeviceListener : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AppLog.i(intent)

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
            if (App.provide(context).transport.isAlive) {
                val aapIntent = Intent(context, AapProjectionActivity::class.java)
                aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
                aapIntent.flags = FLAG_ACTIVITY_NEW_TASK
                context.startActivity(aapIntent)
            }
        }
    }

    private fun sendButton(btnCode: Int, context: Context) {
        val manager  = LocalBroadcastManager.getInstance(context)
        val down = KeyEvent(KeyEvent.ACTION_DOWN, btnCode)
        manager.sendBroadcast(LocalIntent.createKeyEvent(down))
        Utils.ms_sleep(100)
        val up = KeyEvent(KeyEvent.ACTION_UP, btnCode)
        manager.sendBroadcast(LocalIntent.createKeyEvent(up))
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
