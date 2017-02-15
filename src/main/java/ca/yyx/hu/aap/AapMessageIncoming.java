package ca.yyx.hu.aap;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 13/02/2017.
 */

class AapMessageIncoming extends AapMessage {

    static AapMessage decrypt(EncryptedHeader header, int offset, byte[] buf, AapSsl ssl)
    {
        if ((header.flags & 0x08) != 0x08) {
            AppLog.e("WRONG FLAG: enc_len: %d  chan: %d %s flags: 0x%02x  msg_type: 0x%02x %s",
                    header.enc_len, header.chan, Channel.name(header.chan), header.flags, header.msg_type, MsgType.name(header.msg_type, header.chan));
            return null;
        }

        ByteArray ba = ssl.decrypt(offset, header.enc_len, buf);
        if (ba == null) {
            return null;
        }

        AapMessage msg = new AapMessageIncoming(header, ba);

        if (AppLog.LOG_VERBOSE)
        {
            AppLog.d("RECV: " ,msg.toString());
        }
        return msg;
    }

    AapMessageIncoming(EncryptedHeader header, ByteArray ba) {
        super(header.chan, (byte) header.flags, Utils.bytesToInt(ba.data, 0, true), 2, ba.length, ba.data);
    }

    static class EncryptedHeader
    {
        final static int SIZE = 4;

        int chan;
        int flags;
        int enc_len;
        int msg_type;
        byte[] buf = new byte[SIZE];

        EncryptedHeader() {
        }

        void decode() {
            this.chan = (int) buf[0];
            this.flags = buf[1];

            // Encoded length of bytes to be decrypted (minus 4/8 byte headers)
            this.enc_len = Utils.bytesToInt(buf, 2, true);
        }

    }

}
