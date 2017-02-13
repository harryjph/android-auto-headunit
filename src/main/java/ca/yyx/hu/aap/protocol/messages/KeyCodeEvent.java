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

public class KeyCodeEvent extends AapMessage {

    private static MessageNano makeProto(long timeStamp, int keycode, boolean isPress)
    {
        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.KeyEvent keyEvent = new Protocol.KeyEvent();
        // Timestamp in nanoseconds = microseconds x 1,000,000
        inputReport.timestamp = timeStamp * 1000000L;
        inputReport.keyEvent = keyEvent;

        keyEvent.keys = new Protocol.Key[1];
        keyEvent.keys[0] = new Protocol.Key();
        keyEvent.keys[0].keycode = keycode;
        keyEvent.keys[0].down = isPress;

        return inputReport;
    }

    public KeyCodeEvent(long timeStamp, int keycode, boolean isPress)
    {
        super(Channel.ID_INP, MsgType.Input.EVENT, makeProto(timeStamp, keycode, isPress));
    }
}
