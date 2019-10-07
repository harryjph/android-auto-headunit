package info.anodsplace.headunit.aap


import com.google.protobuf.CodedOutputStream
import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.MsgType
import info.anodsplace.headunit.utils.bytesToHex

open class AapMessage(
        internal val channel: Int,
        internal val flags: Byte,
        internal val type: Int,
        internal val dataOffset: Int,
        internal val size: Int,
        val data: ByteArray) {

    constructor(channel: Int, type: Int, proto: MessageLite, buf: ByteArray = ByteArray(size(proto)))
            : this(channel, flags(channel, type), type, HEADER_SIZE + MsgType.SIZE, size(proto), buf) {
        val msgType = this.type
        this.data[0] = channel.toByte()
        this.data[1] = flags
        Utils.intToBytes(proto.serializedSize + MsgType.SIZE, 2, this.data)
        this.data[4] = (msgType shr 8).toByte()
        this.data[5] = (msgType and 0xFF).toByte()

        toByteArray(proto, this.data, HEADER_SIZE + MsgType.SIZE, proto.serializedSize)
    }

    val isAudio: Boolean
        get() = Channel.isAudio(this.channel)

    val isVideo: Boolean
        get() = this.channel == Channel.ID_VID

    internal fun <T : MessageLite.Builder> parse(builder: T): T {
        builder.mergeFrom(this.data, this.dataOffset, this.size - this.dataOffset)
        return builder
    }

    private fun toByteArray(msg: MessageLite, data: ByteArray, offset: Int, length: Int) {
        val output = CodedOutputStream.newInstance(data, offset, length)
        msg.writeTo(output)
        output.checkNoSpaceLeft()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AapMessage) return false

        if (channel != other.channel) return false
        if (flags != other.flags) return false
        if (type != other.type) return false
        if (dataOffset != other.dataOffset) return false
        if (size != other.size) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = channel
        result = 31 * result + flags
        result = 31 * result + type
        result = 31 * result + dataOffset
        result = 31 * result + size
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "AapMessage(channel=$channel, flags=$flags, type=$type, dataOffset=$dataOffset, size=$size, data=${bytesToHex(data)})"
    }


    companion object {
        const val HEADER_SIZE = 4

        private fun size(proto: MessageLite): Int {
            return proto.serializedSize + MsgType.SIZE + HEADER_SIZE
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