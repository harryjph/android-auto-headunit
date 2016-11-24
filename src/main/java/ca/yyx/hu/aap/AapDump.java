package ca.yyx.hu.aap;

import android.util.Log;

import java.util.Locale;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.utils.AppLog;

class AapDump {
    private static final int MAX_HEX_DUMP  = 64;//32;
    private static final int HD_MW = 16;

    static int logd(String prefix, String src, int chan, int flags, byte[] buf, int len) {
        // Source:  HU = HU > AA   AA = AA > HU

        if (!AppLog.LOG_DEBUG)
        {
            return 0;
        }

        if (len < 2) {
            // If less than 2 bytes, needed for msg_type...
            AppLog.e("hu_aad_dmp len: %d", len);
            return 0;
        }

        int rmv = 0;
        int lft = len;
        int msg_type = ((((int) buf[0]) << 8) + ((int) buf[1])) & 0xFFFF;

        boolean is_media = false;
        if (chan == Channel.AA_CH_VID || chan == Channel.AA_CH_MIC || Channel.isAudio(chan))
            is_media = true;

        if (is_media && (flags == 8 || flags == 0x0a || msg_type == 0)) // || msg_type ==1)    // Ignore Video/Audio Data
            return (rmv);

        if (is_media && msg_type == 32768 + 4)                                                 // Ignore Video/Audio Ack
            return (rmv);

        // msg_type = 2 bytes
        rmv+= 2;
        lft-= 2;

        String msg_type_str = iaad_msg_type_str_get(msg_type, src, lft);   // Get msg_type string
        if (flags == 0x08)
            Log.d(AppLog.TAG, String.format("%s src: %s  lft: %5d  Media Data Mid", prefix, src, lft));
        else if (flags == 0x0a)
            Log.d(AppLog.TAG, String.format("%s src: %s  lft: %5d  Media Data End", prefix, src, lft));
        else
            Log.d(AppLog.TAG, String.format("%s src: %s  lft: %5d  msg_type: %5d %s", prefix, src, lft, msg_type, msg_type_str));

        logvHex(prefix, 2, buf, lft);                                  // Hexdump

        if (flags == 0x08)                                                  // If Media Data Mid...
            return (len);                                                     // Done

        if (flags == 0x0a)                                                  // If Media Data End...
            return (len);                                                     // Done

        if (msg_type_enc_get(msg_type) == null) {                          // If encrypted data...
            return (len);                                                     // Done
        }

        if (lft == 0)                                                       // If no content
            return (rmv);                                                     // Done

        if (lft < 2) {                                                      // If less than 2 bytes for content (impossible if content; need at least 1 byte for 0x08 and 1 byte for varint)
            AppLog.e("hu_aad_dmp len: %d  lft: %d", len, lft);
            return (rmv);                                                     // Done with error
        }

        //adj = iaad_dmp_n(1, 1, buf, lft);                                  // Dump the content w/ n=1

        // iaad_adj(&rmv, &buf, &lft, adj);                                // Adjust past the content (to nothing, presumably)

//        if (lft != 0 || rmv != len || rmv < 0)                              // If content left... (must be malformed)
//            AppLog.e ("hu_aad_dmp after content len: %d  lft: %d  rmv: %d  buf: %p", len, lft, rmv, buf);

        AppLog.i("--------------------------------------------------------");  // Empty line / 56 characters

        return (rmv);
    }


    private static final int MSG_TYPE_32 = 32768;
    private static String iaad_msg_type_str_get(int msg_type, String src, int len) {   // Source:  HU = HU > AA   AA = AA > HU

        switch (msg_type) {
            case 0:
                return ("Media Data");
            case 1:
                if (src.charAt(0) == 'H') return ("Version Request");    // Start AA Protocol
            else if (src.charAt(0) == 'A')
            return ("Codec Data");         // First Video packet, respond with Media Ack
            else return ("1 !");
            case 2:
                return ("Version Response");                            // short:major  short:minor   short:status
            case 3:
                return ("SSL Handshake Data");                          // First/Request from HU, Second/Response from AA
            //case 5123:return ("SSL Change Cipher Spec");                      // 0x1403
            //case 5379:return ("SSL Alert");                                   // 0x1503
            //case 5635:return ("SSL Handshake");                               // 0x1603
            //case 5891:return ("SSL App Data");                                // 0x1703
            case 4:
                return ("SSL Authentication Complete Notification");
            case 5:
                return ("Service Discovery Request");
            case 6:
                return ("Service Discovery Response");
            case 7:
                return ("Channel Open Request");
            case 8:
                return ("Channel Open Response");                       // byte:status
            case 9:
                return ("9 ??");
            case 10:
                return ("10 ??");
            case 11:
                return ("Ping Request");
            case 12:
                return ("Ping Response");
            case 13:
                return ("Navigation Focus Request");
            case 14:
                return ("Navigation Focus Notification");               // NavFocusType
            case 15:
                return ("Byebye Request");
            case 16:
                return ("Byebye Response");
            case 17:
                return ("Voice Session Notification");
            case 18:
                return ("Audio Focus Request");
            case 19:
                return ("Audio Focus Notification");                    // AudioFocusType   (AudioStreamType ?)

            case MSG_TYPE_32:// + 0:
                return ("Media Setup Request");                        // Video and Audio sinks receive this and send k3 3 / 32771
            case MSG_TYPE_32 + 1:
                if (src.charAt(0) == 'H') return ("Touch Notification");
            else if (src.charAt(0) == 'A') return ("Sensor/Media Start Request");
            else return ("32769 !");            // src AA also Media Start Request ????
            case MSG_TYPE_32 + 2:
                if (src.charAt(0) == 'H') return ("Sensor Start Response");
            else if (src.charAt(0) == 'A') return ("Touch/Input/Audio Start/Stop Request");
            else return ("32770 !");            // src AA also Media Stop Request ?
            case MSG_TYPE_32 + 3:
                if (len == 6) return ("Media Setup Response");
                else if (len == 2) return ("Key Binding Response");
                else return ("Sensor Notification");
            case MSG_TYPE_32 + 4:
                return ("Codec/Media Data Ack");
            case MSG_TYPE_32 + 5:
                return ("Mic Start/Stop Request");
            case MSG_TYPE_32 + 6:
                return ("k3 6 ?");
            case MSG_TYPE_32 + 7:
                return ("Media Video ? Request");
            case MSG_TYPE_32 + 8:
                return ("Video Focus Notification");

            case 65535:
                return ("Framing Error Notification");

        }
        return ("Unknown");
    }

    private static String msg_type_enc_get(int msg_type) {

        switch (msg_type) {
            case 5123:
                return ("SSL Change Cipher Spec");                    // 0x1403
            case 5379:
                return ("SSL Alert");                                 // 0x1503
            case 5635:
                return ("SSL Handshake");                             // 0x1603
            case 5891:
                return ("SSL App Data");                              // 0x1703
        }
        return null;
    }


    static void logvHex(String prefix, int start, byte[] buf, int len) {

        if (!AppLog.LOG_VERBOSE)
        {
            return;
        }

        if (len + start > MAX_HEX_DUMP)
            len = MAX_HEX_DUMP + start;

        int i, n;

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        String line = String.format(Locale.US, " %08d ", 0 );
        sb.append(line);

        for (i = start, n = 1; i < len; i ++, n ++) {                           // i keeps incrementing, n gets reset to 0 each line

            String hex = String.format(Locale.US, "%02X ", buf [i]);
            sb.append(hex);

            if (n == HD_MW) {                                                 // If at specified line width
                n = 0;                                                          // Reset position in line counter
                Log.v(AppLog.TAG,sb.toString());                                                    // Log line

                sb.setLength(0);

                sb.append(prefix);
                line = String.format(Locale.US, "     %04d ", i + 1 );
                sb.append(line);
            } else if (i == len - 1) {                                           // Else if at last byte
                Log.v(AppLog.TAG,sb.toString());                                    // Log line
            }
        }
    }
}
