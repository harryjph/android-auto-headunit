package info.anodsplace.headunit.aap

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

object NoCheckTrustManager: X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate>? {
        return null
    }

}