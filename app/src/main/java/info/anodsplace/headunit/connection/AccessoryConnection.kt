package info.anodsplace.headunit.connection

import java.io.InputStream
import java.io.OutputStream

interface AccessoryConnection {
    interface Listener {
        fun onConnectionResult(success: Boolean)
    }

    val isSingleMessage: Boolean
    val isConnected: Boolean
    fun connect(listener: Listener)
    fun disconnect()
    fun read(buf: ByteArray, offset: Int, length: Int, timeout: Int): Int
    fun write(buf: ByteArray, offset: Int, length: Int, timeout: Int): Int

    fun read(buf: ByteArray): Int {
        return read(buf, 0, buf.size, DEFAULT_TIMEOUT)
    }

    fun write(buf: ByteArray): Int {
        return write(buf, 0, buf.size, DEFAULT_TIMEOUT)
    }

    fun read(buf: ByteArray, offset: Int, length: Int): Int {
        return read(buf, offset, length, DEFAULT_TIMEOUT)
    }

    fun write(buf: ByteArray, offset: Int, length: Int): Int {
        return write(buf, offset, length, DEFAULT_TIMEOUT)
    }

    fun asInputStream(): InputStream { // TODO override close?
        return object : InputStream() {
            override fun read(): Int {
                val b = ByteArray(1)
                return this@AccessoryConnection.read(b, 0, 1, DEFAULT_TIMEOUT)
            }

            override fun read(b: ByteArray): Int {
                return this@AccessoryConnection.read(b, 0, b.size)
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return this@AccessoryConnection.read(b, off, len)
            }
        }
    }

    fun asOutputStream(): OutputStream { // TODO override close?
        return object : OutputStream() {
            override fun write(b: Int) {
                this@AccessoryConnection.write(byteArrayOf(b.toByte()), 0, 1)
            }

            override fun write(b: ByteArray) {
                this@AccessoryConnection.write(b, 0, b.size)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                this@AccessoryConnection.write(b, off, len)
            }
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT = 150 // ms
        const val CONNECT_TIMEOUT = 1000 // ms
    }
}
