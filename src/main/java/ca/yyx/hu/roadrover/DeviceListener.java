package ca.yyx.hu.roadrover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;

import ca.yyx.hu.App;
import ca.yyx.hu.aap.AapTransport;
import ca.yyx.hu.aap.Messages;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 24/09/2016.
 */

public class DeviceListener extends BroadcastReceiver {
    private static final String ACTION_AUDIO = "com.roadrover.frontpane.audio";
    private static final String ACTION_KEYEVENT = "com.roadrover.frontpane.keyevent";
    private static final String ACTION_STARTMUSIC = "com.roadrover.startmusic";
    private static final String ACTION_RADIO_AREA_CHANGE = "com.roadrover.area.change";
    private static final String ACTION_RADIO_SWITCH_POINT = "com.roadrover.radio.switch.point";
    private static final String ACTION_CAR_KEY = "com.roadrover.carkey";

    public static final String AIRCON_CHANGED = "com.roadrover.car.aircon";
    public static final String CAR_AIRCON_WINDOW_HIDE = "com.roadrover.car.aircon.hide";
    public static final String CAR_FUEL_MINUTE_CHANGED = "com.roadrover.car.fuel.minute";
    public static final String CAR_FUEL_REALTIME_CHANGED = "com.roadrover.car.fuel.realtime";
    public static final String CAR_LIGHT_CHANGED = "com.roadrover.car.light";
    public static final String CAR_ODD_CHANGED = "com.roadrover.car.odd";
    public static final String CAR_OUT_TEMP_CHANGED = "com.roadrover.car.outtemp";
    public static final String CAR_WINDESCREEWIPER_CHANGED = "com.roadrover.car.windescreewiper";
    public static final String CCD_DISABLE = "com.roadrover.ccd.disable";
    public static final String CCD_ENABLE = "com.roadrover.ccd.enable";
    public static final String DOOR_CHANGED = "com.roadrover.car.door";
    public static final String FAULT_MSG_CHANGED = "com.roadrover.car.fault.code";
    public static final String RADAR_DATA_CHANGED = "com.roadrover.radar";
    public static final String CAR_ODD_DATA = "com.roadrover.odddata";
    public static final String CAR_SET_DATA = "com.roadrover.carsetdata.changed";

    public static IntentFilter createIntentFilter()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_AUDIO);
        filter.addAction(ACTION_KEYEVENT);
        filter.addAction(ACTION_STARTMUSIC);
        filter.addAction(ACTION_RADIO_AREA_CHANGE);
        filter.addAction(ACTION_RADIO_SWITCH_POINT);
        filter.addAction(ACTION_CAR_KEY);

        filter.addAction(AIRCON_CHANGED);
        filter.addAction(CAR_AIRCON_WINDOW_HIDE);
        filter.addAction(CAR_FUEL_MINUTE_CHANGED);
        filter.addAction(CAR_FUEL_REALTIME_CHANGED);
        filter.addAction(CAR_LIGHT_CHANGED);
        filter.addAction(CAR_ODD_CHANGED);
        filter.addAction(CAR_OUT_TEMP_CHANGED);
        filter.addAction(CAR_WINDESCREEWIPER_CHANGED);
        filter.addAction(CCD_DISABLE);
        filter.addAction(CCD_ENABLE);
        filter.addAction(DOOR_CHANGED);
        filter.addAction(FAULT_MSG_CHANGED);
        filter.addAction(RADAR_DATA_CHANGED);
        filter.addAction(CAR_ODD_DATA);
        filter.addAction(CAR_SET_DATA);


        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppLog.i(intent);

        AapTransport transport = App.get(context).transport();

        if (ACTION_KEYEVENT.equals(intent.getAction())) {
            int keyCode = intent.getIntExtra("keyvalue", 0);
            sendButton(keyCode, transport);
        } else if (ACTION_CAR_KEY.equals(intent.getAction())) {
            String keyCode = intent.getStringExtra("keyvalue");
            if (keyCode != null) {
                sendButton(Integer.valueOf(keyCode), transport);
            }
        } else if (ACTION_STARTMUSIC.equals(intent.getAction()))
        {
            sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, transport);
        }
    }

    private void sendButton(int btnCode, AapTransport transport)
    {
        transport.sendButton(btnCode, true);
        Utils.ms_sleep(100);
        transport.sendButton(btnCode, false);
    }
}
