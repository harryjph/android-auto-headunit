package ca.yyx.hu.aap.protocol.messages

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.nano.Protocol
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class KeyCodeEvent(timeStamp: Long, keycode: Int, isPress: Boolean)
    : AapMessage(Channel.ID_INP, MsgType.Input.EVENT, KeyCodeEvent.makeProto(timeStamp, keycode, isPress)) {

    companion object {
        private fun makeProto(timeStamp: Long, keycode: Int, isPress: Boolean): MessageNano {
            val inputReport = Protocol.InputReport()
            val keyEvent = Protocol.KeyEvent()
            // Timestamp in nanoseconds = microseconds x 1,000,000
            inputReport.timestamp = timeStamp * 1000000L
            inputReport.keyEvent = keyEvent

            keyEvent.keys = arrayOfNulls<Protocol.Key>(1)
            keyEvent.keys[0] = Protocol.Key()
            keyEvent.keys[0].keycode = keycode
            keyEvent.keys[0].down = isPress

            return inputReport
        }
    }
}
