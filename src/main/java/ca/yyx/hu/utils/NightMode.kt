package ca.yyx.hu.utils

import android.location.Location

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * @author algavris
 * *
 * @date 01/12/2016.
 */

class NightMode(private val mSettings: Settings) {
    private val twilightCalculator = TwilightCalculator()
    private val format = SimpleDateFormat("HH:mm", Locale.US)

    fun current(): Boolean {
        val time = Calendar.getInstance().getTime()
        val location = mSettings.lastKnownLocation
        twilightCalculator.calculateTwilight(time.getTime(), location.getLatitude(), location.getLongitude())
        return twilightCalculator.mState == TwilightCalculator.NIGHT
    }

    override fun toString(): String {
        val sunrise = if (twilightCalculator.mSunrise > 0) format.format(Date(twilightCalculator.mSunrise)) else "-1"
        val sunset = if (twilightCalculator.mSunset > 0) format.format(Date(twilightCalculator.mSunset)) else "-1"
        val mode = if (twilightCalculator.mState == TwilightCalculator.NIGHT) "NIGHT" else "DAY"
        return String.format(Locale.US, "NightMode: %s, (%s - %s)", mode, sunrise, sunset)
    }
}
