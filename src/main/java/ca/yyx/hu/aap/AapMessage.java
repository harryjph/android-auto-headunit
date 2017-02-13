package ca.yyx.hu.aap;


import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 04/10/2016.
 */
public class AapMessage {
    final static int HEADER_SIZE = 4;

    public final byte[] data;
    final int size;
    final int channel;
    final byte flags;
    final int type;
    final int dataOffset;

    private static int size(MessageNano proto)
    {
        return proto.getSerializedSize() + MsgType.SIZE + HEADER_SIZE;
    }

    private static byte flags(int channel, int type)
    {
        byte flags = 0x0b;
        if (channel != Channel.ID_CTR && MsgType.isControl(type)) {
            // Set Control Flag (On non-control channels, indicates generic/"control type" messages
            flags = 0x0f;
        }
        return flags;
    }

    public AapMessage(int channel, byte flags, int msg_type, int dataOffset, int size, byte[] data) {
        this.data = data;
        this.size = size;
        this.channel = channel;
        this.flags = flags;
        this.type = msg_type;
        this.dataOffset = dataOffset;
    }

    public AapMessage(int channel, int type, MessageNano proto) {
        this(channel, type, proto, new byte[size(proto)]);
    }

    public AapMessage(int channel, int type, MessageNano proto, byte[] buf) {
        this(channel, flags(channel, type), type, HEADER_SIZE + MsgType.SIZE, size(proto), buf);

        int msgType = this.type;
        this.data[0] = (byte) channel;
        this.data[1] = flags;
        Utils.intToBytes(proto.getSerializedSize() + MsgType.SIZE, 2, this.data);
        this.data[4] = (byte) (msgType >> 8);
        this.data[5] = (byte) (msgType & 0xFF);
        MessageNano.toByteArray(proto, this.data, HEADER_SIZE + MsgType.SIZE, proto.getSerializedSize());
    }

    public boolean isAudio() {
        return Channel.isAudio(this.channel);
    }

    public boolean isVideo() {
        return this.channel == Channel.ID_VID;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Channel.name(channel));
        sb.append(' ');
        sb.append(MsgType.name(type, channel));
        sb.append('\n');

        AapDump.logHex("", 0, data, this.size, sb);

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        AapMessage msg = (AapMessage) obj;

        if (msg.channel != this.channel) {
            return false;
        }
        if (msg.flags != this.flags) {
            return false;
        }
        if (msg.type != this.type) {
            return false;
        }
        if (msg.size != this.size || msg.data.length < this.size) {
            return false;
        }
        if (msg.dataOffset != this.dataOffset) {
            return false;
        }
        for (int i = 0; i < this.size; i++)
        {
            if (msg.data[i] != this.data[i])
            {
                return false;
            }
        }
        return true;
    }
}