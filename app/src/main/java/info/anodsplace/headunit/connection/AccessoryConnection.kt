package info.anodsplace.headunit.connection

interface AccessoryConnection {
    interface Listener {
        fun onConnectionResult(success: Boolean)
    }

    val isSingleMessage: Boolean
    val isConnected: Boolean
    fun connect(listener: Listener)
    fun disconnect()
    fun read(buf: ByteArray, length: Int, timeout: Int): Int
    fun write(buf: ByteArray, length: Int, timeout: Int): Int
}
