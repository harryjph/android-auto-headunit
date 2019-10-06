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
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null)
        val cert = certificate
        ks.setCertificateEntry(SingleKeyKeyManager.DEFAULT_ALIAS, cert)
        ks.setKeyEntry(SingleKeyKeyManager.DEFAULT_ALIAS, privateKey, charArrayOf(), arrayOf(cert))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, charArrayOf())

        val sslCtx = SSLContext.getInstance("TLSv1.2")

        sslCtx.init(arrayOf(SingleKeyKeyManager(kmf.keyManagers[0] as X509KeyManager)), arrayOf(NoCheckTrustManager), null)

        sslc = sslCtx

        engine = sslc.createSSLEngine()
        engine.useClientMode = true

        val session = engine.getSession()
        val appBufferMax = session.getApplicationBufferSize()
        val netBufferMax = session.getPacketBufferSize()

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

    companion object {

        private const val CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDJTCCAg0CAnZTMA0GCSqGSIb3DQEBCwUAMFsxCzAJBgNVBAYTAlVTMRMwEQYD\n" +
                "VQQIDApDYWxpZm9ybmlhMRYwFAYDVQQHDA1Nb3VudGFpbiBWaWV3MR8wHQYDVQQK\n" +
                "DBZHb29nbGUgQXV0b21vdGl2ZSBMaW5rMB4XDTE0MDcwODIyNDkxOFoXDTQ0MDcw\n" +
                "NzIyNDkxOFowVTELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAkNBMRYwFAYDVQQHDA1N\n" +
                "b3VudGFpbiBWaWV3MSEwHwYDVQQKDBhHb29nbGUtQW5kcm9pZC1SZWZlcmVuY2Uw\n" +
                "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCpqQmvoDW/XsREoj20dRcM\n" +
                "qJGWh8RlUoHB8CpBpsoqV4nAuvNngkyrdpCf1yg0fVAp2Ugj5eOtzbiN6BxoNHpP\n" +
                "giZ64pc+JRlwjmyHpssDaHzP+zHZM7acwMcroNVyynSzpiydEDyx/KPtEz5AsKi7\n" +
                "c7AYYEtnCmAnK/waN1RT5KdZ9f97D9NeF7Ljdk+IKFROJh7Nv/YGiv9GdPZh/ezS\n" +
                "m2qhD3gzdh9PYs2cu0u+N17PYpSYB7vXPcYa/gmIVipIJ5RuMQVBWrCgtfzwKPqb\n" +
                "nJQVykm8LnysK+8RCgmPLN3uhsZx6Whax2TVXb1q68DoiaFPhvMfPr2i/9IKaC69\n" +
                "AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAIpfjQriEtbpUyWLoOOfJsjFN04+ajq9\n" +
                "1XALCPd+2ixWHZIBJiucrrf0H7OgY7eFnNbU0cRqiDZHI8BtvzFxNi/JgXqCmSHR\n" +
                "rlaoIsITfqo8KHwcAMs4qWTeLQmkTXBZYz0M3HwC7N1vOGjAJJN5qENIm1Jq+/3c\n" +
                "fxVg2zhHPKY8qtdgl73YIXb9Xx3WmPCBeRBCKJncj0Rq14uaOjWXRyBgbmdzMXJz\n" +
                "FGPHx3wN04JqGyfPFlDazXExFQwuAryjoYBRdxPxGufeQCp3am4xxI2oxNIzR+4L\n" +
                "nOcDhgU1B7sbkVzbKj5gjdOQAmxnKCfBtUNB63a7yzGPYGPIwlBsm54=\n" +
                "-----END CERTIFICATE-----"
        val certificate: X509Certificate
            get() {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                return certificateFactory.generateCertificate(ByteArrayInputStream(CERTIFICATE.toByteArray())) as X509Certificate
            }

        private const val PRIVKEY = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCpqQmvoDW/XsRE\n" +
                "oj20dRcMqJGWh8RlUoHB8CpBpsoqV4nAuvNngkyrdpCf1yg0fVAp2Ugj5eOtzbiN\n" +
                "6BxoNHpPgiZ64pc+JRlwjmyHpssDaHzP+zHZM7acwMcroNVyynSzpiydEDyx/KPt\n" +
                "Ez5AsKi7c7AYYEtnCmAnK/waN1RT5KdZ9f97D9NeF7Ljdk+IKFROJh7Nv/YGiv9G\n" +
                "dPZh/ezSm2qhD3gzdh9PYs2cu0u+N17PYpSYB7vXPcYa/gmIVipIJ5RuMQVBWrCg\n" +
                "tfzwKPqbnJQVykm8LnysK+8RCgmPLN3uhsZx6Whax2TVXb1q68DoiaFPhvMfPr2i\n" +
                "/9IKaC69AgMBAAECggEAbBoW3963IG6jpA+0PW11+EzYJw/u5ZiCsS3z3s0Fd6E7\n" +
                "VqBIQyXU8FOlpxMSvQ8zqtaVjroGLlIsS88feo4leM+28Qm70I8W/I7jPDPcmxlS\n" +
                "nbqycnDu5EY5IeVi27eAUI+LUbBs3APb900Rl2p4uKfoBkAlC0yjI5J1GcczZhf7\n" +
                "RDh1wGgFWZI+ljiSrfpdiA4XmcZ9c7FlO5+NTotZzYeNx1iZprajV1/dlDy8UWEk\n" +
                "woWtppeGzUf3HHgl8yay62ub2vo5I1Z7Z98Roq8KC1o7k2IXOrHztCl3X03gMwlI\n" +
                "F4WQ6Fx5LZDU9dfaPhzkutekVgbtO9SzHgb3NXCZwQKBgQDcSS/OLll18ssjBwc7\n" +
                "PsdaIFIPlF428Tk8qezEnDmHS6xeztkGnpOlilk9jYSsVUbQmq8MwBSjfMVH95B0\n" +
                "w0yyfOYqjgTocg4lRCoPuBdnuBY/lU1Lws4FoGsGMNFkHWjHzl622mavkJiDzWA+\n" +
                "CORPUllS/DnPKJnZk2n0zZRKaQKBgQDFKqvePMx/a/ayQ09UZYxov0vwRyNkHevm\n" +
                "wEGQjOiHKozWvLqWhCvFtwo+VqHqmCw95cYUpg1GvppB6Lnw2uHgWAWxr3ugDjaR\n" +
                "YSqG/L7FG6FDF+1sPvBuxNpBmto59TI1fBFmU9VBGLDnr1M27qH3KTWlA3lCsovV\n" +
                "6Dbk7D+vNQKBgE6GgFYdS6KyFBu+a6OA84t7LgWDvDoVr3Oil1ZW4mMKZL2/OroT\n" +
                "WUqPkNRSWFMeawn9uhzvc+v7lE/dPk+BNxwBTgMpcTJzRfue2ueTljRQ+Q1daZpy\n" +
                "LQLwdnZUfLAVk752IGlKXYSEJPoHAiHbBZgJIPJmGy1vqbhXxlOP3SbRAoGBAJoA\n" +
                "Q2/5gy0/sdf5FRxxmOM0D+dkWTNY36pDnrJ+LR1uUcVkckUghWQQHRMl7aBkLaJH\n" +
                "N5lnPdV1CN3UHnAPNwBZIFFyJJiWoW6aO3JmNceVVjcmmE7FNlz+qw81GaDNcOMv\n" +
                "vhN0BYyr8Xl1iwTMDXwVFw6FkRBUjz6L+1yBXxjFAoGAJZcU+tEM1+gHPCqHK2bP\n" +
                "kfYOCyEAro4zY/VWXZKHgCoPau8Uc9+vFu2QVMb5kVyLTdyRLQKpooR6f8En6utS\n" +
                "/G15YuqRYqzSTrMBzpRrqIwbgKI9RHNPAvhtVAmXnwsYDPIQ1rrELK6WzTjUySRd\n" +
                "7gyCoq+DlY7ZKDa7FUz05Ek=\n" +
                "-----END PRIVATE KEY-----"
        val privateKey: PrivateKey
            get() {
                val privateKeyContent = PRIVKEY.replace("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.decode(privateKeyContent, Base64.DEFAULT))
                val kf = KeyFactory.getInstance("RSA")
                return kf.generatePrivate(keySpecPKCS8)
            }
    }
}