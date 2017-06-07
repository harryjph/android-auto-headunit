package ca.yyx.hu.aap.protocol.messages

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 24/02/2017.
 */

open class SensorEvent(val sensorType: Int, proto: MessageNano)
    : AapMessage(Channel.ID_SEN, MsgType.Sensor.EVENT, proto)
