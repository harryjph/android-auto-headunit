package info.anodsplace.headunit.aap

import info.anodsplace.headunit.connection.AccessoryConnection
import info.anodsplace.headunit.decoder.MicRecorder
import info.anodsplace.headunit.main.BackgroundNotification
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.Settings

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */
internal interface AapRead {
    fun read(): Int

    abstract class Base internal constructor(
            private val mConnection: AccessoryConnection?,
            internal val mSsl: AapSsl,
            internal val mHandler: AapMessageHandler) : AapRead {

        override fun read(): Int {
            if (mConnection == null) {
                AppLog.e("No connection.")
                return -1
            }

            return doRead(mConnection)
        }

        protected abstract fun doRead(connection: AccessoryConnection): Int
    }

    object Factory {
        fun create(connection: AccessoryConnection, transport: AapTransport, recorder: MicRecorder, aapAudio: AapAudio, aapVideo: AapVideo, settings: Settings, notification: BackgroundNotification): AapRead {
            val handler = AapMessageHandlerType(transport, recorder, aapAudio, aapVideo, settings, notification)

            return if (connection.isSingleMessage)
                AapReadSingleMessage(connection, AapSslNative(), handler)
            else
                AapReadMultipleMessages(connection, AapSslNative(), handler)
        }
    }
}
