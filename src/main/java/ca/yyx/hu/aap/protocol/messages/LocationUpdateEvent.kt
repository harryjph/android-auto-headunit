package ca.yyx.hu.aap.protocol.messages

import android.location.Location
import ca.yyx.hu.aap.protocol.nano.Protocol
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 15/02/2017.
 */

class LocationUpdateEvent(location: Location)
    : SensorEvent(Protocol.SENSOR_TYPE_LOCATION, LocationUpdateEvent.makeProto(location)) {

    companion object {
        private fun makeProto(location: Location): MessageNano {
            val sensorBatch = Protocol.SensorBatch()
            sensorBatch.locationData = arrayOfNulls<Protocol.SensorBatch.LocationData>(1)
            sensorBatch.locationData[0] = Protocol.SensorBatch.LocationData()
            sensorBatch.locationData[0].timestamp = location.time
            sensorBatch.locationData[0].latitude = (location.latitude * 1E7).toInt()
            sensorBatch.locationData[0].longitude = (location.longitude * 1E7).toInt()
            sensorBatch.locationData[0].altitude = (location.altitude * 1E2).toInt()
            sensorBatch.locationData[0].bearing = (location.bearing * 1E6).toInt()
            // AA expects speed in knots, so convert back
            sensorBatch.locationData[0].speed = (location.speed * 1.94384 * 1E3).toInt()
            sensorBatch.locationData[0].accuracy = (location.accuracy * 1E3).toInt()
            return sensorBatch
        }
    }
}
