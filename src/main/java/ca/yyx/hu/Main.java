package ca.yyx.hu;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.util.Locale;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 26/11/2016.
 */
public class Main {
    public static void main(String[] args) throws InvalidProtocolBufferNanoException {
        System.out.println("Main");

        Protocol.VideoFocusNotification videoFocus = new Protocol.VideoFocusNotification();
        videoFocus.mode = 1;
        videoFocus.unsolicited = true;

        AapMessage aap = new AapMessage(Channel.ID_VID, MsgType.Media.VIDEOFOCUSNOTIFICATION, videoFocus);

        printByteArray(aap.data);

        byte rsp2[] = {(byte) 0x80, 0x08, 0x08, 1, 0x10, 1};
        int flags = 0x0b;
        ByteArray msg = createMessage(Channel.ID_VID, flags, -1, rsp2, rsp2.length);
        printByteArray(msg.data);

    }


    static ByteArray createMessage(int chan, int flags, int type, byte[] data, int size) {

        ByteArray buffer = new ByteArray(6 + size);

        buffer.put(chan, flags);

        if (type >= 0) {
            buffer.encodeInt(size + 2);
            // If type not negative, which indicates encrypted type should not be touched...
            buffer.encodeInt(type);
        } else {
            buffer.encodeInt(size);
        }

        buffer.put(data, size);
        return buffer;
    }

    private static void printByteArray(byte[] ba)
    {
        for (int i = 0; i < ba.length; i++) {
            String hex = String.format(Locale.US, "%02X", ba[i]);
            System.out.print(hex);
//            int pos = (ba[i] >> 3);
//            if (pos > 0) {
//                System.out.print("[" + pos + "]");
//            }
            System.out.print(' ');
        }
        System.out.println();
    }

    public static class ByteArray {
        public final byte[] data;
        public final int offset;
        public int length;

        public ByteArray(int offset, byte[] data, int length)
        {
            this.data = data;
            this.length = length;
            this.offset = offset;
        }

        public ByteArray(int maxSize) {
            this.data = new byte[maxSize];
            this.length = 0;
            this.offset = 0;
        }

        public void put(int value) {
            this.data[this.length++] = (byte) value;
        }

        public void put(int... values) {
            for (int i = 0; i < values.length; i++)
            {
                this.put(values[i]);
            }
        }

        public void move(int size) {
            this.length += size;
        }

        public void inc(int index, int value) {
            this.data[index] += value;
        }

        public void encodeInt(int value) {
            this.put((byte) (value / 256));                                            // Encode length of following data:
            this.put((byte) (value % 256));
        }

        public void put(byte[] data, int size) {
            this.put(0, data, size);
        }

        public void put(int start, byte[] data, int size) {
            System.arraycopy(data, start, this.data, this.length, size);
            this.length += size;
        }

        public void reset()
        {
            this.length = 0;
        }

    }

}
