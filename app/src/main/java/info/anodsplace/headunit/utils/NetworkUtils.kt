package info.anodsplace.headunit.utils

import android.content.Context
import android.net.wifi.WifiManager

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

object NetworkUtils {
    fun getWifiIpAddress(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo.ipAddress
    }

    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    fun intToInetAddress(hostAddress: Int): InetAddress {
        val addressBytes = byteArrayOf((0xff and hostAddress).toByte(), (0xff and (hostAddress shr 8)).toByte(), (0xff and (hostAddress shr 16)).toByte(), (0xff and (hostAddress shr 24)).toByte())

        try {
            return InetAddress.getByAddress(addressBytes)
        } catch (e: UnknownHostException) {
            AppLog.e(e)
            throw AssertionError()
        }
    }
}
