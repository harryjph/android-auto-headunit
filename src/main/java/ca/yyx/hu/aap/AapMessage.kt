package ca.yyx.hu.aap


import com.google.protobuf.nano.MessageNano

import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.utils.Utils
import java.util.*

/**
 * @author algavris
 * *
 * @date 04/10/2016.
 */
open class AapMessage(
        internal val channel: Int,
        internal val flags: Byte,
        internal val type: Int,
        internal val dataOffset: Int,
        internal val size: Int,
        val data: ByteArray) {

    @JvmOverloads constructor(channel: Int, type: Int, proto: MessageNano, buf: ByteArray = ByteArray(size(proto)))
            : this(channel, flags(channel, type), type, HEADER_SIZE + MsgType.SIZE, size(proto), buf) {

        val msgType = this.type
        this.data[0] = channel.toByte()
        this.data[1] = flags
        Utils.intToBytes(proto.getSerializedSize() + MsgType.SIZE, 2, this.data)
        this.data[4] = (msgType shr 8).toByte()
        this.data[5] = (msgType and 0xFF).toByte()
        MessageNano.toByteArray(proto, this.data, HEADER_SIZE + MsgType.SIZE, proto.getSerializedSize())
    }

    val isAudio: Boolean
        get() = Channel.isAudio(this.channel)

    val isVideo: Boolean
        get() = this.channel == Channel.ID_VID

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(Channel.name(channel))
        sb.append(' ')
        sb.append(MsgType.name(type, channel))
        sb.append('\n')

        AapDump.logHex("", 0, data, this.size, sb)

        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        val msg = other as AapMessage

        if (msg.channel != this.channel) {
            return false
        }
        if (msg.flags != this.flags) {
            return false
        }
        if (msg.type != this.type) {
            return false
        }
        if (msg.size != this.size || msg.data.size < this.size) {
            return false
        }
        if (msg.dataOffset != this.dataOffset) {
            return false
        }
        for (i in 0..this.size - 1) {
            if (msg.data[i] != this.data[i]) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int{
        var result = channel
        result = 31 * result + flags
        result = 31 * result + type
        result = 31 * result + dataOffset
        result = 31 * result + size
 //       result = 31 * result + Arrays.hashCode(data)
        return result
    }

    companion object {
        internal val HEADER_SIZE = 4

        private fun size(proto: MessageNano): Int {
            return proto.getSerializedSize() + MsgType.SIZE + HEADER_SIZE
        }

        private fun flags(channel: Int, type: Int): Byte {
            var flags: Byte = 0x0b
            if (channel != Channel.ID_CTR && MsgType.isControl(type)) {
                // Set Control Flag (On non-control channels, indicates generic/"control type" messages
                flags = 0x0f
            }
            return flags
        }
    }
}