package info.anodsplace.headunit.aap.protocol.messages

import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.nano.Sensors
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 * *
 * * Driving status doesn't receive sensor start request
 */

class DrivingStatusEvent(status: Int)
    : AapMessage(Channel.ID_SEN, Sensors.MSG_SENSORS_EVENT, DrivingStatusEvent.makeProto(status)) {

    companion object {
        private fun makeProto(status: Int): MessageNano {
            val sensorBatch = Sensors.SensorBatch()
            sensorBatch.drivingStatus = arrayOfNulls<Sensors.SensorBatch.DrivingStatusData>(1)
            sensorBatch.drivingStatus[0] = Sensors.SensorBatch.DrivingStatusData()
            sensorBatch.drivingStatus[0].status = status
            return sensorBatch
        }
    }
}
