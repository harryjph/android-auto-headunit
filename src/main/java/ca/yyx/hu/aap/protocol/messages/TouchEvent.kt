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

class TouchEvent(timeStamp: Long, action: Int, x: Int, y: Int)
    : AapMessage(Channel.ID_INP, MsgType.Input.EVENT, TouchEvent.makeProto(timeStamp, action, x, y)) {

    companion object {
        private fun makeProto(timeStamp: Long, action: Int, x: Int, y: Int): MessageNano {
            val inputReport = Protocol.InputReport()
            val touchEvent = Protocol.TouchEvent()
            inputReport.timestamp = timeStamp * 1000000L
            inputReport.touchEvent = touchEvent

            touchEvent.pointerData = arrayOfNulls<Protocol.TouchEvent.Pointer>(1)
            val pointer = Protocol.TouchEvent.Pointer()
            pointer.x = x
            pointer.y = y
            touchEvent.pointerData[0] = pointer
            touchEvent.actionIndex = 0
            touchEvent.action = action

            return inputReport
        }
    }
}
