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
        if (header.chan == Channel.ID_VID && header.flags == 9) {
            // If First fragment Video...
            // (Packet is encrypted so we can't get the real msg_type or check for 0, 0, 0, 1)
            int total_size = Utils.bytesToInt(buf, offset, false);
            AppLog.v("First fragment total_size: %d", total_size);
            offset += 4;
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
        final static int SIZE = 6;

        int chan;
        int flags;
        int enc_len;
        int msg_type;

        EncryptedHeader() {
        }

        void decode(int offset, byte[] buf) {
            this.chan = (int) buf[offset];
            this.flags = buf[offset + 1];

            // Encoded length of bytes to be decrypted (minus 4/8 byte headers)
            this.enc_len = Utils.bytesToInt(buf, offset + 2, true);

            // Message Type (or post handshake, mostly indicator of SSL encrypted data)
            this.msg_type = Utils.bytesToInt(buf, offset + 4, true);
        }

    }

}
