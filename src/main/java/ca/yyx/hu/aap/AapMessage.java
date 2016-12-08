package ca.yyx.hu.aap;


import com.google.protobuf.nano.MessageNano;

import java.util.Locale;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;

/**
 * @author algavris
 * @date 04/10/2016.
 */
class AapMessage {
    private final static int HEADER_SIZE = 4;

    final byte[] data;
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
        if (channel != Channel.AA_CH_CTR && MsgType.isControl(type)) {
            // Set Control Flag (On non-control channels, indicates generic/"control type" messages
            flags = 0x0f;
        }
        return flags;
    }

    AapMessage(int channel, byte flags, int msg_type, int dataOffset, int size, byte[] data) {
        this.data = data;
        this.size = size;
        this.channel = channel;
        this.flags = flags;
        this.type = msg_type;
        this.dataOffset = dataOffset;
    }

    AapMessage(int channel, int flags, int size, byte[] data) {
        this(channel, (byte) flags, -1, HEADER_SIZE, size, data);
    }

    AapMessage(int channel, int type, MessageNano proto) {
        this(channel, type, proto, new byte[size(proto)]);
    }

    AapMessage(int channel, int type, MessageNano proto, byte[] buf) {
        this(channel, flags(channel, type), type, MsgType.SIZE + HEADER_SIZE, size(proto), buf);

        int msgType = this.type;
        this.data[0] = (byte) channel;
        this.data[1] = flags;
        this.data[2] = (byte) (msgType >> 8);
        this.data[3] = (byte) (msgType & 0xFF);
        MessageNano.toByteArray(proto, this.data, MsgType.SIZE + HEADER_SIZE, proto.getSerializedSize());
    }

    public boolean isAudio() {
        return Channel.isAudio(this.channel);
    }

    public boolean isVideo() {
        return this.channel == Channel.AA_CH_VID;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(Channel.name(channel));
        sb.append(' ');
        sb.append(MsgType.name(type, channel));
        sb.append(' ');

        for (int i = 0; i < Math.max(this.size, 64); i++) {
            String hex = String.format(Locale.US, "%02X", data[i]);
            sb.append(hex);
            sb.append(' ');
        }

        /*
         String prefix = String.format(Locale.US, "SEND %d %s %01x", chan, Channel.name(chan), flags);
        AapDump.logd(prefix, "HU", chan, flags, buf, len);
         */

        return sb.toString();
    }
}