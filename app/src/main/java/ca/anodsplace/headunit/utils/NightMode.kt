 package ca.anodsplace.headunit.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * @author algavris
 * *
 * @date 01/12/2016.
 */

class NightMode(private val settings: Settings, val hasGPSLocation: Boolean) {
    private val calculator = NightModeCalculator(settings)

    var current: Boolean = false
        get()  {
            return when (settings.nightMode){
                Settings.NightMode.AUTO -> calculator.current
                Settings.NightMode.DAY -> false
                Settings.NightMode.NIGHT -> true
                Settings.NightMode.AUTO_WAIT_GPS -> {
                    if (hasGPSLocation) calculator.current else false
                }
                Settings.NightMode.NONE -> false
            }
        }

    override fun toString(): String {
        return when (settings.nightMode){
            Settings.NightMode.AUTO -> "NightMode: ${calculator.current}"
            Settings.NightMode.DAY -> "NightMode: DAY"
            Settings.NightMode.NIGHT -> "NightMode: NIGHT"
            Settings.NightMode.AUTO_WAIT_GPS -> {
                if (hasGPSLocation)"NightMode: ${calculator.current}" else "NightMode: DAY"
            }
            Settings.NightMode.NONE -> "NightMode: NONE"
        }
    }
}

private class NightModeCalculator(private val settings: Settings) {
    private val twilightCalculator = TwilightCalculator()
    private val format = SimpleDateFormat("HH:mm", Locale.US)

    var current: Boolean = false
        get()  {
            val time = Calendar.getInstance().time
            val location = settings.lastKnownLocation
            twilightCalculator.calculateTwilight(time.time, location.latitude, location.longitude)
            return twilightCalculator.mState == TwilightCalculator.NIGHT
        }

    override fun toString(): String {
        val sunrise = if (twilightCalculator.mSunrise > 0) format.format(Date(twilightCalculator.mSunrise)) else "-1"
        val sunset = if (twilightCalculator.mSunset > 0) format.format(Date(twilightCalculator.mSunset)) else "-1"
        val mode = if (twilightCalculator.mState == TwilightCalculator.NIGHT) "NIGHT" else "DAY"
        return String.format(Locale.US, "%s, (%s - %s)", mode, sunrise, sunset)
    }
}
