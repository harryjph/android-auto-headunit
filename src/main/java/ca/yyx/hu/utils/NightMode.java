package ca.yyx.hu.utils;

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author algavris
 * @date 01/12/2016.
 */

public class NightMode {
    private final TwilightCalculator twilightCalculator = new TwilightCalculator();
    private final SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
    private final Settings mSettings;

    public NightMode(Settings settings) {
        mSettings = settings;
    }

    public boolean current()
    {
        Date time = Calendar.getInstance().getTime();
        Location location = mSettings.getLastKnownLocation();
        twilightCalculator.calculateTwilight(time.getTime(), location.getLatitude(), location.getLongitude());
        return twilightCalculator.mState == TwilightCalculator.NIGHT;
    }

    @Override
    public String toString() {
        String sunrise = twilightCalculator.mSunrise > 0 ? format.format(new Date(twilightCalculator.mSunrise)) : "-1";
        String sunset = twilightCalculator.mSunset > 0 ? format.format(new Date(twilightCalculator.mSunset)) : "-1";
        String mode =  twilightCalculator.mState == TwilightCalculator.NIGHT ? "NIGHT" : "DAY";
        return String.format(Locale.US, "NightMode: %s, (%s - %s)", mode, sunrise, sunset);
    }
}
