package info.anodsplace.headunit.connection


import java.io.BufferedInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

import info.anodsplace.headunit.utils.AppLog
class SocketAccessoryConnection(private val mIp: String) : AccessoryConnection {
    private val mSocket: Socket = Socket()
    private var mInputStream: BufferedInputStream? = null

    override val isSingleMessage: Boolean
        get() = true

    override fun write(buf: ByteArray, offset: Int, length: Int, timeout: Int): Int {
        return try {
            mSocket.getOutputStream().write(buf, offset, length)
            mSocket.getOutputStream().flush()
            length
        } catch (e: IOException) {
            AppLog.e(e)
            -1
        }
    }

    override fun read(buf: ByteArray, offset: Int, length: Int, timeout: Int): Int {
        return try {
            mSocket.soTimeout = timeout
            mInputStream!!.read(buf, offset, length)
        } catch (e: IOException) {
            -1
        }
    }

    override val isConnected: Boolean
        get() = mSocket.isConnected

    override fun connect(listener: AccessoryConnection.Listener) {
        Thread(Runnable {
            try {
                mSocket.tcpNoDelay = true
                mSocket.reuseAddress = true
                mSocket.connect(InetSocketAddress(mIp, 5277), 3000)
                mInputStream = BufferedInputStream(mSocket.getInputStream(), DEF_BUFFER_LENGTH)
                listener.onConnectionResult(mSocket.isConnected)
            } catch (e: IOException) {
                AppLog.e(e)
                listener.onConnectionResult(false)
            }
        }, "socket_connect").start()
    }

    override fun disconnect() {
        if (mSocket.isConnected) {
            try {
                mSocket.close()
            } catch (e: IOException) {
                AppLog.e(e)
                //catch logic
            }

        }
        mInputStream = null
    }

    companion object {
        private const val DEF_BUFFER_LENGTH = 131080
    }
}
