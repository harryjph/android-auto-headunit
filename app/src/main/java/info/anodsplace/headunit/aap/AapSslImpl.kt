package info.anodsplace.headunit.aap

import info.anodsplace.headunit.utils.AppLog
import java.nio.ByteBuffer
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult

class AapSslImpl: AapSsl {
    private val sslContext: SSLContext
    private val sslEngine: SSLEngine
    private val txBuffer: ByteBuffer
    private val rxBuffer: ByteBuffer

    init {
        // Handshake in client mode.
        val sslCtx = SSLContext.getInstance("TLSv1.2")
        sslCtx.init(arrayOf(SingleKeyKeyManager), arrayOf(NoCheckTrustManager), null)

        sslContext = sslCtx

        sslEngine = sslContext.createSSLEngine()
        sslEngine.useClientMode = true

        val session = sslEngine.session
        val appBufferMax = session.applicationBufferSize
        val netBufferMax = session.packetBufferSize

        txBuffer = ByteBuffer.allocate(netBufferMax)
        rxBuffer = ByteBuffer.allocate(appBufferMax + 50)
    }

    override fun prepare(): Int {
        sslEngine.beginHandshake()
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

    override fun handshakeRead(): ByteArray {
        txBuffer.clear()
        val result = sslEngine.wrap(emptyArray(), txBuffer)
        runDelegatedTasks("wrap", result, sslEngine)
        return txBuffer.array().copyOfRange(0, result.bytesProduced())
    }

    override fun handshakeWrite(handshakeData: ByteArray) {
        rxBuffer.clear()
        val data = ByteBuffer.wrap(handshakeData)
        val result = sslEngine.unwrap(data, rxBuffer)
        runDelegatedTasks("unwrap", result, sslEngine)
    }

    override fun decrypt(start: Int, length: Int, buffer: ByteArray): ByteArray {
        rxBuffer.clear()
        val encrypted = ByteBuffer.wrap(buffer, start, length)
        val result = sslEngine.unwrap(encrypted, rxBuffer)
        runDelegatedTasks("decrypt", result, sslEngine)
        return rxBuffer.array().copyOfRange(0, result.bytesProduced())
    }

    override fun encrypt(offset: Int, length: Int, buffer: ByteArray): ByteArray {
        txBuffer.clear()
        val byteBuffer = ByteBuffer.wrap(buffer, offset, length)
        val result = sslEngine.wrap(byteBuffer, txBuffer)
        runDelegatedTasks("encrypt", result, sslEngine)
        return txBuffer.array().copyOfRange(0, result.bytesProduced())
    }
}
