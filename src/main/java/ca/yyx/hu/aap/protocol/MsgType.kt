package ca.yyx.hu.aap.protocol

/**
 * @author algavris
 * *
 * @date 30/11/2016.
 */

object MsgType {

    val SIZE = 2

    fun isControl(type: Int): Boolean {
        return type >= Control.MEDIADATA && type <= Control.AUDIOFOCUSNOTFICATION
    }

    object Control {
        val MEDIADATA = 0x00
        val CODECDATA = 0x01
        val VERSIONRESPONSE = 0x02
        val HANDSHAKE = 0x03
        val SERVICEDISCOVERYREQUEST = 0x05
        val SERVICEDISCOVERYRESPONSE = 0x06
        val CHANNELOPENREQUEST = 0x07
        val CHANNELOPENRESPONSE = 0x08
        val PINGREQUEST = 0x0B
        val PINGRESPONSE = 0x0C
        val NAVFOCUSREQUESTNOTIFICATION = 0x0D
        val NAVFOCUSRNOTIFICATION = 0x0E
        val BYEYEREQUEST = 0x0F
        val BYEYERESPONSE = 0x10
        val VOICESESSIONNOTIFICATION = 0x11
        val AUDIOFOCUSREQUESTNOTFICATION = 0x12
        val AUDIOFOCUSNOTFICATION = 0x13
    }

    object Media {
        val SETUPREQUEST = 0x8000
        val STARTREQUEST = 0x8001
        val STOPREQUEST = 0x8002
        val CONFIGRESPONSE = 0x8003
        val ACK = 0x8004
        val MICREQUEST = 0x8005
        val MICRESPONSE = 0x8006
        val VIDEOFOCUSREQUESTNOTIFICATION = 0x8007
        val VIDEOFOCUSNOTIFICATION = 0x8008
    }

    object Sensor {
        val STARTREQUEST = 0x8001
        val STARTRESPONSE = 0x8002
        val EVENT = 0x8003
    }

    object Input {
        val EVENT = 0x8001
        val BINDINGREQUEST = 0x8002
        val BINDINGRESPONSE = 0x8003
    }


    fun name(type: Int, channel: Int): String {

        when (type) {
            Control.MEDIADATA -> return "Media Data"
            Control.CODECDATA -> return "Codec Data"
            Control.VERSIONRESPONSE ->
                // short:major  short:minor   short:status
                return "Version Response"
            Control.HANDSHAKE -> return "SSL Handshake Data"
            4 -> return "SSL Authentication Complete Notification"
            Control.SERVICEDISCOVERYREQUEST -> return "Service Discovery Request"
            Control.SERVICEDISCOVERYRESPONSE -> return "Service Discovery Response"
            Control.CHANNELOPENREQUEST -> return "Channel Open Request"
            Control.CHANNELOPENRESPONSE -> return "Channel Open Response"                       // byte:status
            9 -> return "9 ??"
            10 -> return "10 ??"
            Control.PINGREQUEST -> return "Ping Request"
            Control.PINGRESPONSE -> return "Ping Response"
            Control.NAVFOCUSREQUESTNOTIFICATION -> return "Navigation Focus Request"
            Control.NAVFOCUSRNOTIFICATION -> return "Navigation Focus Notification"               // NavFocusType
            Control.BYEYEREQUEST -> return "Byebye Request"
            Control.BYEYERESPONSE -> return "Byebye Response"
            Control.VOICESESSIONNOTIFICATION -> return "Voice Session Notification"
            Control.AUDIOFOCUSREQUESTNOTFICATION -> return "Audio Focus Request"
            Control.AUDIOFOCUSNOTFICATION -> return "Audio Focus Notification"                    // AudioFocusType   (AudioStreamType ?)

            Media.SETUPREQUEST -> return "Media Setup Request"                        // Video and Audio sinks receive this and send k3 3 / 32771
            Media.STARTREQUEST -> {
                if (channel == Channel.ID_SEN) {
                    return "Sensor Start Request"
                } else if (channel == Channel.ID_INP) {
                    return "Input Event"
                }
                return "Media Start Request"
            }
            Media.STOPREQUEST -> {
                if (channel == Channel.ID_SEN) {
                    return "Sensor Start Response"
                } else if (channel == Channel.ID_INP) {
                    return "Input Binding Request"
                }
                return "Media Stop Request"
            }
            Media.CONFIGRESPONSE -> {
                if (channel == Channel.ID_SEN) {
                    return "Sensor Event"
                } else if (channel == Channel.ID_INP) {
                    return "Input Binding Response"
                }
                return "Media Config Response"
            }
            Media.ACK -> return "Codec/Media Data Ack"
            Media.MICREQUEST -> return "Mic Start/Stop Request"
            Media.MICRESPONSE -> return "Mic Response"
            Media.VIDEOFOCUSREQUESTNOTIFICATION -> return "Video Focus Request"
            Media.VIDEOFOCUSNOTIFICATION -> return "Video Focus Notification"

            65535 -> return "Framing Error Notification"
        }
        return "Unknown"
    }

}
