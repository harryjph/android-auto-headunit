package info.anodsplace.headunit.aap.protocol.messages

import android.view.MotionEvent
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.nano.Input
import com.google.protobuf.nano.MessageNano

/**
 * @author algavris
 * @date 13/02/2017.
 */
class TouchEvent(timeStamp: Long, action: Int, x: Int, y: Int)
    : AapMessage(Channel.ID_INP, Input.MSG_INPUT_EVENT, makeProto(timeStamp, action, x, y)) {

    companion object {
        fun motionEventToAction(event: MotionEvent): Int {
            return when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> Input.TouchEvent.TOUCH_ACTION_DOWN
                MotionEvent.ACTION_DOWN -> MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_MOVE -> MotionEvent.ACTION_MOVE
                MotionEvent.ACTION_CANCEL -> MotionEvent.ACTION_UP
                MotionEvent.ACTION_POINTER_UP -> MotionEvent.ACTION_POINTER_UP
                MotionEvent.ACTION_UP -> MotionEvent.ACTION_UP
                else -> {
                    -1
                }
            }
        }

        private fun makeProto(timeStamp: Long, action: Int, x: Int, y: Int): MessageNano {
            val inputReport = Input.InputReport()
            val touchEvent = Input.TouchEvent()
            inputReport.timestamp = timeStamp * 1000000L
            inputReport.touchEvent = touchEvent

            touchEvent.pointerData = arrayOfNulls<Input.TouchEvent.Pointer>(1)
            val pointer = Input.TouchEvent.Pointer()
            pointer.x = x
            pointer.y = y
            touchEvent.pointerData[0] = pointer
            touchEvent.actionIndex = 0
            touchEvent.action = action

            return inputReport
        }
    }
}
