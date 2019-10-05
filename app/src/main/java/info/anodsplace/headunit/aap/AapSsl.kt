package info.anodsplace.headunit.aap
internal interface AapSsl { // TODO replace this
    fun prepare(): Int
    fun handshake()
    fun bioRead(): ByteArrayWithLimit?
    fun bioWrite(start: Int, length: Int, buffer: ByteArray): Int
    fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit?
    fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit?
}
