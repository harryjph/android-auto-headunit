package info.anodsplace.headunit.aap

import android.util.Base64
import java.io.ByteArrayInputStream
import java.net.Socket
import java.security.Key
import java.security.KeyFactory
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509KeyManager

class SingleKeyKeyManager(private val delegate: X509KeyManager): X509ExtendedKeyManager() {
    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        return delegate.getClientAliases(keyType, issuers)
    }

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        return delegate.getServerAliases(keyType, issuers)
    }

    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String {
        return delegate.chooseServerAlias(keyType, issuers, socket)
    }

    override fun getCertificateChain(alias: String?): Array<X509Certificate> {
        return delegate.getCertificateChain(alias)
    }

    override fun getPrivateKey(alias: String?): PrivateKey {
        return delegate.getPrivateKey(alias)
    }

    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String {
        return DEFAULT_ALIAS
    }

    override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String {
        return DEFAULT_ALIAS
    }

    companion object {
        const val DEFAULT_ALIAS = "myAlias"
    }
}
