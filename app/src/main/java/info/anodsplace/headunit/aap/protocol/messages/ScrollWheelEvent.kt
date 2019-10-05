package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.proto.Input


class ScrollWheelEvent(timeStamp: Long, delta: Int)
    : AapMessage(Channel.ID_INP, Input.InputMsgType.EVENT_VALUE, makeProto(timeStamp, delta)) {
    companion object {
        const val KEYCODE_SCROLL_WHEEL = 65536

        private fun makeProto(timeStamp: Long, delta: Int): MessageLite {
            return Input.InputReport.newBuilder().also {
                it.timestamp = timeStamp * 1000000L
                it.keyEvent = Input.KeyEvent.newBuilder().build() // TODO: check if required
                it.relativeEvent = Input.RelativeEvent.newBuilder().also { event ->
                    event.addData(Input.RelativeEvent_Rel.newBuilder().apply {
                        setDelta(delta)
                        keycode = KEYCODE_SCROLL_WHEEL
                    })
                }.build()
            }.build()

        }
    }

}
