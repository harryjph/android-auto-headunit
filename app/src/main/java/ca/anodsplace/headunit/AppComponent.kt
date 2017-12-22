package ca.anodsplace.headunit

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import ca.anodsplace.headunit.aap.AapTransport
import ca.anodsplace.headunit.decoder.AudioDecoder
import ca.anodsplace.headunit.decoder.VideoDecoder
import ca.anodsplace.headunit.main.BackgroundNotification
import ca.anodsplace.headunit.utils.Settings

/**
 * @author algavris
 * @date 23/06/2017
 */
class AppComponent(private val app: App) {

    private var _transport: AapTransport? = null
    val transport: AapTransport
        get() {
            if (_transport == null) {
               _transport = AapTransport(audioDecoder, videoDecoder, audioManager, settings, backgroundNotification, app)
            }
            return _transport!!
        }

    val settings = Settings(app)
    val videoDecoder = VideoDecoder()
    val audioDecoder = AudioDecoder()
    var hasVideoFocus = false

    fun resetTransport() {
        hasVideoFocus = false
        _transport?.quit()
        _transport = null
    }

    val backgroundNotification = BackgroundNotification(app)

    val audioManager: AudioManager
        get() = app.getSystemService(Application.AUDIO_SERVICE) as AudioManager
    val notificationManager: NotificationManager
        get() = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
