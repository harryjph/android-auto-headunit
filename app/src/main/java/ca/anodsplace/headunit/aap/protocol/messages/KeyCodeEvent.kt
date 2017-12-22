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

class KeyCodeEvent(timeStamp: Long, keycode: Int, isPress: Boolean)
    : AapMessage(Channel.ID_INP, Input.MSG_INPUT_EVENT, KeyCodeEvent.makeProto(timeStamp, keycode, isPress)) {

    companion object {
        private fun makeProto(timeStamp: Long, keycode: Int, isPress: Boolean): MessageNano {
            val inputReport = Input.InputReport()
            val keyEvent = Input.KeyEvent()
            // Timestamp in nanoseconds = microseconds x 1,000,000
            inputReport.timestamp = timeStamp * 1000000L
            inputReport.keyEvent = keyEvent

            keyEvent.keys = arrayOfNulls<Input.Key>(1)
            keyEvent.keys[0] = Input.Key()
            keyEvent.keys[0].keycode = keycode
            keyEvent.keys[0].down = isPress

            return inputReport
        }
    }
}
