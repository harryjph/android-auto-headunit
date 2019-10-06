package info.anodsplace.headunit.aap

import info.anodsplace.headunit.aap.protocol.messages.Messages
import info.anodsplace.headunit.utils.AppLog

internal object AapSslNative : AapSsl {
    init {
        System.loadLibrary("crypto")
        System.loadLibrary("ssl")
        System.loadLibrary("hu_jni")
    }

    private external fun native_ssl_prepare(): Int
    private external fun native_ssl_do_handshake(): Int
    private external fun native_ssl_bio_read(offset: Int, res_len: Int, res_buf: ByteArray): Int
    private external fun native_ssl_bio_write(offset: Int, msg_len: Int, msg_buf: ByteArray): Int
    private external fun native_ssl_read(offset: Int, res_len: Int, res_buf: ByteArray): Int
    private external fun native_ssl_write(offset: Int, msg_len: Int, msg_buf: ByteArray): Int

    private val bio_read = ByteArray(Messages.DEF_BUFFER_LENGTH)
    private val enc_buf = ByteArray(Messages.DEF_BUFFER_LENGTH)
    private val dec_buf = ByteArray(Messages.DEF_BUFFER_LENGTH)

    override fun prepare() {
        val ret = native_ssl_prepare()
        if (ret < 0) {
            AppLog.e { "SSL prepare failed: $ret" }
            throw IllegalStateException("SSL Prepare Failed: $ret")
        }
    }

    private fun bioRead(): ByteArray {
        val size = native_ssl_bio_read(0, Messages.DEF_BUFFER_LENGTH, bio_read)
        AppLog.i { "SSL BIO read: $size" }
        if (size <= 0) {
            throw IllegalArgumentException("Return <= 0")
        }
        return ByteArrayWithLimit(bio_read, size).copy()
    }

    private fun bioWrite(start: Int, length: Int, buffer: ByteArray): Int {
        return native_ssl_bio_write(start, length, buffer)
    }

    override fun handshakeRead(): ByteArray {
        native_ssl_do_handshake()
        return bioRead()
    }

    override fun handshakeWrite(handshakeData: ByteArray) {
        bioWrite(0, handshakeData.size, handshakeData)
    }

    override fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArray {
        val bytes_written = native_ssl_bio_write(start, length, buffer)
        // Write encrypted to SSL input BIO
        if (bytes_written <= 0) {
            AppLog.e { "BIO_write() bytes_written: $bytes_written" }
            throw IllegalArgumentException("Return <= 0")
        }

        val bytes_read = native_ssl_read(0, Messages.DEF_BUFFER_LENGTH, dec_buf)
        // Read decrypted to decrypted rx buf
        if (bytes_read <= 0) {
            AppLog.e { "SSL_read bytes_read: $bytes_read" }
            throw IllegalArgumentException("Return <= 0")
        }
        return ByteArrayWithLimit(dec_buf, bytes_read).copy()
    }

    override fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArray {
        val bytes_written = native_ssl_write(offset, length, buffer)
        // Write plaintext to SSL
        if (bytes_written <= 0) {
            AppLog.e { "SSL_write() bytes_written: $bytes_written" }
            throw IllegalArgumentException("Return <= 0")
        }

        if (bytes_written != length) {
            AppLog.e { "SSL Write len: $length bytes_written: $bytes_written" }
        }

        AppLog.d { "SSL Write len: $length bytes_written: $bytes_written" }

        val bytes_read = native_ssl_bio_read(offset, Messages.DEF_BUFFER_LENGTH - offset, enc_buf)
        if (bytes_read <= 0) {
            AppLog.e { "BIO read  bytes_read: $bytes_read" }
            throw IllegalArgumentException("Return <= 0")
        }

        AppLog.d { "BIO read bytes_read: $bytes_read" }

        return ByteArrayWithLimit(enc_buf, bytes_read + offset).copy()
    }

    class ByteArrayWithLimit(val data: ByteArray, var limit: Int) {
        fun copy(): ByteArray {
            val byteArray = ByteArray(limit)
            System.arraycopy(data, 0, byteArray, 0, limit)
            return byteArray
        }
    }
}
