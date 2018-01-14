package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.connection.AccessoryConnection
import info.anodsplace.headunit.utils.AppLog

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

internal class AapReadSingleMessage(connection: AccessoryConnection, ssl: AapSsl, handler: AapMessageHandler)
    : AapRead.Base(connection, ssl, handler) {

    private val recv_header = AapMessageIncoming.EncryptedHeader()
    private val msg_buffer = ByteArray(65535) // unsigned short max

    override fun doRead(connection: AccessoryConnection): Int {
        val header_size = connection.recv(recv_header.buf, recv_header.buf.size, 150)
        if (header_size != AapMessageIncoming.EncryptedHeader.SIZE) {
            AppLog.v("Header: recv %d", header_size)
            return -1
        }

        recv_header.decode()

        val msg_size = connection.recv(msg_buffer, recv_header.enc_len, 150)
        if (msg_size != recv_header.enc_len) {
            AppLog.v("Message: recv %d", msg_size)
            return -1
        }

        try {
            val msg = AapMessageIncoming.decrypt(recv_header, 0, msg_buffer, ssl)

            // Decrypt & Process 1 received encrypted message
            if (msg == null) {
                // If error...
                AppLog.e("Error iaap_recv_dec_process: enc_len: %d chan: %d %s flags: %01x msg_type: %d", recv_header.enc_len, recv_header.chan, Channel.name(recv_header.chan), recv_header.flags, recv_header.msg_type)
                return -1
            }

            handler.handle(msg)
            return 0
        } catch (e: AapMessageHandler.HandleException) {
            return -1
        }

    }
}
