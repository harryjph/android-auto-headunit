package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.MsgType
import info.anodsplace.headunit.utils.AppLog


internal class AapMessageIncoming(header: EncryptedHeader, ba: ByteArrayWithLimit)
    : AapMessage(header.chan, header.flags.toByte(), Utils.bytesToInt(ba.data, 0, true), calcOffset(header), ba.limit, ba.data) {

    internal class EncryptedHeader {

        var chan: Int = 0
        var flags: Int = 0
        var enc_len: Int = 0
        var msg_type: Int = 0
        var buf = ByteArray(SIZE)

        fun decode() {
            this.chan = buf[0].toInt()
            this.flags = buf[1].toInt()

            // Encoded length of bytes to be decrypted (minus 4/8 byte headers)
            this.enc_len = Utils.bytesToInt(buf, 2, true)
        }

        companion object {
            const val SIZE = 4
        }

    }

    companion object {

        fun decrypt(header: EncryptedHeader, offset: Int, buf: ByteArray, ssl: AapSsl): AapMessage? {
            if (header.flags and 0x08 != 0x08) {
                AppLog.e { "WRONG FLAG: enc_len: ${header.enc_len}  chan: ${header.chan} ${Channel.name(header.chan)} flags: 0x${header.flags.toString(16)}  msg_type: 0x${header.msg_type.toString(16)} ${MsgType.name(header.msg_type, header.chan)}" }
                return null
            }

            val ba = ssl.decrypt(offset, header.enc_len, buf) ?: return null

            val msg = AapMessageIncoming(header, ba)

            AppLog.d { "RECV: $msg" }
            return msg
        }

        fun calcOffset(header: EncryptedHeader): Int {
            return 2
        }
    }
}
