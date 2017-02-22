package ca.yyx.hu.aap.protocol.messages;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 13/02/2017.
 */

public class MediaAck extends AapMessage {

    private final static Protocol.Ack mediaAck = new Protocol.Ack();
    private final static byte[] ackBuf = new byte[20];

    private static MessageNano makeProto(int sessionId) {
        mediaAck.clear();
        mediaAck.sessionId = sessionId;
        mediaAck.ack = 1;

        return mediaAck;
    }

    public MediaAck(int channel, int sessionId)
    {
        super(channel, MsgType.Media.ACK, makeProto(sessionId), ackBuf);
    }
}
