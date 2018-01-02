package info.anodsplace.headunit.aap.protocol.messages

import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.nano.Sensors
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 24/02/2017.
 */

open class SensorEvent(val sensorType: Int, proto: MessageNano)
    : AapMessage(Channel.ID_SEN, Sensors.MSG_SENSORS_EVENT, proto)
