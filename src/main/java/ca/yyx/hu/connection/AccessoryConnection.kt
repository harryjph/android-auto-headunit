package ca.yyx.hu.connection

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */

interface AccessoryConnection {

    interface Listener {
        fun onConnectionResult(success: Boolean)
    }

    val isSingleMessage: Boolean
    fun send(buf: ByteArray, length: Int, timeout: Int): Int
    fun recv(buf: ByteArray, length: Int, timeout: Int): Int
    val isConnected: Boolean
    fun connect(listener: Listener)
    fun disconnect()
}
