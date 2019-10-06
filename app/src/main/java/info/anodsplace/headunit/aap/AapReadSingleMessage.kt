package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.connection.AccessoryConnection
import info.anodsplace.headunit.utils.AppLog


internal class AapReadSingleMessage(connection: AccessoryConnection, ssl: AapSsl, handler: AapMessageHandler)
    : AapRead.Base(connection, ssl, handler) {

    private val recv_header = AapMessageIncoming.EncryptedHeader()
    private val msg_buffer = ByteArray(65535) // unsigned short max

    override fun doRead(connection: AccessoryConnection): Int {
        val header_size = connection.read(recv_header.buf, 0, recv_header.buf.size, 150)
        if (header_size != AapMessageIncoming.EncryptedHeader.SIZE) {
            AppLog.d { "Header: read $header_size" }
            return -1
        }

        recv_header.decode()

        val msg_size = connection.read(msg_buffer, 0, recv_header.enc_len, 150)
        if (msg_size != recv_header.enc_len) {
            AppLog.d { "Message: read $msg_size" }
            return -1
        }

        try {
            val msg = AapMessageIncoming.decrypt(recv_header, 0, msg_buffer, ssl)

            // Decrypt & Process 1 received encrypted message
            if (msg == null) {
                AppLog.e { "Error iaap_recv_dec_process: enc_len: ${recv_header.enc_len} chan: ${recv_header.chan} ${Channel.name(recv_header.chan)} flags: ${recv_header.flags.toString(16)} msg_type: ${recv_header.msg_type}" }
                return -1
            }

            handler.handle(msg)
            return 0
        } catch (e: AapMessageHandler.HandleException) {
            return -1
        }
    }
}
