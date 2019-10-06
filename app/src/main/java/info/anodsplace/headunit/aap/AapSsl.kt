package info.anodsplace.headunit.aap

internal interface AapSsl { // TODO replace this
    fun prepare()
    fun handshakeRead(): ByteArray
    fun handshakeWrite(handshakeData: ByteArray)
    fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArray
    fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArray

    companion object {
        val INSTANCE: AapSsl = AapSslImpl
    }
}
