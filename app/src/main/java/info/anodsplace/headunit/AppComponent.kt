package info.anodsplace.headunit

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import info.anodsplace.headunit.aap.AapTransport
import info.anodsplace.headunit.decoder.AudioDecoder
import info.anodsplace.headunit.decoder.VideoDecoder
import info.anodsplace.headunit.utils.Settings

/**
 * @author algavris
 * @date 23/06/2017
 */
class AppComponent(private val app: App) {

    private var _transport: AapTransport? = null
    val transport: AapTransport
        get() {
            if (_transport == null) {
               _transport = AapTransport(audioDecoder, videoDecoder, audioManager, settings, app)
            }
            return _transport!!
        }

    val settings = Settings(app)
    val videoDecoder = VideoDecoder()
    val audioDecoder = AudioDecoder()

    fun resetTransport() {
        _transport?.quit()
        _transport = null
    }

    private val audioManager: AudioManager
        get() = app.getSystemService(Application.AUDIO_SERVICE) as AudioManager

    val localBroadcastManager = LocalBroadcastManager.getInstance(app)
}
