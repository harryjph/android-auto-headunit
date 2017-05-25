package ca.yyx.hu.aap

import org.junit.Test

import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.connection.AccessoryConnection
import ca.yyx.hu.utils.AppLog

import org.junit.Assert.assertEquals

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class AapReadTest {

    internal inner class FakeConnection : AccessoryConnection {
        var data = ByteArray(0)

        override val isSingleMessage: Boolean
            get() = false

        override fun send(buf: ByteArray, length: Int, timeout: Int): Int {
            return 0
        }

        override fun recv(buf: ByteArray, length: Int, timeout: Int): Int {
            System.arraycopy(this.data, 0, buf, 0, this.data.size)
            return this.data.size
        }

        override val isConnected: Boolean
            get() = false

        override fun connect(listener: AccessoryConnection.Listener) {

        }

        override fun disconnect() {

        }
    }

    internal inner class FakeHandler : AapMessageHandler {
        var msg = arrayOfNulls<AapMessage>(10)
        var idx = 0
        @Throws(AapMessageHandler.HandleException::class)
        override fun handle(message: AapMessage) {
            this.msg[this.idx] = message
            this.idx++
        }
    }

    internal inner class FakeSsl : AapSsl {

        override fun prepare(): Int {
            return 0
        }

        override fun handshake() {

        }

        override fun bioRead(): ByteArrayWithLimit? {
            return null
        }

        override fun bioWrite(start: Int, length: Int, buffer: ByteArray): Int {
            return 0
        }

        override fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit? {
            val result = ByteArray(length)
            System.arraycopy(buffer, start, result, 0, length)
            return ByteArrayWithLimit(result, length)
        }

        override fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit? {
            return null
        }
    }

    @Test
    fun readMultipleMessages() {
        AppLog.LOGGER = AppLog.Logger.StdOut()
        val conn = FakeConnection()
        val ssl = FakeSsl()
        val handler = FakeHandler()
        val poll = AapReadMultipleMessages(conn, ssl, handler)

        conn.data = byteArrayOf(Channel.ID_CTR.toByte(), 0x8, 0, 6, 0, MsgType.Control.PINGRESPONSE.toByte(), b('M'), b('S'), b('G'), b('1'), Channel.ID_VID.toByte(), 0x9, 0, 7, 0, 0, 0, 1, 0, MsgType.Control.MEDIADATA.toByte(), b('V'), b('I'), b('D'), b('E'), b('O'), Channel.ID_CTR.toByte(), 0x8, 0, 6, 0, MsgType.Control.PINGRESPONSE.toByte(), b('M'))

        var res = poll.read()
        assertEquals(res.toLong(), 0)
        assertEquals(handler.idx.toLong(), 2)
        assertEquals(handler.msg[0], AapMessage(Channel.ID_CTR, 0x8.toByte(), MsgType.Control.PINGRESPONSE, 2, 6, byteArrayOf(0, MsgType.Control.PINGRESPONSE.toByte(), b('M'), b('S'), b('G'), b('1'))))
        assertEquals(handler.msg[1], AapMessage(Channel.ID_VID, 0x9.toByte(), MsgType.Control.MEDIADATA, 2, 7, byteArrayOf(0, MsgType.Control.MEDIADATA.toByte(), b('V'), b('I'), b('D'), b('E'), b('O'))))

        conn.data = byteArrayOf(b('S'), b('G'), b('2'), Channel.ID_CTR.toByte(), 0x8)

        res = poll.read()
        assertEquals(res.toLong(), 0)
        assertEquals(handler.idx.toLong(), 3)
        assertEquals(handler.msg[2], AapMessage(Channel.ID_CTR, 0x8.toByte(), MsgType.Control.PINGRESPONSE, 2, 6, byteArrayOf(0, MsgType.Control.PINGRESPONSE.toByte(), b('M'), b('S'), b('G'), b('2'))))

        conn.data = byteArrayOf(0, 6, 0, MsgType.Control.PINGRESPONSE.toByte(), b('M'), b('S'), b('G'), b('3'))

        res = poll.read()
        assertEquals(res.toLong(), 0)
        assertEquals(handler.idx.toLong(), 4)
        assertEquals(handler.msg[3], AapMessage(Channel.ID_CTR, 0x8.toByte(), MsgType.Control.PINGRESPONSE, 2, 6, byteArrayOf(0, MsgType.Control.PINGRESPONSE.toByte(), b('M'), b('S'), b('G'), b('3'))))
    }
}

private fun b(ch: Char): Byte { return ch.toByte() }