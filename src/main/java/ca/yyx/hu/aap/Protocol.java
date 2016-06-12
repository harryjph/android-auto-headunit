package ca.yyx.hu.aap;

import java.nio.ByteBuffer;

import ca.yyx.hu.decoder.MicRecorder;

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

    private static byte BYEBYE_REQUEST = 15; // 0x0f

    // Base 32768 // 0x8000

    //static final int VIDEO_REQUEST = 32775; // 0x8007 //0x08 0x00 0x00 0x07

    static final byte RESPONSE_MIC_STOP = 1;
    static final byte RESPONSE_MIC_START = 2;
    static final byte RESPONSE_AUDIO_STOP = 3;
    static final byte RESPONSE_AUDIO1_STOP = 4;
    static final byte RESPONSE_AUDIO2_STOP = 5;

    // Byebye Request:  000b0004000f0800  00 0b 00 04 00 0f 08 00
    static byte[] BUYBUY_REQUEST = { AA_CH_CTR, 0x0b, 0x00, 0x00, 0x00, BYEBYE_REQUEST, 0x08, 0 };
    //static byte[] VIDEO_REQUEST  = { AA_CH_CTR, 0x0b, 0x00, 0x00, 0x00, 0x08, 0x04 };

    // Message Type = 0 for data,
    // OR 32774 for Stop w/mandatory 0x08 int
    // and optional 0x10 int (senderprotocol/aq -> com.google.android.e.b.ca)
    static byte[] MIC_DATA       = { AA_CH_MIC, 0x0b,  0x00, 0x00, 0x00, 0x00 };

    static byte[] TOUCH_REQUEST  = { AA_CH_TOU, 0x0b, 0x00, 0x00, -128, 0x01, 0x08, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0, 0, 0,
            0x1a, 0x0e, 0x0a, 0x08, 0x08, 0x2e,
            0, 0x10, 0x2b, 0, 0x18, 0x00, 0x10, 0x00, 0x18, 0x00
    };

    static byte[] VIDEO_SETUP_REQUEST  = { AA_CH_VID, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00 };


    public static byte[] createMicBuffer()
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

    private void message(int messageType, byte[] data)
    {
        ByteBuffer bb = ByteBuffer.allocate(data.length+2);
        bb.putShort((short)messageType);
        bb.put(data, 0, data.length);
    }
}
