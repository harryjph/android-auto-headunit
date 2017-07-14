package ca.yyx.hu.aap

import android.media.AudioManager
import ca.yyx.hu.aap.protocol.AudioConfigs
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.messages.DrivingStatusEvent
import ca.yyx.hu.aap.protocol.messages.ServiceDiscoveryResponse
import ca.yyx.hu.aap.protocol.nano.Protocol
import ca.yyx.hu.decoder.MicRecorder
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Settings
import ca.yyx.hu.utils.Utils
import com.google.protobuf.nano.InvalidProtocolBufferNanoException

/**
 * @author algavris
 * *
 * @date 01/10/2016.
 */

internal class AapControl(
        private val aapTransport: AapTransport,
        private val mMicRecorder: MicRecorder,
        private val aapAudio: AapAudio,
        private val mSettings: Settings) {

    @Throws(InvalidProtocolBufferNanoException::class)
    fun execute(message: AapMessage): Int {

        if (message.type == 7) {
            val request = message.parse(Protocol.ChannelOpenRequest())
            return channel_open_request(request, message.channel)
        }

        when (message.channel) {
            Channel.ID_CTR -> return executeControl(message)
            Channel.ID_INP -> return executeTouch(message)
            Channel.ID_SEN -> return executeSensor(message)
            Channel.ID_VID, Channel.ID_AUD, Channel.ID_AU1, Channel.ID_AU2, Channel.ID_MIC -> return executeMedia(message)
        }
        return 0
    }

    @Throws(InvalidProtocolBufferNanoException::class)
    private fun executeMedia(message: AapMessage): Int {

        when (message.type) {
            MsgType.Media.SETUPREQUEST -> {
                val setupRequest = message.parse(Protocol.MediaSetupRequest())
                return media_sink_setup_request(setupRequest, message.channel)
            }
            MsgType.Media.STARTREQUEST -> {
                val startRequest = message.parse(Protocol.Start())
                return media_start_request(startRequest, message.channel)
            }
            MsgType.Media.STOPREQUEST -> return media_sink_stop_request(message.channel)
            MsgType.Media.VIDEOFOCUSREQUESTNOTIFICATION -> {
                val focusRequest = message.parse(Protocol.VideoFocusRequestNotification())
                AppLog.i("Video Focus Request - disp_id: %d, mode: %d, reason: %d", focusRequest.dispChannelId, focusRequest.mode, focusRequest.reason)
                return 0
            }
            MsgType.Media.MICREQUEST -> {
                val micRequest = message.parse(Protocol.MicrophoneRequest())
                return mic_request(micRequest)
            }
            MsgType.Media.ACK -> return 0
            else -> AppLog.e("Unsupported")
        }
        return 0
    }

    private fun mic_request(micRequest: Protocol.MicrophoneRequest): Int {
        AppLog.d("Mic request: %s", micRequest)

        if (micRequest.open) {
            mMicRecorder.start()
        } else {
            mMicRecorder.stop()
        }
        return 0
    }

    private fun media_sink_stop_request(channel: Int): Int {
        AppLog.i("Media Sink Stop Request: " + Channel.name(channel))
        if (Channel.isAudio(channel)) {
            aapAudio.stopAudio(channel)
        }
        return 0
    }

    @Throws(InvalidProtocolBufferNanoException::class)
    private fun executeTouch(message: AapMessage): Int {

        when (message.type) {
            MsgType.Input.BINDINGREQUEST -> {
                val request = message.parse(Protocol.KeyBindingRequest())
                return input_binding(request, message.channel)
            }
            else -> AppLog.e("Unsupported")
        }
        return 0
    }


    @Throws(InvalidProtocolBufferNanoException::class)
    private fun executeSensor(message: AapMessage): Int {
        // 0 - 31, 32768-32799, 65504-65535
        when (message.type) {
            MsgType.Sensor.STARTREQUEST -> {
                val request = message.parse(Protocol.SensorRequest())
                return sensor_start_request(request, message.channel)
            }
            else -> AppLog.e("Unsupported")
        }
        return 0
    }

    @Throws(InvalidProtocolBufferNanoException::class)
    private fun executeControl(message: AapMessage): Int {

        when (message.type) {
            MsgType.Control.SERVICEDISCOVERYREQUEST -> {
                val request = message.parse(Protocol.ServiceDiscoveryRequest())
                return service_discovery_request(request)
            }
            MsgType.Control.PINGREQUEST -> {
                val pingRequest = message.parse(Protocol.PingRequest())
                return ping_request(pingRequest, message.channel)
            }
            MsgType.Control.NAVFOCUSREQUESTNOTIFICATION -> {
                val navigationFocusRequest = message.parse(Protocol.NavFocusRequestNotification())
                return navigation_focus_request(navigationFocusRequest, message.channel)
            }
            MsgType.Control.BYEYEREQUEST -> {
                val shutdownRequest = message.parse(Protocol.ByeByeRequest())
                return byebye_request(shutdownRequest, message.channel)
            }
            MsgType.Control.BYEYERESPONSE -> {
                AppLog.i("Byebye Response")
                return -1
            }
            MsgType.Control.VOICESESSIONNOTIFICATION -> {
                val voiceRequest = message.parse(Protocol.VoiceSessionNotification())
                return voice_session_notification(voiceRequest)
            }
            MsgType.Control.AUDIOFOCUSREQUESTNOTFICATION -> {
                val audioFocusRequest = message.parse(Protocol.AudioFocusRequestNotification())
                return audio_focus_request(audioFocusRequest, message.channel)
            }
            else -> AppLog.e("Unsupported")
        }
        return 0
    }

    private fun media_start_request(request: Protocol.Start, channel: Int): Int {
        AppLog.i("Media Start Request %s: %s", Channel.name(channel), request)

        aapTransport.setSessionId(channel, request.sessionId)
        return 0
    }

    private fun media_sink_setup_request(request: Protocol.MediaSetupRequest, channel: Int): Int {

        AppLog.i("Media Sink Setup Request: %d", request.type)
        // R 2 VID b 00000000 08 03
        // R 4 AUD b 00000000 08 01

        val configResponse = Protocol.Config()
        configResponse.status = Protocol.Config.CONFIG_STATUS_2
        configResponse.maxUnacked = 1
        configResponse.configurationIndices = intArrayOf(0)

        val msg = AapMessage(channel, MsgType.Media.CONFIGRESPONSE, configResponse)
        AppLog.i(msg.toString())
        aapTransport.send(msg)

        if (channel == Channel.ID_VID) {
            aapTransport.gainVideoFocus()
        }

        return 0
    }

    private fun input_binding(request: Protocol.KeyBindingRequest, channel: Int): Int {
        AppLog.i("Input binding request %s", request)

        aapTransport.send(AapMessage(channel, MsgType.Input.BINDINGRESPONSE, Protocol.BindingResponse()))
        return 0
    }

    private fun sensor_start_request(request: Protocol.SensorRequest, channel: Int): Int {
        AppLog.i("Sensor Start Request sensor: %d, minUpdatePeriod: %d", request.type, request.minUpdatePeriod)

        // R 1 SEN b 00000000 08 01 10 00     Sen: 1, 10, 3, 8, 7
        // Yes: SENSOR_TYPE_COMPASS/LOCATION/RPM/DIAGNOSTICS/GEAR      No: SENSOR_TYPE_DRIVING_STATUS

        val msg = AapMessage(channel, MsgType.Sensor.STARTRESPONSE, Protocol.SensorResponse())
        AppLog.i(msg.toString())

        aapTransport.send(msg)

        aapTransport.startSensor(request.type)
        return 0
    }

    private fun channel_open_request(request: Protocol.ChannelOpenRequest, channel: Int): Int {
        // Channel Open Request
        AppLog.i("Channel Open Request - priority: %d  chan: %d %s", request.priority, request.serviceId, Channel.name(request.serviceId))

        val response = Protocol.ChannelOpenResponse()
        response.status = Protocol.STATUS_OK

        val msg = AapMessage(channel, MsgType.Control.CHANNELOPENRESPONSE, response)
        AppLog.i(msg.toString())

        aapTransport.send(msg)

        if (channel == Channel.ID_SEN) {
            Utils.ms_sleep(2)
            AppLog.i("Send driving status")
            aapTransport.send(DrivingStatusEvent(Protocol.SensorBatch.DrivingStatusData.DRIVING_STATUS_PARKED))
        }
        return 0
    }

    @Throws(InvalidProtocolBufferNanoException::class)
    private fun service_discovery_request(request: Protocol.ServiceDiscoveryRequest): Int {                  // Service Discovery Request
        AppLog.i("Service Discovery Request: %s", request.phoneName)                               // S 0 CTR b src: HU  lft:   113  msg_type:     6 Service Discovery Response    S 0 CTR b 00000000 0a 08 08 01 12 04 0a 02 08 0b 0a 13 08 02 1a 0f

        val msg = ServiceDiscoveryResponse(mSettings)
        AppLog.i(msg.toString())

        aapTransport.send(msg)
        return 0
    }

    private fun ping_request(request: Protocol.PingRequest, channel: Int): Int {
        AppLog.i("Ping Request: %d", request.timestamp)

        // Channel Open Response
        val response = Protocol.PingResponse()
        response.timestamp = System.nanoTime()

        val msg = AapMessage(channel, MsgType.Control.PINGRESPONSE, response)
        AppLog.i(msg.toString())

        aapTransport.send(msg)
        return 0
    }

    private fun navigation_focus_request(request: Protocol.NavFocusRequestNotification, channel: Int): Int {
        AppLog.i("Navigation Focus Request: %d", request.focusType)

        // Send Navigation Focus Notification
        val response = Protocol.NavFocusNotification()
        response.focusType = Protocol.NAV_FOCUS_2

        val msg = AapMessage(channel, MsgType.Control.NAVFOCUSRNOTIFICATION, response)
        AppLog.i(msg.toString())

        aapTransport.send(msg)
        return 0
    }

    private fun byebye_request(request: Protocol.ByeByeRequest, channel: Int): Int {
        if (request.reason == 1)
            AppLog.i("Byebye Request reason: 1 AA Exit Car Mode")
        else
            AppLog.e("Byebye Request reason: %d", request.reason)

        val msg = AapMessage(channel, MsgType.Control.BYEYERESPONSE, Protocol.ByeByeResponse())
        AppLog.i(msg.toString())
        aapTransport.send(msg)
        Utils.ms_sleep(100)
        aapTransport.quit()
        return -1
    }

    private fun voice_session_notification(request: Protocol.VoiceSessionNotification): Int {
        // sr:  00000000 00 11 08 01      Microphone voice search usage     sr:  00000000 00 11 08 02
        if (request.status == Protocol.VoiceSessionNotification.VOICE_STATUS_START)
            AppLog.i("Voice Session Notification: 1 START")
        else if (request.status == Protocol.VoiceSessionNotification.VOICE_STATUS_STOP)
            AppLog.i("Voice Session Notification: 2 STOP")
        else
            AppLog.e("Voice Session Notification: %d", request.status)
        return 0
    }

    @Throws(InvalidProtocolBufferNanoException::class)
    private fun audio_focus_request(notification: Protocol.AudioFocusRequestNotification, channel: Int): Int {                  // Audio Focus Request
        AppLog.i("Audio Focus Request: ${notification.request} ${focusName[notification.request]}")

        aapAudio.requestFocusChange(AudioConfigs.stream(channel), notification.request, AudioManager.OnAudioFocusChangeListener {
            val response = Protocol.AudioFocusNotification()

            focusResponse[notification.request]?.let { newSate ->
                AppLog.i("Audio Focus Change: $it ${focusName[it]}")
                response.focusState = newSate
                AppLog.i("Audio Focus State: $newSate ${stateName[newSate]}")

                val msg = AapMessage(channel, MsgType.Control.AUDIOFOCUSNOTFICATION, response)
                AppLog.i(msg.toString())
                aapTransport.send(msg)
            }
        })

        return 0
    }

    companion object {
        private val focusName = mapOf(
            AudioManager.AUDIOFOCUS_GAIN to "AUDIOFOCUS_GAIN",
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT to "AUDIOFOCUS_GAIN_TRANSIENT",
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE to "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE",
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK to "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK",
            AudioManager.AUDIOFOCUS_LOSS to "AUDIOFOCUS_LOSS",
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT to "AUDIOFOCUS_LOSS_TRANSIENT",
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK to "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK",
            AudioManager.AUDIOFOCUS_NONE to "AUDIOFOCUS_NONE"
        )

        private val focusResponse = mapOf(
            AudioManager.AUDIOFOCUS_LOSS to Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT to Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN to Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT to Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK to Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_GAIN_TRANSIENT_GUIDANCE_ONLY
        )

        private val stateName = mapOf(
            Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_GAIN to "AUDIOFOCUS_STATE_GAIN",
            Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_GAIN_TRANSIENT to "AUDIOFOCUS_STATE_GAIN_TRANSIENT",
            Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_LOSS to "AUDIOFOCUS_STATE_LOSS",
            Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_LOSS_TRANSIENT to "AUDIOFOCUS_STATE_LOSS_TRANSIENT",
            Protocol.AudioFocusNotification.AUDIOFOCUS_STATE_LOSS_TRANSIENT_CAN_DUCK to "AUDIOFOCUS_STATE_LOSS_TRANSIENT_CAN_DUCK"
        )
    }

}