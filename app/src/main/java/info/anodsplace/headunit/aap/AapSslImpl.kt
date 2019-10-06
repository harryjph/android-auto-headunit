package info.anodsplace.headunit.aap

import android.util.Base64
import java.nio.ByteBuffer
import javax.net.ssl.*
import info.anodsplace.headunit.utils.AppLog
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec

class AapSslImpl: AapSsl {
    val sslc: SSLContext
    val engine: SSLEngine
    val cTOs: ByteBuffer
    val rxBuffer: ByteBuffer

    init {
        // Handshake in client mode.
        val sslCtx = SSLContext.getInstance("TLSv1.2")
        sslCtx.init(arrayOf(SingleKeyKeyManager), arrayOf(NoCheckTrustManager), null)

        sslc = sslCtx

        engine = sslc.createSSLEngine()
        engine.useClientMode = true

        val session = engine.session
        val appBufferMax = session.applicationBufferSize
        val netBufferMax = session.packetBufferSize

        cTOs = ByteBuffer.allocate(netBufferMax)
        rxBuffer = ByteBuffer.allocate(appBufferMax + 50)
    }

    override fun prepare(): Int {
        engine.beginHandshake()
        return 0
    }

    private fun runDelegatedTasks(msg: String, result: SSLEngineResult, engine: SSLEngine) {
        AppLog.e { "$msg: ${result.status}/${result.handshakeStatus}, ${result.bytesConsumed()}/${result.bytesProduced()} bytes" }
        if (result.handshakeStatus === SSLEngineResult.HandshakeStatus.NEED_TASK) {
            var runnable: Runnable? = engine.delegatedTask
            while (runnable != null) {
                AppLog.e { "running delegated task..." }
                runnable.run()
                runnable = engine.delegatedTask
            }
            val hsStatus = engine.handshakeStatus
            if (hsStatus === SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw Exception("handshake shouldn't need additional tasks")
            }
            AppLog.e { "new HandshakeStatus: $hsStatus" }
        }
    }

    override fun handshakeRead(): ByteArrayWithLimit? {
        cTOs.clear()
        val result = engine.wrap(ByteBuffer.allocate(0), cTOs)
        runDelegatedTasks("wrap", result, engine)
        return ByteArrayWithLimit(cTOs.array(), result.bytesProduced())
    }

    override fun handshakeWrite(certificate: ByteArray) {
        val data = ByteBuffer.wrap(certificate)
        rxBuffer.clear()
        val result = engine.unwrap(data, rxBuffer)
        runDelegatedTasks("unwrap", result, engine)
    }

    override fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit? {
        val sTOc = ByteBuffer.wrap(buffer, start, length)
        val byteBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
        runDelegatedTasks("decrypt", engine.unwrap(sTOc, byteBuffer), engine)
        byteBuffer.flip()
        return ByteArrayWithLimit(byteBuffer.array(), byteBuffer.limit())
    }

    override fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArrayWithLimit? {
        val byteBuffer = ByteBuffer.wrap(buffer, offset, length)
        val cTOs = ByteBuffer.allocate(engine.session.packetBufferSize)
        runDelegatedTasks("encrypt", engine.wrap(byteBuffer, cTOs), engine)
        cTOs.flip()
        return ByteArrayWithLimit(cTOs.array(), cTOs.limit())
    }
}