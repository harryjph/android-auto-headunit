package ca.yyx.hu.aap;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 13/02/2017.
 */

class AapReadMultipleMessages extends AapRead.Base {

    AapReadMultipleMessages(AccessoryConnection connection, AapSsl ssl, AapMessageHandler handler) {
        super(connection, ssl, handler);
    }

    @Override
    protected int doRead() {

        int size = mConnection.recv(recv_buffer, recv_buffer.length, 150);
        if (size <= 0) {
//            AppLog.v("recv %d", size);
            return 0;
        }
        try {
            processBulk(size, recv_buffer);
        } catch (AapMessageHandler.HandleException e) {
            return -1;
        }
        return 0;
    }

    private void processBulk(int buf_len, byte[] buf) throws AapMessageHandler.HandleException {

        int body_start = 0;
        int have_len = buf_len;

        while (have_len > 0) {
            int msg_start = body_start;
            recv_header.decode(body_start, buf);

            // Length starting at byte 4: Unencrypted Message Type or Encrypted data start
            have_len -= 4;
            body_start +=4;

            if (recv_header.chan == Channel.ID_VID && recv_header.flags == 9) {
                have_len -= 4;
            }

            int need_len = recv_header.enc_len - have_len;
            if (need_len > 0) {
                AppLog.e("Need more - offset: %d, msg_start: %d, enc_len: %d. need_len: %d, buf_len: %d, buf_limit: %d",
                        body_start + have_len, // 6436
                        msg_start, // 4
                        recv_header.enc_len, // 26816
                        need_len, // 20384
                        buf_len, // 6436
                        buf.length); // 131080

                return;
            }

            AapMessage msg = AapMessageIncoming.decrypt(recv_header, body_start, recv_buffer, mSsl);

            // Decrypt & Process 1 received encrypted message
            if (msg == null) {
                // If error...
                AppLog.e("Error iaap_recv_dec_process: enc_len: %d chan: %d %s flags: %01x msg_type: %d", recv_header.enc_len, recv_header.chan, Channel.name(recv_header.chan), recv_header.flags, recv_header.msg_type);
                continue;
            }

            mHandler.handle(msg);

            have_len -= recv_header.enc_len;
            body_start += recv_header.enc_len;
            if (have_len != 0) {
                AppLog.i("iaap_recv_dec_process() more than one message have_len: %d  enc_len: %d", have_len, recv_header.enc_len);
            }

        }

    }

}
