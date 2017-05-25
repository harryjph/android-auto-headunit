package ca.yyx.hu.aap

/**
 * @author algavris
 * *
 * @date 14/02/2017.
 */
internal interface AapSsl {
    fun prepare(): Int
    fun handshake()
    fun bioRead(): ByteArrayWithLimit?
    fun bioWrite(start: Int, length: Int, buffer: ByteArray): Int
    fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit?
    fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit?
}
