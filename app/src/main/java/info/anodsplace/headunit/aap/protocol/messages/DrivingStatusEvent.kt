package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.proto.Sensors

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 * *
 * * Driving status doesn't receive sensor start request
 */

class DrivingStatusEvent(status: Sensors.SensorBatch.DrivingStatusData.Status)
    : AapMessage(Channel.ID_SEN, Sensors.SensorsMsgType.SENSOR_EVENT_VALUE, makeProto(status)) {

    companion object {
        private fun makeProto(status: Sensors.SensorBatch.DrivingStatusData.Status): MessageLite {
            return Sensors.SensorBatch.newBuilder()
                    .addDrivingStatus(Sensors.SensorBatch.DrivingStatusData.newBuilder()
                            .setStatus(status.number))
                    .build()
        }
    }
}
