package ca.yyx.hu.aap;


import com.google.protobuf.nano.MessageNano;

import net.hockeyapp.android.utils.Util;

import java.util.Locale;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 04/10/2016.
 */
public class AapMessage {
    public final static int HEADER_SIZE = 4;

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

    public AapMessage(int channel, int type, MessageNano proto) {
        this(channel, type, proto, new byte[size(proto)]);
    }

    AapMessage(int channel, int type, MessageNano proto, byte[] buf) {
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
        return this.channel == Channel.AA_CH_VID;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Channel.name(channel));
        sb.append(' ');
        sb.append(MsgType.name(type, channel));
        sb.append('\n');

        for (int i = 0; i < Math.min(this.size, 64); i++) {
            String hex = String.format(Locale.US, "%02X", data[i]);
            sb.append(hex);
            sb.append(' ');
        }

        return sb.toString();
    }
}