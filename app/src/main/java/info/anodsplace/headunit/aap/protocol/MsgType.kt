package info.anodsplace.headunit.aap.protocol

import info.anodsplace.headunit.aap.protocol.proto.Control
import info.anodsplace.headunit.aap.protocol.proto.Media

/**
 * @author algavris
 * *
 * @date 30/11/2016.
 */

object MsgType {

    const val SIZE = 2

    fun isControl(type: Int): Boolean {
        return type >= Control.ControlMsgType.MEDIADATA.number && type <= Control.ControlMsgType.AUDIOFOCUSNOTFICATION.number
    }

    fun name(type: Int, channel: Int): String {

        when (type) {
            Control.ControlMsgType.MEDIADATA_VALUE -> return "Media Data"
            Control.ControlMsgType.CODECDATA_VALUE -> return "Codec Data"
            Control.ControlMsgType.VERSIONRESPONSE_VALUE ->
                // short:major  short:minor   short:status
                return "Version Response"
            Control.ControlMsgType.HANDSHAKE_VALUE -> return "SSL Handshake Data"
            4 -> return "SSL Authentication Complete Notification"
            Control.ControlMsgType.SERVICEDISCOVERYREQUEST_VALUE -> return "Service Discovery Request"
            Control.ControlMsgType.SERVICEDISCOVERYRESPONSE_VALUE -> return "Service Discovery Response"
            Control.ControlMsgType.CHANNELOPENREQUEST_VALUE -> return "Channel Open Request"
            Control.ControlMsgType.CHANNELOPENRESPONSE_VALUE -> return "Channel Open Response"                       // byte:status
            9 -> return "9 ??"
            10 -> return "10 ??"
            Control.ControlMsgType.PINGREQUEST_VALUE -> return "Ping Request"
            Control.ControlMsgType.PINGRESPONSE_VALUE -> return "Ping Response"
            Control.ControlMsgType.NAVFOCUSREQUESTNOTIFICATION_VALUE -> return "Navigation Focus Request"
            Control.ControlMsgType.NAVFOCUSRNOTIFICATION_VALUE -> return "Navigation Focus Notification"               // NavFocusType
            Control.ControlMsgType.BYEYEREQUEST_VALUE -> return "Byebye Request"
            Control.ControlMsgType.BYEYERESPONSE_VALUE -> return "Byebye Response"
            Control.ControlMsgType.VOICESESSIONNOTIFICATION_VALUE -> return "Voice Session Notification"
            Control.ControlMsgType.AUDIOFOCUSREQUESTNOTFICATION_VALUE -> return "Audio Focus Request"
            Control.ControlMsgType.AUDIOFOCUSNOTFICATION_VALUE -> return "Audio Focus Notification"                    // AudioFocusType   (AudioStreamType ?)

            Media.MediaMsgType.SETUPREQUEST_VALUE -> return "Media Setup Request"                        // Video and Audio sinks receive this and send k3 3 / 32771
            Media.MediaMsgType.STARTREQUEST_VALUE -> {
                return when (channel) {
                    Channel.ID_SEN -> "Sensor Start Request"
                    Channel.ID_INP -> "Input Event"
                    Channel.ID_MPB -> "Media Playback Status"
                    else -> "Media Start Request"
                }
            }
            Media.MediaMsgType.STOPREQUEST_VALUE -> {
                return when (channel) {
                    Channel.ID_SEN -> "Sensor Start Response"
                    Channel.ID_INP -> "Input Binding Request"
                    Channel.ID_MPB -> "Media Playback Status"
                    else -> "Media Stop Request"
                }
            }
            Media.MediaMsgType.CONFIGRESPONSE_VALUE -> {
                return when (channel) {
                    Channel.ID_SEN -> "Sensor Event"
                    Channel.ID_INP -> "Input Binding Response"
                    Channel.ID_MPB -> "Media Playback Status"
                    else -> "Media Config Response"
                }
            }
            Media.MediaMsgType.ACK_VALUE -> return "Codec/Media Data Ack"
            Media.MediaMsgType.MICREQUEST_VALUE -> return "Mic Start/Stop Request"
            Media.MediaMsgType.MICRESPONSE_VALUE -> return "Mic Response"
            Media.MediaMsgType.VIDEOFOCUSREQUESTNOTIFICATION_VALUE -> return "Video Focus Request"
            Media.MediaMsgType.VIDEOFOCUSNOTIFICATION_VALUE -> return "Video Focus Notification"

            65535 -> return "Framing Error Notification"
        }
        return "Unknown"
    }

}
