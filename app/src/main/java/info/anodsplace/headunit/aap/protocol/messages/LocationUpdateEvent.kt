package info.anodsplace.headunit.aap.protocol.messages

import android.location.Location
import com.google.protobuf.Message
import info.anodsplace.headunit.aap.protocol.proto.Sensors

/**
 * @author algavris
 * *
 * @date 15/02/2017.
 */

class LocationUpdateEvent(location: Location)
    : SensorEvent(Sensors.SensorType.LOCATION_VALUE, makeProto(location)) {

    companion object {
        private fun makeProto(location: Location): Message {
            return Sensors.SensorBatch.newBuilder().also {
                it.addLocationData(
                        Sensors.SensorBatch.LocationData.newBuilder().apply {
                            timestamp = location.time
                            latitude = (location.latitude * 1E7).toInt()
                            longitude = (location.longitude * 1E7).toInt()
                            altitude = (location.altitude * 1E2).toInt()
                            bearing = (location.bearing * 1E6).toInt()
                            // AA expects speed in knots, so convert back
                            speed = (location.speed * 1.94384 * 1E3).toInt()
                            accuracy = (location.accuracy * 1E3).toInt()
                        }
                )
            }.build()
        }
    }
}
