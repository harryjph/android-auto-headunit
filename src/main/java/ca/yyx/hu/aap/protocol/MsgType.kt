package ca.yyx.hu.aap.protocol

import ca.yyx.hu.aap.protocol.nano.Control
import ca.yyx.hu.aap.protocol.nano.Media

/**
 * @author algavris
 * *
 * @date 30/11/2016.
 */

object MsgType {

    const val SIZE = 2

    fun isControl(type: Int): Boolean {
        return type >= Control.MSG_CONTROL_MEDIADATA && type <= Control.MSG_CONTROL_AUDIOFOCUSNOTFICATION
    }

    fun name(type: Int, channel: Int): String {

        when (type) {
            Control.MSG_CONTROL_MEDIADATA -> return "Media Data"
            Control.MSG_CONTROL_CODECDATA -> return "Codec Data"
            Control.MSG_CONTROL_VERSIONRESPONSE ->
                // short:major  short:minor   short:status
                return "Version Response"
            Control.MSG_CONTROL_HANDSHAKE -> return "SSL Handshake Data"
            4 -> return "SSL Authentication Complete Notification"
            Control.MSG_CONTROL_SERVICEDISCOVERYREQUEST -> return "Service Discovery Request"
            Control.MSG_CONTROL_SERVICEDISCOVERYRESPONSE -> return "Service Discovery Response"
            Control.MSG_CONTROL_CHANNELOPENREQUEST -> return "Channel Open Request"
            Control.MSG_CONTROL_CHANNELOPENRESPONSE -> return "Channel Open Response"                       // byte:status
            9 -> return "9 ??"
            10 -> return "10 ??"
            Control.MSG_CONTROL_PINGREQUEST -> return "Ping Request"
            Control.MSG_CONTROL_PINGRESPONSE -> return "Ping Response"
            Control.MSG_CONTROL_NAVFOCUSREQUESTNOTIFICATION -> return "Navigation Focus Request"
            Control.MSG_CONTROL_NAVFOCUSRNOTIFICATION -> return "Navigation Focus Notification"               // NavFocusType
            Control.MSG_CONTROL_BYEYEREQUEST -> return "Byebye Request"
            Control.MSG_CONTROL_BYEYERESPONSE -> return "Byebye Response"
            Control.MSG_CONTROL_VOICESESSIONNOTIFICATION -> return "Voice Session Notification"
            Control.MSG_CONTROL_AUDIOFOCUSREQUESTNOTFICATION -> return "Audio Focus Request"
            Control.MSG_CONTROL_AUDIOFOCUSNOTFICATION -> return "Audio Focus Notification"                    // AudioFocusType   (AudioStreamType ?)

            Media.MSG_MEDIA_SETUPREQUEST -> return "Media Setup Request"                        // Video and Audio sinks receive this and send k3 3 / 32771
            Media.MSG_MEDIA_STARTREQUEST -> {
                if (channel == Channel.ID_SEN) {
                    return "Sensor Start Request"
                } else if (channel == Channel.ID_INP) {
                    return "Input Event"
                } else if (channel == Channel.ID_MPB) {
                    return "Media Playback Status"
                }
                return "Media Start Request"
            }
            Media.MSG_MEDIA_STOPREQUEST -> {
                if (channel == Channel.ID_SEN) {
                    return "Sensor Start Response"
                } else if (channel == Channel.ID_INP) {
                    return "Input Binding Request"
                } else if (channel == Channel.ID_MPB) {
                    return "Media Playback Status"
                }
                return "Media Stop Request"
            }
            Media.MSG_MEDIA_CONFIGRESPONSE -> {
                if (channel == Channel.ID_SEN) {
                    return "Sensor Event"
                } else if (channel == Channel.ID_INP) {
                    return "Input Binding Response"
                } else if (channel == Channel.ID_MPB) {
                    return "Media Playback Status"
                }
                return "Media Config Response"
            }
            Media.MSG_MEDIA_ACK -> return "Codec/Media Data Ack"
            Media.MSG_MEDIA_MICREQUEST -> return "Mic Start/Stop Request"
            Media.MSG_MEDIA_MICRESPONSE -> return "Mic Response"
            Media.MSG_MEDIA_VIDEOFOCUSREQUESTNOTIFICATION -> return "Video Focus Request"
            Media.MSG_MEDIA_VIDEOFOCUSNOTIFICATION -> return "Video Focus Notification"

            65535 -> return "Framing Error Notification"
        }
        return "Unknown"
    }

}
