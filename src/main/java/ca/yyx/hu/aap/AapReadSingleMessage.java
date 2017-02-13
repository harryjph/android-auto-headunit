package ca.yyx.hu.aap;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 13/02/2017.
 */

class AapReadSingleMessage extends AapRead.Base {

    AapReadSingleMessage(AccessoryConnection connection, AapSsl ssl, AapMessageHandler handler) {
        super(connection, ssl, handler);
    }

    @Override
    protected int doRead() {
        int header_size = mConnection.recv(recv_buffer, AapMessageIncoming.EncryptedHeader.SIZE, 150);
        if (header_size != AapMessageIncoming.EncryptedHeader.SIZE) {
            AppLog.v("Header: recv %d", header_size);
            return -1;
        }

        recv_header.decode(0, recv_buffer);

        int msg_size = mConnection.recv(recv_buffer, recv_header.enc_len, 150);
        if (msg_size != recv_header.enc_len) {
            AppLog.v("Message: recv %d", msg_size);
            return -1;
        }

        try {
            AapMessage msg = AapMessageIncoming.decrypt(recv_header, 0, recv_buffer, mSsl);

            // Decrypt & Process 1 received encrypted message
            if (msg == null) {
                // If error...
                AppLog.e("Error iaap_recv_dec_process: enc_len: %d chan: %d %s flags: %01x msg_type: %d", recv_header.enc_len, recv_header.chan, Channel.name(recv_header.chan), recv_header.flags, recv_header.msg_type);
                return -1;
            }

            mHandler.handle(msg);
            return 0;
        } catch (AapMessageHandler.HandleException e) {
            return -1;
        }
    }
}
