package ca.yyx.hu.aap;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.messages.Messages;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 13/02/2017.
 */

class AapReadMultipleMessages extends AapRead.Base {

    private final ByteBuffer fifo = ByteBuffer.allocate(Messages.DEF_BUFFER_LENGTH * 2);
    private byte[] recv_buffer = new byte[Messages.DEF_BUFFER_LENGTH];
    private final AapMessageIncoming.EncryptedHeader recv_header = new AapMessageIncoming.EncryptedHeader();
    private byte[] msg_buffer = new byte[65535]; // unsigned short max

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

    private void processBulk(int size, byte[] buf) throws AapMessageHandler.HandleException {

        fifo.put(buf, 0, size);
        fifo.flip();

        while (fifo.hasRemaining()) {


            // Parse the header
            try {
                fifo.get(recv_header.buf, 0, recv_header.buf.length);
            } catch (BufferUnderflowException e) {
                // we'll come back later for more data
                AppLog.v("BufferUnderflowException whilst trying to read 4 bytes capacity = %d, position = %d", fifo.capacity(), fifo.position());
                break;
            }
            recv_header.decode();

            if (recv_header.chan == Channel.ID_VID && (recv_header.flags & 0x01) == 0x01) {
                fifo.position(fifo.position() + 4);
            }

            // Retrieve the entire message now we know the length
            try {
                fifo.get(msg_buffer, 0, recv_header.enc_len);
            } catch (BufferUnderflowException e) {
                // rewind so we process the header again next time
                AppLog.v("BufferUnderflowException whilst trying to read %d bytes limit = %d, position = %d", recv_header.enc_len, fifo.limit(), fifo.position());
                fifo.position(fifo.position() - 4);
                break;
            }

            AapMessage msg = AapMessageIncoming.decrypt(recv_header, 0, msg_buffer, mSsl);

            // Decrypt & Process 1 received encrypted message
            if (msg == null) {
                // If error...
                AppLog.e("Error iaap_recv_dec_process: enc_len: %d chan: %d %s flags: %01x msg_type: %d", recv_header.enc_len, recv_header.chan, Channel.name(recv_header.chan), recv_header.flags, recv_header.msg_type);
                continue;
            }

            mHandler.handle(msg);
        }

        // consume
        fifo.compact();
    }

}
