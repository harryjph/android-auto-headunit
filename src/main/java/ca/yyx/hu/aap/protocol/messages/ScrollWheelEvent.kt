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

class ScrollWheelEvent(timeStamp: Long, delta: Int)
    : AapMessage(Channel.ID_INP, MsgType.Input.EVENT, ScrollWheelEvent.makeProto(timeStamp, delta)) {
    companion object {
        val KEYCODE_SCROLL_WHEEL = 65536

        private fun makeProto(timeStamp: Long, delta: Int): MessageNano {
            val inputReport = Protocol.InputReport()
            val keyEvent = Protocol.KeyEvent()
            // Timestamp in nanoseconds = microseconds x 1,000,000
            inputReport.timestamp = timeStamp * 1000000L
            inputReport.keyEvent = keyEvent

            val relativeEvent = Protocol.RelativeEvent()
            relativeEvent.data = arrayOfNulls<Protocol.RelativeEvent_Rel>(1)
            relativeEvent.data[0] = Protocol.RelativeEvent_Rel()
            relativeEvent.data[0].delta = delta
            relativeEvent.data[0].keycode = KEYCODE_SCROLL_WHEEL
            inputReport.relativeEvent = relativeEvent

            return inputReport
        }
    }

}
