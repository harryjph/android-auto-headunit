package info.anodsplace.headunit.utils

import android.text.format.DateUtils

class TwilightCalculator {
    /**
     * Time of sunset (civil twilight) in milliseconds or -1 in the case the day
     * or night never ends.
     */
    var mSunset: Long = 0
    /**
     * Time of sunrise (civil twilight) in milliseconds or -1 in the case the
     * day or night never ends.
     */
    var mSunrise: Long = 0
    /** Current state  */
    var mState: State = State.DAY

    /**
     * calculates the civil twilight bases on time and geo-coordinates.
     *
     * @param time time in milliseconds.
     * @param latitude latitude in degrees.
     * @param longitude latitude in degrees.
     */
    fun calculateTwilight(time: Long, latitude: Double, longitude: Double) {
        val daysSince2000 = (time - UTC_2000).toFloat() / DateUtils.DAY_IN_MILLIS
        // mean anomaly
        val meanAnomaly = 6.240059968f + daysSince2000 * 0.01720197f
        // true anomaly
        val trueAnomaly = meanAnomaly.toDouble() + C1 * Math.sin(meanAnomaly.toDouble()) + C2 * Math.sin((2 * meanAnomaly).toDouble()) + C3 * Math.sin((3 * meanAnomaly).toDouble())
        // ecliptic longitude
        val solarLng = trueAnomaly + 1.796593063 + Math.PI
        // solar transit in days since 2000
        val arcLongitude = -longitude / 360
        val n = Math.round(daysSince2000.toDouble() - J0.toDouble() - arcLongitude).toFloat()
        val solarTransitJ2000 = (n.toDouble() + J0.toDouble() + arcLongitude + 0.0053 * Math.sin(meanAnomaly.toDouble())
                + -0.0069 * Math.sin(2 * solarLng))
        // declination of sun
        val solarDec = Math.asin(Math.sin(solarLng) * Math.sin(OBLIQUITY.toDouble()))
        val latRad = latitude * DEGREES_TO_RADIANS
        val cosHourAngle = (Math.sin(ALTIDUTE_CORRECTION_CIVIL_TWILIGHT.toDouble()) - Math.sin(latRad) * Math.sin(solarDec)) / (Math.cos(latRad) * Math.cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            mState = State.NIGHT
            mSunset = -1
            mSunrise = -1
            return
        } else if (cosHourAngle <= -1) {
            mState = State.DAY
            mSunset = -1
            mSunrise = -1
            return
        }
        val hourAngle = (Math.acos(cosHourAngle) / (2 * Math.PI)).toFloat()
        mSunset = Math.round((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000
        mSunrise = Math.round((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000
        mState = if (time in (mSunrise + 1) until mSunset) {
            State.DAY
        } else {
            State.NIGHT
        }
    }

    enum class State {
        DAY,
        NIGHT,
    }

    companion object {
        private const val DEGREES_TO_RADIANS: Double = (Math.PI / 180.0f)
        // element for calculating solar transit.
        private const val J0 = 0.0009f
        // correction for civil twilight
        private const val ALTIDUTE_CORRECTION_CIVIL_TWILIGHT = -0.104719755f
        // coefficients for calculating Equation of Center.
        private const val C1 = 0.0334196f
        private const val C2 = 0.000349066f
        private const val C3 = 0.000005236f
        private const val OBLIQUITY = 0.40927971f
        // Java time on Jan 1, 2000 12:00 UTC.
        private const val UTC_2000 = 946728000000L
    }
}