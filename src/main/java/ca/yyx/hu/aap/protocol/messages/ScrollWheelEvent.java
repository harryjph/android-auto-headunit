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

public class ScrollWheelEvent extends AapMessage {
    public static final int KEYCODE_SCROLL_WHEEL = 65536;

    private static MessageNano makeProto(long timeStamp, int delta)
    {
        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.KeyEvent keyEvent = new Protocol.KeyEvent();
        // Timestamp in nanoseconds = microseconds x 1,000,000
        inputReport.timestamp = timeStamp * 1000000L;
        inputReport.keyEvent = keyEvent;

        Protocol.RelativeEvent relativeEvent = new Protocol.RelativeEvent();
        relativeEvent.data = new Protocol.RelativeEvent_Rel[1];
        relativeEvent.data[0] = new Protocol.RelativeEvent_Rel();
        relativeEvent.data[0].delta = delta;
        relativeEvent.data[0].keycode = KEYCODE_SCROLL_WHEEL;
        inputReport.relativeEvent = relativeEvent;

        return inputReport;
    }

    public ScrollWheelEvent(long timeStamp, int delta)
    {
        super(Channel.ID_INP, MsgType.Input.EVENT, makeProto(timeStamp, delta));
    }

}
