package ca.yyx.hu.aap.protocol.messages;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 13/02/2017.
 */

public class TouchEvent extends AapMessage {

    private static MessageNano makeProto(long timeStamp, int action, int x, int y)
    {
        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.TouchEvent touchEvent = new Protocol.TouchEvent();
        inputReport.timestamp = timeStamp * 1000000L;
        inputReport.touchEvent = touchEvent;

        touchEvent.pointerData = new Protocol.TouchEvent.Pointer[1];
        Protocol.TouchEvent.Pointer pointer = new Protocol.TouchEvent.Pointer();
        pointer.x = x;
        pointer.y = y;
        touchEvent.pointerData[0] = pointer;
        touchEvent.actionIndex = 0;
        touchEvent.action = action;

        return inputReport;
    }

    public TouchEvent(long timeStamp, int action, int x, int y)
    {
        super(Channel.ID_INP, MsgType.Input.EVENT, makeProto(timeStamp, action, x, y));
    }

}
