package info.anodsplace.headunit.aap

object Utils {
    fun putTime(offset: Int, arr: ByteArray, time: Long) {
        var time = time
        for (ctr in 7 downTo 0) { // Fill 8 bytes backwards
            arr[offset + ctr] = (time and 0xFF).toByte()
            time = time shr 8
        }
    }

    fun intToBytes(value: Int, offset: Int, buf: ByteArray) {
        buf[offset] = (value / 256).toByte() // Encode length of following data:
        buf[offset + 1] = (value % 256).toByte()
    }

    // TODO
    fun bytesToInt(buf: ByteArray, idx: Int, isShort: Boolean): Int {
        return if (isShort) { (buf[idx].toInt() and 0xFF shl 8) + (buf[idx + 1].toInt() and 0xFF)
        } else (buf[idx].toInt() and 0xFF shl 24) + (buf[idx + 1].toInt() and 0xFF shl 16) + (buf[idx + 2].toInt() and 0xFF shl 8) + (buf[idx + 3].toInt() and 0xFF)
    }

    fun getAccessoryVersion(buffer: ByteArray): Int {
        return buffer[1].toInt() shl 8 or buffer[0].toInt()
    }
}

fun Short.toHexString(): String {
    val hex = this.toString(16)
    return when (hex.length) {
        0 -> "0000"
        1 -> "000$hex"
        2 -> "00$hex"
        3 -> "0$hex"
        else -> hex
    }
}
