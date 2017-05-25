package ca.yyx.hu

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.support.v4.content.LocalBroadcastManager
import ca.yyx.hu.aap.AapProjectionActivity
import ca.yyx.hu.aap.AapTransport
import ca.yyx.hu.aap.protocol.messages.LocationUpdateEvent
import ca.yyx.hu.decoder.AudioDecoder
import ca.yyx.hu.decoder.VideoDecoder
import ca.yyx.hu.utils.LocalIntent
import ca.yyx.hu.utils.LocalIntent.extractLocation
import ca.yyx.hu.utils.Settings

/**
 * @author algavris
 * *
 * @date 30/05/2016.
 */

class App : Application(), AapTransport.Listener {

    private lateinit var mVideoDecoder: VideoDecoder
    private lateinit var mAudioDecoder: AudioDecoder
    private var mTransport: AapTransport? = null
    private lateinit var mSettings: Settings

    private val mLocationUpdatesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location = extractLocation(intent)
            App.get(context).transport().send(LocationUpdateEvent(location))

            if (location.latitude != 0.0 && location.longitude != 0.0) {
                mSettings.lastKnownLocation = location
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mAudioDecoder = AudioDecoder()
        mVideoDecoder = VideoDecoder()
        mSettings = Settings(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationUpdatesReceiver, LocalIntent.FILTER_LOCATION_UPDATE)

    }

    fun transport(): AapTransport {
        if (mTransport == null) {
            mTransport = AapTransport(mAudioDecoder, mVideoDecoder, getSystemService(AUDIO_SERVICE) as AudioManager, mSettings, this)
        }
        return mTransport!!
    }

    fun audioDecoder(): AudioDecoder {
        return mAudioDecoder
    }

    fun videoDecoder(): VideoDecoder {
        return mVideoDecoder
    }

    fun reset() {
        mTransport = null
    }

    override fun gainVideoFocus() {
        AapProjectionActivity.start(this)
    }

    companion object {
        val IS_LOLLIPOP = Build.VERSION.SDK_INT >= 21

        fun get(context: Context): App {
            return context.applicationContext as App
        }
    }
}
