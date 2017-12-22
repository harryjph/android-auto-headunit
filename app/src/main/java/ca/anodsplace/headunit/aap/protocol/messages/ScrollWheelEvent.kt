package ca.anodsplace.headunit.aap.protocol.messages

import ca.anodsplace.headunit.aap.AapMessage
import ca.anodsplace.headunit.aap.protocol.Channel
import ca.anodsplace.headunit.aap.protocol.nano.Input
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class ScrollWheelEvent(timeStamp: Long, delta: Int)
    : AapMessage(Channel.ID_INP, Input.MSG_INPUT_EVENT, ScrollWheelEvent.makeProto(timeStamp, delta)) {
    companion object {
        const val KEYCODE_SCROLL_WHEEL = 65536

        private fun makeProto(timeStamp: Long, delta: Int): MessageNano {
            val inputReport = Input.InputReport()
            val keyEvent = Input.KeyEvent()
            // Timestamp in nanoseconds = microseconds x 1,000,000
            inputReport.timestamp = timeStamp * 1000000L
            inputReport.keyEvent = keyEvent

            val relativeEvent = Input.RelativeEvent()
            relativeEvent.data = arrayOfNulls<Input.RelativeEvent_Rel>(1)
            relativeEvent.data[0] = Input.RelativeEvent_Rel()
            relativeEvent.data[0].delta = delta
            relativeEvent.data[0].keycode = KEYCODE_SCROLL_WHEEL
            inputReport.relativeEvent = relativeEvent

            return inputReport
        }
    }

}
