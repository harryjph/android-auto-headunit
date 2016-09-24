package ca.yyx.hu.aap;

import ca.yyx.hu.decoder.MicRecorder;

/**
 * @author algavris
 * @date 08/06/2016.
 */

public class Protocol {
    static final int DEF_BUFFER_LENGTH = 131080;

    static final int BTN_UP = 0x13;
    static final int BTN_DOWN = 0x14;
    static final int BTN_LEFT = 0x15;
    static final int BTN_RIGHT = 0x16;
    static final int BTN_BACK = 0x04;
    static final int BTN_ENTER = 0x17;
    static final int BTN_MIC = 0x54;
    static final int BTN_PHONE = 0x5;
    static final int BTN_START = 126;

    public static final int BTN_PLAYPAUSE = 0x55;
    public static final int BTN_NEXT = 0x57;
    public static final int BTN_PREV = 0x58;
    public static final int BTN_STOP = 127;

    static ByteArray createMessage(int chan, int flags, int type, byte[] data, int size) {

        ByteArray buffer = new ByteArray(6 + size);

        buffer.put(chan, flags);
        buffer.encodeInt(size + 2);

        if (type >= 0) {
            // If type not negative, which indicates encrypted type should not be touched...
            buffer.encodeInt(type);
        }

        buffer.put(data, size);
        return buffer;
    }

    static ByteArray createButtonMessage(long timeStamp, int button, boolean isPress)
    {
        ByteArray buffer = new ByteArray(22);

        buffer.put(0x80, 0x01, 0x08);
        int size = Encode.longToByteArray(timeStamp, buffer.data, buffer.length);
        buffer.move(size);

        int press = isPress ? 0x01 : 0x00;
        buffer.put(0x22, 0x0A, 0x0A, 0x08, 0x08, button, 0x10, press, 0x18, 0x00, 0x20, 0x00);
        return buffer;
     }

    static final byte RESPONSE_MIC_STOP = 1;
    static final byte RESPONSE_MIC_START = 2;
    static final byte RESPONSE_AUDIO_STOP = 3;
    static final byte RESPONSE_AUDIO1_STOP = 4;
    static final byte RESPONSE_AUDIO2_STOP = 5;

    static byte[] VERSION_REQUEST = { 0, 1, 0, 1 };
    static byte[] BYEBYE_REQUEST = { 0x00, 0x0f, 0x08, 0x00 };

    static byte[] createMicBuffer()
    {
        return new byte[10 + MicRecorder.MIC_BUFFER_SIZE];
    }

    static ByteArray createTouchMessage(long timeStamp, byte action, int x, int y) {
        ByteArray buffer = new ByteArray(32);

        buffer.put(0x80, 0x01, 0x08);

        int size = Encode.longToByteArray(timeStamp, buffer.data, buffer.length);          // Encode timestamp
        buffer.move(size);

        int size1_idx = buffer.length + 1;
        int size2_idx = buffer.length + 3;

        buffer.put(0x1a, 0x09, 0x0a, 0x03);

        /* Set magnitude of each axis */
        byte axis = 0;
        int[] coordinates = {x, y, 0};
        for (int i=0; i<3; i++) {
            axis += 0x08; //0x08, 0x10, 0x18
            buffer.put(axis);
            size = Encode.intToByteArray(coordinates[i], buffer.data, buffer.length);
            buffer.move(size);
            buffer.inc(size1_idx, size);
            buffer.inc(size2_idx, size);
        }

        buffer.put(0x10, 0x00, 0x18, action);
        return buffer;
    }

    static byte[] createNightModeMessage(boolean enabled) {
        byte[] buffer = new byte[6];

        buffer[0] = -128;
        buffer[1] = 0x03;
        buffer[2] = 0x52;
        buffer[3] = 0x02;
        buffer[4] = 0x08;
        if (enabled)
            buffer[5] = 0x01;
        else
            buffer[5]= 0x00;

        return buffer;
    }
}
