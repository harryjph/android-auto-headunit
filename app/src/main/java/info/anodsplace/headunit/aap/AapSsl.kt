package info.anodsplace.headunit.aap

internal interface AapSsl { // TODO replace this
    fun prepare(): Int
    fun handshakeRead(): ByteArrayWithLimit?
    fun handshakeWrite(certificate: ByteArray)
    fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit?
    fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit?
}
