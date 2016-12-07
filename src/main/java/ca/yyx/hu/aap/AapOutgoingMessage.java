package ca.yyx.hu.aap;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.protocol.MsgType;

/**
 * @author algavris
 * @date 06/12/2016.
 */

class AapOutgoingMessage {
    final int channel;
    final MessageNano proto;
    final int type;
    final int size;

    AapOutgoingMessage(int channel, int type, MessageNano proto) {
        this.channel = channel;
        this.proto = proto;
        this.type = type;
        this.size = proto.getSerializedSize() + MsgType.SIZE;
    }

    byte[] byteArray()
    {
        byte[] buf = new byte[this.size];
        return byteArray(buf);
    }

    byte[] byteArray(byte[] buf)
    {
        // Header
        int msgType = this.type;
        buf[0] = (byte) (msgType >> 8);
        buf[1] = (byte) (msgType & 0xFF);
        MessageNano.toByteArray(this.proto, buf, MsgType.SIZE, this.proto.getSerializedSize());
        return buf;
    }
}
