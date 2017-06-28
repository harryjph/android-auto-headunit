package ca.yyx.hu.aap

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.UiModeManager
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.widget.Toast
import ca.yyx.hu.App
import ca.yyx.hu.R
import ca.yyx.hu.app.RemoteControlReceiver
import ca.yyx.hu.aap.protocol.messages.NightModeEvent
import ca.yyx.hu.connection.AccessoryConnection
import ca.yyx.hu.connection.SocketAccessoryConnection
import ca.yyx.hu.connection.UsbAccessoryConnection
import ca.yyx.hu.connection.UsbReceiver
import ca.yyx.hu.decoder.AudioDecoder
import ca.yyx.hu.location.GpsLocationService
import ca.yyx.hu.utils.*

/**
 * @author algavris
 * *
 * @date 03/06/2016.
 */

class AapService : Service(), UsbReceiver.Listener, AccessoryConnection.Listener {

    private lateinit var mMediaSession: MediaSessionCompat
    private lateinit var mAudioDecoder: AudioDecoder
    private lateinit var mUiModeManager: UiModeManager
    private var mAccessoryConnection: AccessoryConnection? = null
    private lateinit var mUsbReceiver: UsbReceiver
    private lateinit var nightModeReceiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mUiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        mUiModeManager.nightMode = UiModeManager.MODE_NIGHT_AUTO

        mAudioDecoder = App.provide(this).audioDecoder

        mMediaSession = MediaSessionCompat(this, "MediaSession", ComponentName(this, RemoteControlReceiver::class.java), null)
        mMediaSession.setCallback(MediaSessionCallback(this))
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        mUsbReceiver = UsbReceiver(this)
        nightModeReceiver = NightModeReceiver(Settings(this), mUiModeManager)

        val nightModeFilter = IntentFilter()
        nightModeFilter.addAction(Intent.ACTION_TIME_TICK)
        nightModeFilter.addAction(LocalIntent.ACTION_LOCATION_UPDATE)
        registerReceiver(nightModeReceiver, nightModeFilter)
        registerReceiver(mUsbReceiver, UsbReceiver.createFilter())
    }

    override fun onDestroy() {
        super.onDestroy()
        onDisconnect()
        unregisterReceiver(nightModeReceiver)
        unregisterReceiver(mUsbReceiver)
        mUiModeManager.disableCarMode(0)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        mAccessoryConnection = createConnection(intent, this)
        if (mAccessoryConnection == null) {
            AppLog.e("Cannot create connection " + intent)
            stopSelf()
            return START_NOT_STICKY
        }

        mUiModeManager.enableCarMode(0)

        val aapIntent = Intent(this, AapProjectionActivity::class.java)
        aapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val noty = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_aa)
                .setTicker("Headunit is running")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Headunit is running")
                .setContentText("...")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, aapIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(Notification.PRIORITY_HIGH)
                .build()

        startService(GpsLocationService.intent(this))

        mMediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0F)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build())

        mMediaSession.isActive = true

        startForeground(1, noty)

        mAccessoryConnection!!.connect(this)

        return START_STICKY
    }

    override fun onConnectionResult(success: Boolean) {
        if (success) {
            reset()
            App.provide(this).transport.connectAndStart(mAccessoryConnection!!)
        } else {
            AppLog.e("Cannot connect to device")
            Toast.makeText(this, "Cannot connect to the device", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun onDisconnect() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(LocalIntent.ACTION_DISCONNECT))
        reset()
        mAccessoryConnection!!.disconnect()
        mAccessoryConnection = null
    }

    private fun reset() {
        App.provide(this).resetTransport()
        mAudioDecoder.stop()
        App.provide(this).videoDecoder.stop("AapService::reset")
    }


    private class MediaSessionCallback internal constructor(private val mContext: Context) : MediaSessionCompat.Callback() {

        override fun onCommand(command: String, extras: Bundle, cb: ResultReceiver) {
            AppLog.i(command)
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            AppLog.i(action)
        }

        override fun onSkipToNext() {
            AppLog.i("onSkipToNext")

            App.provide(mContext).transport.sendButton(KeyEvent.KEYCODE_MEDIA_NEXT, true)
            Utils.ms_sleep(10)
            App.provide(mContext).transport.sendButton(KeyEvent.KEYCODE_MEDIA_NEXT, false)
        }

        override fun onSkipToPrevious() {
            AppLog.i("onSkipToPrevious")

            App.provide(mContext).transport.sendButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS, true)
            Utils.ms_sleep(10)
            App.provide(mContext).transport.sendButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS, false)
        }

        override fun onPlay() {
            AppLog.i("PLAY")

            App.provide(mContext).transport.sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true)
            Utils.ms_sleep(10)
            App.provide(mContext).transport.sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            AppLog.i(mediaButtonEvent.toString())
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    override fun onUsbDetach(device: UsbDevice) {
        if (mAccessoryConnection is UsbAccessoryConnection) {
            if ((mAccessoryConnection as UsbAccessoryConnection).isDeviceRunning(device)) {
                stopSelf()
            }
        }
    }

    override fun onUsbAttach(device: UsbDevice) {

    }

    override fun onUsbPermission(granted: Boolean, connect: Boolean, device: UsbDevice) {

    }

    private class NightModeReceiver(private val settings: Settings, private val mUiModeManager: UiModeManager) : BroadcastReceiver() {
        private var nightMode = NightMode(settings, false)
        private var initialized = false
        private var lastValue = false

        override fun onReceive(context: Context, intent: Intent) {

            if (!nightMode.hasGPSLocation && intent.action == LocalIntent.ACTION_LOCATION_UPDATE)
            {
                nightMode = NightMode(settings, true)
            }

            val isCurrent = nightMode.current
            if (!initialized || lastValue != isCurrent) {
                lastValue = isCurrent
                AppLog.i(nightMode.toString())
                initialized = App.provide(context).transport.send(NightModeEvent(isCurrent))
                if (initialized)
                {
                    mUiModeManager.nightMode = if (isCurrent) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
                }
            }
        }
    }

    companion object {
        private const val TYPE_USB = 1
        private const val TYPE_WIFI = 2
        const val EXTRA_CONNECTION_TYPE = "extra_connection_type"
        const val EXTRA_IP = "extra_ip"

        fun createIntent(device: UsbDevice, context: Context): Intent {
            val intent = Intent(context, AapService::class.java)
            intent.putExtra(UsbManager.EXTRA_DEVICE, device)
            intent.putExtra(EXTRA_CONNECTION_TYPE, TYPE_USB)
            return intent
        }

        fun createIntent(ip: String, context: Context): Intent {
            val intent = Intent(context, AapService::class.java)
            intent.putExtra(EXTRA_IP, ip)
            intent.putExtra(EXTRA_CONNECTION_TYPE, TYPE_WIFI)
            return intent
        }

        private fun createConnection(intent: Intent, context: Context): AccessoryConnection? {

            val connectionType = intent.getIntExtra(EXTRA_CONNECTION_TYPE, 0)

            if (connectionType == TYPE_USB) {
                val device = LocalIntent.extractDevice(intent)
                if (device == null) {
                    AppLog.e("No device in " + intent)
                    return null
                }
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                return UsbAccessoryConnection(usbManager, device)
            } else if (connectionType == TYPE_WIFI) {
                val ip = intent.getStringExtra(EXTRA_IP)
                return SocketAccessoryConnection(ip)
            }

            return null
        }
    }
}
