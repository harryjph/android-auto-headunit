package ca.yyx.hu.utils;

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
    private Date time;
    private final SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);

    public boolean current()
    {
        time = Calendar.getInstance().getTime();
        twilightCalculator.calculateTwilight(time.getTime(), 32.0864169,34.7557871);
        return twilightCalculator.mState == TwilightCalculator.NIGHT;
    }

    @Override
    public String toString() {
        if (time == null) {
            return "NightMode: Not Initialized";
        }

        String sunrise = twilightCalculator.mSunrise > 0 ? format.format(new Date(twilightCalculator.mSunrise)) : "-1";
        String sunset = twilightCalculator.mSunset > 0 ? format.format(new Date(twilightCalculator.mSunset)) : "-1";
        String mode =  twilightCalculator.mState == TwilightCalculator.NIGHT ? "NIGHT" : "DAY";
        return String.format(Locale.US, "NightMode: %s, (%s - %s)", mode, sunrise, sunset);
    }
}
