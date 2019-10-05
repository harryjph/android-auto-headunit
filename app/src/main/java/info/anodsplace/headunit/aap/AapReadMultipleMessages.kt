package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.messages.Messages
import info.anodsplace.headunit.connection.AccessoryConnection
import info.anodsplace.headunit.utils.AppLog
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer


internal class AapReadMultipleMessages(
        connection: AccessoryConnection,
        ssl: AapSsl,
        handler: AapMessageHandler)
    : AapRead.Base(connection, ssl, handler) {

    private val fifo = ByteBuffer.allocate(Messages.DEF_BUFFER_LENGTH * 2)
    private val recv_buffer = ByteArray(Messages.DEF_BUFFER_LENGTH)
    private val recv_header = AapMessageIncoming.EncryptedHeader()
    private val msg_buffer = ByteArray(65535) // unsigned short max

    override fun doRead(connection: AccessoryConnection): Int {

        val size = connection.recv(recv_buffer, recv_buffer.size, 150)
        if (size <= 0) {
            //            AppLog.v("recv %d", size);
            return 0
        }
        try {
            processBulk(size, recv_buffer)
        } catch (e: AapMessageHandler.HandleException) {
            return -1
        }

        return 0
    }

    @Throws(AapMessageHandler.HandleException::class)
    private fun processBulk(size: Int, buf: ByteArray) {

        fifo.put(buf, 0, size)
        fifo.flip()

        while (fifo.hasRemaining()) {

            fifo.mark()
            // Parse the header
            try {
                fifo.get(recv_header.buf, 0, recv_header.buf.size)
            } catch (e: BufferUnderflowException) {
                // we'll come back later for more data
                AppLog.e { "BufferUnderflowException whilst trying to read 4 bytes capacity = ${fifo.capacity()}, position = ${fifo.position()}" }
                fifo.reset()
                break
            }

            recv_header.decode()

            if (recv_header.flags == 0x09) {
                val size_buf = ByteArray(4)
                fifo.get(size_buf, 0, 4)
                // If First fragment Video...
                // (Packet is encrypted so we can't get the real msg_type or check for 0, 0, 0, 1)
                val total_size = Utils.bytesToInt(size_buf, 0, false)
                AppLog.d { "First fragment total_size: $total_size" }
            }

            // Retrieve the entire message now we know the length
            try {
                fifo.get(msg_buffer, 0, recv_header.enc_len)
            } catch (e: BufferUnderflowException) {
                // rewind so we process the header again next time
                AppLog.e { "BufferUnderflowException whilst trying to read ${recv_header.enc_len} bytes limit = ${fifo.limit()}, position = ${fifo.position()}" }
                fifo.reset()
                break
            }

            val msg = AapMessageIncoming.decrypt(recv_header, 0, msg_buffer, ssl)

            // Decrypt & Process 1 received encrypted message
            if (msg == null) {
                AppLog.e { "enc_len: ${recv_header.enc_len} chan: ${recv_header.chan} ${Channel.name(recv_header.chan)} flags: ${recv_header.flags.toString(16)} msg_type: ${recv_header.msg_type}" }
                break
            }

            handler.handle(msg)
        }

        // consume
        fifo.compact()
    }

}
