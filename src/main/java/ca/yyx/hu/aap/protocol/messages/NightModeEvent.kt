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
 */

class NightModeEvent(enabled: Boolean)
    : SensorEvent(Protocol.SENSOR_TYPE_NIGHT, NightModeEvent.makeProto(enabled)) {

    companion object {
        private fun makeProto(enabled: Boolean): MessageNano {
            val sensorBatch = Protocol.SensorBatch()
            sensorBatch.nightMode = arrayOfNulls<Protocol.SensorBatch.NightModeData>(1)
            sensorBatch.nightMode[0] = Protocol.SensorBatch.NightModeData()
            sensorBatch.nightMode[0].isNight = enabled
            return sensorBatch
        }
    }
}
