package ca.yyx.hu.aap;

import android.os.SystemClock;

import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 08/06/2016.
 */

public class Protocol {
    private static final int AA_CH_CTR = 0;                               // Sync with AapTransport.java, hu_aap.h and hu_aap.c:aa_type_array[]
    private static final int AA_CH_TOU = 3;
    private static final int AA_CH_SEN = 1;
    private static final int AA_CH_VID = 2;
    private static final int AA_CH_MIC = 7;

    static final int BTN_UP = 0x13;
    static final int BTN_DOWN = 0x14;
    static final int BTN_LEFT = 0x15;
    static final int BTN_RIGHT = 0x16;
    static final int BTN_BACK = 0x04;
    static final int BTN_ENTER = 0x17;
    static final int BTN_MIC = 0x54;
    public static final int BTN_PLAYPAUSE = 0x55;
    public static final int BTN_NEXT = 0x57;
    public static final int BTN_PREV = 0x58;
    static final int BTN_PHONE = 0x5;
    static final int BTN_START = 126;
    static final int BTN_STOP = 127;

    static byte[] createButtonMessage(long timeStamp, int button, boolean isPress)
    {
        byte[] buffer = new byte[26];

        int buffCount = 0;

        buffer[buffCount++] = AA_CH_TOU;
        buffer[buffCount++] = 0x0b;
        buffer[buffCount++] = 0x00;
        buffer[buffCount++] = 0x00;

        buffer[buffCount++] = (byte) 0x80;
        buffer[buffCount++] = 0x01;
        buffer[buffCount++] = 0x08;
        buffCount += Utils.varint_encode(timeStamp, buffer, buffCount);
        buffer[buffCount++] = 0x22;
        buffer[buffCount++] = 0x0A;
        buffer[buffCount++] = 0x0A;
        buffer[buffCount++] = 0x08;
        buffer[buffCount++] = 0x08;
        buffer[buffCount++] = (byte) button;
        buffer[buffCount++] = 0x10;
        buffer[buffCount++] = (byte) (isPress ? 0x01 : 0x00);
        buffer[buffCount++] = 0x18;
        buffer[buffCount++] = 0x00;
        buffer[buffCount++] = 0x20;
        buffer[buffCount++] = 0x00;
        return buffer;
     }

    static final byte RESPONSE_MIC_STOP = 1;
    static final byte RESPONSE_MIC_START = 2;
    static final byte RESPONSE_AUDIO_STOP = 3;
    static final byte RESPONSE_AUDIO1_STOP = 4;
    static final byte RESPONSE_AUDIO2_STOP = 5;

    static byte[] BYEBYE_REQUEST = { AA_CH_CTR, 0x0b, 0x00, 0x00, 0x00, 0x0f, 0x08, 0 };

    static byte[] TOUCH_REQUEST  = {
            AA_CH_TOU, 0x0b, 0x00, 0x00,
            -128, 0x01, 0x08,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0, 0, 0,
            0x1a, 0x0e, 0x0a, 0x08, 0x08, 0x2e,
            0, 0x10, 0x2b, 0, 0x18, 0x00, 0x10, 0x00, 0x18, 0x00
    };

    static byte[] createMicBuffer()
    {
        byte[] mic_buf = new byte[14 + MicRecorder.MIC_BUFFER_SIZE];
        mic_buf[0] = AA_CH_MIC;// Mic channel
        mic_buf[1] = 0x0b;  // Flag filled here
        mic_buf[2] = 0x00;  // 2 bytes Length filled here
        mic_buf[3] = 0x00;
        mic_buf[4] = 0x00;  // Message Type = 0 for data, OR 32774 for Stop w/mandatory 0x08 int and optional 0x10 int (senderprotocol/aq -> com.google.android.e.b.ca)
        mic_buf[5] = 0x00;
        return mic_buf;
    }

    static int createTouchMessage(byte[] ba_touch, long timeStamp, byte action, int x, int y) {
        int idx = 1 + 6 + Utils.varint_encode(timeStamp, ba_touch, 1 + 6);          // Encode timestamp

        ba_touch[idx++] = 0x1a;                                           // Value 3 array
        int size1_idx = idx;                                                // Save size1_idx
        ba_touch[idx++] = 0x0a;                                           // Default size 10
//
        ba_touch[idx++] = 0x0a;                                           // Contents = 1 array
        int size2_idx = idx;                                                // Save size2_idx
        ba_touch[idx++] = 0x04;                                           // Default size 4
        //
        ba_touch[idx++] = 0x08;                                             // Value 1
        int siz_arr = Utils.varint_encode(x, ba_touch, idx);                 // Encode X
        idx += siz_arr;
        ba_touch[size1_idx] += siz_arr;                                    // Adjust array sizes for X
        ba_touch[size2_idx] += siz_arr;

        ba_touch[idx++] = 0x10;                                             // Value 2
        siz_arr = Utils.varint_encode(y, ba_touch, idx);                 // Encode Y
        idx += siz_arr;
        ba_touch[size1_idx] += siz_arr;                                    // Adjust array sizes for Y
        ba_touch[size2_idx] += siz_arr;

        ba_touch[idx++] = 0x18;                                             // Value 3
        ba_touch[idx++] = 0x00;                                           // Encode Z ?
        //
        ba_touch[idx++] = 0x10;
        ba_touch[idx++] = 0x00;

        ba_touch[idx++] = 0x18;
        ba_touch[idx++] = action;

        int len_touch = idx;
        return len_touch;
    }
}
