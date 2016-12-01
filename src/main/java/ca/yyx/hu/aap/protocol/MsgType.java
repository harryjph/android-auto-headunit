package ca.yyx.hu.aap.protocol;

/**
 * @author algavris
 * @date 30/11/2016.
 */

public class MsgType {

    public static final int SIZE = 2;

    public static class Control {
        public static final int MEDIADATA = 0x00;
        public static final int CODECDATA = 0x01;
        public static final int VERSIONRESPONSE = 0x02;
        public static final int SSLHANDSHAKE = 0x03;
        public static final int SERVICEDISCOVERYREQUEST = 0x05;
        public static final int SERVICEDISCOVERYRESPONSE = 0x06;
        public static final int CHANNELOPENREQUEST = 0x07;
        public static final int CHANNELOPENRESPONSE = 0x08;
        public static final int PINGREQUEST = 0x0B;
        public static final int PINGRESPONSE = 0x0C;
        public static final int NAVFOCUSREQUESTNOTIFICATION = 0x0D;
        public static final int NAVFOCUSRNOTIFICATION = 0x0E;
        public static final int BYEYEREQUEST = 0x0F;
        public static final int SHUTDOWNRESPONSE = 0x10;
        public static final int VOICESESSIONNOTIFICATION = 0x11;
        public static final int AUDIOFOCUSREQUESTNOTFICATION = 0x12;
        public static final int AUDIOFOCUSNOTFICATION = 0x13;
    }

    public static class Media {
        public static final int SETUPREQUEST = 0x8000;
        public static final int STARTREQUEST = 0x8001;
        public static final int STOPREQUEST = 0x8002;
        public static final int CONFIGRESPONSE = 0x8003;
        public static final int ACK = 0x8004;
        public static final int MICREQUEST = 0x8005;
        public static final int MICRESPONSE = 0x8006;
        public static final int VIDEOFOCUSREQUESTNOTIFICATION = 0x8007;
        public static final int VIDEOFOCUSNOTIFICATION = 0x8008;
    }

    public static class Sensor {
        public static final int STARTREQUEST = 0x8001;
        public static final int STARTRESPONSE = 0x8002;
        public static final int EVENT = 0x8003;
    }

    public static class Input {
        public static final int EVENT = 0x8001;
        public static final int BINDINGREQUEST = 0x8002;
        public static final int BINDINGRESPONSE = 0x8003;
    }


    public static String name(int type, int channel) {

        switch (type) {
            case Control.MEDIADATA:
                return "Media Data";
            case Control.CODECDATA:
                return "Codec Data";
            case Control.VERSIONRESPONSE:
                // short:major  short:minor   short:status
                return "Version Response";
            case Control.SSLHANDSHAKE:
                return "SSL Handshake Data";
            case 4:
                return "SSL Authentication Complete Notification";
            case Control.SERVICEDISCOVERYREQUEST:
                return "Service Discovery Request";
            case Control.SERVICEDISCOVERYRESPONSE:
                return "Service Discovery Response";
            case Control.CHANNELOPENREQUEST:
                return "Channel Open Request";
            case Control.CHANNELOPENRESPONSE:
                return "Channel Open Response";                       // byte:status
            case 9:
                return "9 ??";
            case 10:
                return "10 ??";
            case Control.PINGREQUEST:
                return "Ping Request";
            case Control.PINGRESPONSE:
                return "Ping Response";
            case Control.NAVFOCUSREQUESTNOTIFICATION:
                return "Navigation Focus Request";
            case Control.NAVFOCUSRNOTIFICATION:
                return "Navigation Focus Notification";               // NavFocusType
            case Control.BYEYEREQUEST:
                return "Byebye Request";
            case Control.SHUTDOWNRESPONSE:
                return "Byebye Response";
            case Control.VOICESESSIONNOTIFICATION:
                return "Voice Session Notification";
            case Control.AUDIOFOCUSREQUESTNOTFICATION:
                return "Audio Focus Request";
            case Control.AUDIOFOCUSNOTFICATION:
                return "Audio Focus Notification";                    // AudioFocusType   (AudioStreamType ?)

            case Media.SETUPREQUEST:
                return "Media Setup Request";                        // Video and Audio sinks receive this and send k3 3 / 32771
            case Media.STARTREQUEST:
                if (channel == Channel.AA_CH_SEN) {
                    return "Sensor Start Request";
                } else if (channel == Channel.AA_CH_TOU) {
                    return "Input Event";
                }
                return "Media Start Request";
            case Media.STOPREQUEST:
                if (channel == Channel.AA_CH_SEN) {
                    return "Sensor Start Response";
                } else if (channel == Channel.AA_CH_TOU) {
                    return "Input Binding Request";
                }
                return "Media Stop Request";
            case Media.CONFIGRESPONSE:
                if (channel == Channel.AA_CH_SEN) {
                    return "Sensor Event";
                } else if (channel == Channel.AA_CH_TOU) {
                    return "Input Binding Response";
                }
                return "Media Config Response";
            case Media.ACK:
                return "Codec/Media Data Ack";
            case Media.MICREQUEST:
                return "Mic Start/Stop Request";
            case Media.MICRESPONSE:
                return "Mic Response";
            case Media.VIDEOFOCUSREQUESTNOTIFICATION:
                return "Video Focus Request";
            case Media.VIDEOFOCUSNOTIFICATION:
                return "Video Focus Notification";

            case 65535:
                return "Framing Error Notification";
        }
        return "Unknown";
    }

}
