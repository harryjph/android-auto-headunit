package ca.yyx.hu.connection


import android.net.sip.SipAudioCall
import android.support.annotation.NonNull
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

import ca.yyx.hu.utils.AppLog

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */
class SocketAccessoryConnection(internal val mIp: String) : AccessoryConnection {
    internal val mSocket: Socket = Socket()
    private var mInputStream: BufferedInputStream? = null

    override val isSingleMessage: Boolean
        get() = true

    override fun send(buf: ByteArray, length: Int, timeout: Int): Int {
        try {
            //            mSocket.setSendBufferSize(length);
            mSocket.getOutputStream().write(buf, 0, length)
            mSocket.getOutputStream().flush()
            return length
        } catch (e: IOException) {
            AppLog.e(e)
            return -1
        }

    }

    override fun recv(buf: ByteArray, length: Int, timeout: Int): Int {

        try {
            mSocket.soTimeout = timeout
            return mInputStream!!.read(buf, 0, length)
        } catch (e: IOException) {
            return -1
        }

    }

    override val isConnected: Boolean
        get() = mSocket.isConnected

    override fun connect(listener: AccessoryConnection.Listener) {

        Thread(Runnable {
            try {
                mSocket.tcpNoDelay = true
                mSocket.reuseAddress = true
                //                    mSocket.setReceiveBufferSize(DEF_BUFFER_LENGTH);
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
