package ca.yyx.hu.aap

import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Utils

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

internal class AapMessageIncoming(header: AapMessageIncoming.EncryptedHeader, ba: ByteArrayWithLimit)
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
                AppLog.e("WRONG FLAG: enc_len: %d  chan: %d %s flags: 0x%02x  msg_type: 0x%02x %s",
                        header.enc_len, header.chan, Channel.name(header.chan), header.flags, header.msg_type, MsgType.name(header.msg_type, header.chan))
                return null
            }

            val ba = ssl.decrypt(offset, header.enc_len, buf) ?: return null

            val msg = AapMessageIncoming(header, ba)

            if (AppLog.LOG_VERBOSE) {
                AppLog.d("RECV: ", msg.toString())
            }
            return msg
        }

        fun calcOffset(header: AapMessageIncoming.EncryptedHeader): Int {
            return 2
        }
    }
}
