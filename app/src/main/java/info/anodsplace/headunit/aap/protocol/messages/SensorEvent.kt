package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.proto.Sensors

/**
 * @author algavris
 * *
 * @date 24/02/2017.
 */

open class SensorEvent(val sensorType: Int, proto: MessageLite)
    : AapMessage(Channel.ID_SEN, Sensors.SensorsMsgType.SENSOR_EVENT_VALUE, proto)
