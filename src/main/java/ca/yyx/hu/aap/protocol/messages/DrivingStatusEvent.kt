package ca.yyx.hu.aap.protocol.messages

import com.google.protobuf.nano.MessageNano

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.nano.Protocol

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 * *
 * * Driving status doesn't receive sensor start request
 */

class DrivingStatusEvent(status: Int)
    : AapMessage(Channel.ID_SEN, MsgType.Sensor.EVENT, DrivingStatusEvent.makeProto(status)) {

    companion object {
        private fun makeProto(status: Int): MessageNano {
            val sensorBatch = Protocol.SensorBatch()
            sensorBatch.drivingStatus = arrayOfNulls<Protocol.SensorBatch.DrivingStatusData>(1)
            sensorBatch.drivingStatus[0] = Protocol.SensorBatch.DrivingStatusData()
            sensorBatch.drivingStatus[0].status = status
            return sensorBatch
        }
    }
}
