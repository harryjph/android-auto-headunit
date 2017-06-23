package ca.yyx.hu

import android.app.Application
import android.media.AudioManager
import ca.yyx.hu.aap.AapTransport
import ca.yyx.hu.decoder.AudioDecoder
import ca.yyx.hu.decoder.VideoDecoder
import ca.yyx.hu.utils.Settings

/**
 * @author algavris
 * @date 23/06/2017
 */
class AppComponent(private val app: App) {

    private var _transport: AapTransport? = null
    val transport: AapTransport
        get() {
            if (_transport == null) {
               _transport = AapTransport(audioDecoder, videoDecoder, app.getSystemService(Application.AUDIO_SERVICE) as AudioManager, settings, app)
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

}