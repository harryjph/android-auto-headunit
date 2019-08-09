package info.anodsplace.headunit.aap.protocol.messages

import info.anodsplace.headunit.aap.protocol.nano.Sensors
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class NightModeEvent(enabled: Boolean)
    : SensorEvent(Sensors.SENSOR_TYPE_NIGHT, makeProto(enabled)) {

    companion object {
        private fun makeProto(enabled: Boolean): MessageNano {
            val sensorBatch = Sensors.SensorBatch()
            sensorBatch.nightMode = arrayOfNulls<Sensors.SensorBatch.NightModeData>(1)
            sensorBatch.nightMode[0] = Sensors.SensorBatch.NightModeData()
            sensorBatch.nightMode[0].isNight = enabled
            return sensorBatch
        }
    }
}
