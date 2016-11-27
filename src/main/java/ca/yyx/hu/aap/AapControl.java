package ca.yyx.hu.aap;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.SystemUI;
import ca.yyx.hu.utils.Utils;

import static android.R.id.message;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapControl {
    private static final int MSG_TYPE_2 = 32768;
    private final AapTransport mTransport;
    private final AapAudio mAapAudio;
    private final String mBtMacAddress;

    AapControl(AapTransport transport, AapAudio audio, String btMacAddress) {
        mTransport = transport;
        mAapAudio = audio;
        mBtMacAddress = btMacAddress;
    }

    int execute(AapMessage message) throws InvalidProtocolBufferNanoException {

        if (message.type == 7)
        {
            return channel_open_request(message);
        }

        switch (message.channel)
        {
            case Channel.AA_CH_CTR:
                return executeControl(message);
            case Channel.AA_CH_TOU:
                return executeTouch(message.channel, message.type, message.data, message.length);
            case Channel.AA_CH_SEN:
                return executeSensor(message.channel, message.type, message.data, message.length);
            case Channel.AA_CH_VID:
                return executeVideo(message.channel, message.type, message.data, message.length);
            case Channel.AA_CH_AUD:
            case Channel.AA_CH_AU1:
            case Channel.AA_CH_AU2:
                return executeAudio(message.channel, message.type, message.data, message.length);
            case Channel.AA_CH_MIC:
                return executeMicrophone(message.channel, message.type, message.data, message.length);
        }
        return 0;
    }

    private int executeMicrophone(int chan, int msg_type, byte[] buf, int len) {
        switch (msg_type)
        {
            case MSG_TYPE_2 + 0x01:
                return mic_start_request(chan, buf, len);
            case MSG_TYPE_2 + 0x04:
                return mic_ack(chan, buf, len);
            case MSG_TYPE_2 + 0x05:
                return mic_switch_request(chan, buf, len);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
    }

    private int executeAudio(int chan, int msg_type, byte[] buf, int len) {
        switch (msg_type)
        {
            case MSG_TYPE_2:// + 0x00:
                return media_sink_setup_request(chan, buf, len);
            case MSG_TYPE_2 + 0x01:
                return audio_sink_start_request(chan, buf, len);
            case MSG_TYPE_2 + 0x02:
                return audio_sink_stop_request(chan, buf, len);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
    }

    private int mic_start_request(int chan, byte[] buf, int len) {                  // Media Mic Start Request...
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Media Mic Start Request ????");
        else
            AppLog.e("Media Mic Start Request ????: %d", buf[3]);
        return 0;
    }

    private int mic_ack(int chan, byte[] buf, int len) {
        AppLog.i("MIC ACK");
        return 0;
    }

    private int mic_switch_request(int chan, byte[] buf, int len) {
        if (len == 4 && buf[2] == 0x08 && buf[3] == 0) {
            AppLog.i("Mic Start/Stop Request: 0 STOP");

            mTransport.micStop();
        } else if (len != 10 || buf[2] != 0x08 || buf[3] != 1 || buf[4] != 0x10 || buf[6] != 0x18 || buf[8] != 0x20) {
            AppLog.e("Mic Start/Stop Request");
        } else {
            AppLog.i("Mic Start/Stop Request: 1 START %d %d %d", buf[5], buf[7], buf[9]);
            mTransport.micStart();
        }
        return 0;
    }


    private int audio_sink_start_request(int chan, byte[] buf, int len) {                  // Audio Sink Start Request...     First/Second R 4 AUD b 00000000 08 00/01 10 00
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            AppLog.e("Audio Sink Start Request");
        else
            AppLog.i("Audio Sink Start Request: %d %d", buf[3], buf[5]);
        mAapAudio.setAudioAckVal(chan, buf[3]);
        return 0;
    }

    private int audio_sink_stop_request(int chan, byte[] buf, int len) {                  // 08-22 20:03:09.075 D/ .... hex_dump(30767): S 4 AUD b 00000000 08 00 10 01   Only at stop ??
        if (len != 2)//4 || buf [2] != 0x08)
            AppLog.e("Audio Sink Stop Request");
        else
            AppLog.i("Audio Sink Stop Request");//: %d", buf [3]);
            mAapAudio.stopAudio(chan);
        return 0;
    }

    private int executeTouch(int chan, int msg_type, byte[] buf, int len) {

        switch (msg_type)
        {
            case MSG_TYPE_2 + 0x02:
                return aa_pro_tou_b02(chan, buf, len);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
    }

    private int executeVideo(int chan, int msg_type, byte[] buf, int len) {

        switch (msg_type)
        {
            case MSG_TYPE_2:// + 0x00:
                return media_sink_setup_request(chan, buf, len);
            case MSG_TYPE_2 + 0x01:
                return video_start_request(chan, buf, len);
            case MSG_TYPE_2 + 0x07:
                return aa_pro_vid_b07(chan, buf, len);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
    }

    private int executeSensor(int chan, int msg_type, byte[] buf, int len) {
        // 0 - 31, 32768-32799, 65504-65535
        switch (msg_type)
        {
            case MSG_TYPE_2 + 0x01:
                return sensor_start_request(chan, buf, len);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
    }

    private int executeControl(AapMessage message) throws InvalidProtocolBufferNanoException {

        switch (message.type)
        {
            case 5:
                Protocol.ServiceDiscoveryRequest request = parse(new Protocol.ServiceDiscoveryRequest(), message);
                return service_discovery_request(request, message.channel);
            case 0x0b:
                Protocol.PingRequest pingRequest = parse(new Protocol.PingRequest(), message);
                return ping_request(pingRequest, message.channel, message.data);
            case 0x0d:
                Protocol.NavigationFocusRequest navigationFocusRequest = parse(new Protocol.NavigationFocusRequest(), message);
                return navigation_focus_request(navigationFocusRequest, message.channel, message.data);
            case 0x0f:
                Protocol.ShutdownRequest shutdownRequest = parse(new Protocol.ShutdownRequest(), message);
                return byebye_request(shutdownRequest, message.channel, message.data);
            case 0x10:
                AppLog.i("Byebye Response");                                         // R 0 CTR b src: AA  lft:     0  msg_type:    16 Byebye Response
                return -1;
            case 0x11:
                Protocol.VoiceSessionNotification voiceRequest = parse(new Protocol.VoiceSessionNotification(), message);
                return voice_session_notification(voiceRequest);
            case 0x12:
                Protocol.AudioFocusRequestNotification audioFocusRequest = parse(new Protocol.AudioFocusRequestNotification(), message);
                return audio_focus_request(audioFocusRequest, message.channel, message.data);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
    }

    static <T extends MessageNano> T parse(T msg, final AapMessage message) throws InvalidProtocolBufferNanoException
    {
        return  MessageNano.mergeFrom(msg, message.data, message.dataOffset, message.length - message.dataOffset);
    }

    private int aa_pro_vid_b07(int chan, byte[] buf, int len) {                  // Media Video ? Request...
        if (len != 4 || buf[2] != 0x10)
            AppLog.e("Media Video ? Request");
        else
            AppLog.i("Media Video ? Request: %d", buf[3]);
        return 0;
    }

    private int video_start_request(int chan, byte[] buf, int len) {
        // Media Video Start Request...
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            AppLog.e("Media Video Start Request");
        else
            AppLog.i("Media Video Start Request: %d %d", buf[3], buf[5]);
        return 0;
    }

    private int media_sink_setup_request(int chan, byte[] buf, int len) {
        // Media Sink Setup Request
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Media Sink Setup Request");
        else
            AppLog.i("Media Sink Setup Request: %d", buf[3]);
        // R 2 VID b 00000000 08 03       R 4 AUD b 00000000 08 01

        byte rsp[] = {(byte) 0x80, 0x03, 0x08, 2, 0x10, 1, 0x18, 0};
        //0x1a, 4, 0x08, 1, 0x10, 2};      // 1/2, MaxUnack, int[] 1        2, 0x08, 1};//
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);
        // Respond with Config Response
        if (ret != 0)
            return ret;

        if (Channel.isAudio(chan)) {
            return ret;
            // Rely on solicited focus request
            //ms_sleep (20);
            // Else if success and channel = audio...
            //byte rspa [] = {0, 19, 0x08, 1, 0x10, 1};
            // 1, 1     AudioFocus gained focusState=1=AUDIO_FOCUS_STATE_GAIN unsolicited=true
            //return (hu_aap_enc_send (chan, rspa, sizeof (rspa)));
            // Respond with AudioFocus gained
        }

        if (chan == Channel.AA_CH_VID) {
            mTransport.gainVideoFocus();
        }
        return ret;
    }

    private int aa_pro_tou_b02(int chan, byte[] buf, int len) {
        // TouchScreen/Input Start Request...    Or "send setup, ch:X" for channel X
        if (len < 2 || len > 256)
            AppLog.e("Touch/Input/Audio Start/Stop Request");
        else
            AppLog.i("Touch/Input/Audio Start/Stop Request");
        // R 3 TOU b src: AA  lft:     0  msg_type: 32770 Touch/Input/Audio Start/Stop Request
        // R 3 TOU b src: AA  lft:    18  msg_type: 32770 Touch/Input/Audio Start/Stop Request
        // R 3 TOU b 00000000 0a 10 03 54 55 56 57 58 7e 7f d1 01 81 80 04 84     R 3 TOU b     0010 80 04 (Echo Key Array discovered)
        byte rsp[] = {(byte) 0x80, 0x03, 0x08, 0};
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);
        // Respond with Key Binding/Audio Response = OK
        return (ret);
    }

    private int sensor_start_request(int chan, byte[] buf, int len) {                  // Sensor Start Request...
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            AppLog.e("Sensor Start Request");
        else
            AppLog.i("Sensor Start Request sensor: %d   period: %d", buf[3], buf[5]);
        // R 1 SEN b 00000000 08 01 10 00     Sen: 1, 10, 3, 8, 7
        // Yes: SENSOR_TYPE_COMPASS/LOCATION/RPM/DIAGNOSTICS/GEAR      No: SENSOR_TYPE_DRIVING_STATUS
        byte rsp[] = {(byte) 0x80, 0x02, 0x08, 0};
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);
        // Send Sensor Start Response
        return (ret);
    }

    private int channel_open_request(AapMessage message) {
        // Channel Open Request
        AppLog.i("Channel Open Request: %d  chan: %d %s", message.data[3], message.data[5], Channel.name(message.data[5]));
        // R 1 SEN f 00000000 08 00 10 01   R 2 VID f 00000000 08 00 10 02   R 3 TOU f 00000000 08 00 10 03   R 4 AUD f 00000000 08 00 10 04   R 5 MIC f 00000000 08 00 10 05
        byte rsp[] = {0, 8, 8, 0};                                         // Status 0 = OK
        int ret = mTransport.sendEncrypted(message.channel, rsp, rsp.length);                // Send Channel Open Response

        if (ret != 0)                                                            // If error, done with error
            return (ret);

        if (message.channel == Channel.AA_CH_SEN) {                                            // If Sensor channel...
            Utils.ms_sleep(2);//20);
            return mTransport.sendEncrypted(message.channel, Messages.DRIVING_STATUS, Messages.DRIVING_STATUS.length);           // Send Sensor Notification
        }
        return (ret);
    }

    private int service_discovery_request(Protocol.ServiceDiscoveryRequest request, int channel) throws InvalidProtocolBufferNanoException {                  // Service Discovery Request
        AppLog.i("Service Discovery Request: %s", request.phoneName);                               // S 0 CTR b src: HU  lft:   113  msg_type:     6 Service Discovery Response    S 0 CTR b 00000000 0a 08 08 01 12 04 0a 02 08 0b 0a 13 08 02 1a 0f

        byte[] serviceDiscoveryResponse = Messages.createServiceDiscoveryResponse(mBtMacAddress);
        return mTransport.sendEncrypted(channel, serviceDiscoveryResponse, serviceDiscoveryResponse.length);                // Send Service Discovery Response from sd_buf
    }

    private int ping_request(Protocol.PingRequest request, int channel, byte[] buf) {
        AppLog.i("Ping Request: %d", request.timestamp);
        // Channel Open Response
        buf[0] = 0;
        buf[1] = 12;

        Protocol.PingResponse response = new Protocol.PingResponse();
        response.timestamp = System.nanoTime();
        write(response, buf, 2);
        // Send Channel Open Response
        return mTransport.sendEncrypted(channel, buf, response.getSerializedSize() + 2);
    }

    private int navigation_focus_request(Protocol.NavigationFocusRequest request, int channel, byte[] buf) {
        AppLog.i("Navigation Focus Request: %d", request.focusType);
        // Send Navigation Focus Notification
        Protocol.NavigationFocusResponse response = new Protocol.NavigationFocusResponse();
        response.focusType = 2;
        buf[0] = 0;
        buf[1] = 14;
        write(response, buf, 2);
        mTransport.sendEncrypted(channel, buf, 4);
        return 0;
    }

    private int byebye_request(Protocol.ShutdownRequest request, int channel, byte[] buf) {                  // Byebye Request
        if (request.reason == 1)
            AppLog.i("Byebye Request reason: 1 AA Exit Car Mode");
        else
            AppLog.e("Byebye Request reason: %d", request.reason);

        mTransport.sendEncrypted(channel, Messages.BYEBYE_RESPONSE, Messages.BYEBYE_RESPONSE.length);
        Utils.ms_sleep(100);
        return -1;
    }

    private int voice_session_notification(Protocol.VoiceSessionNotification request) {                  // sr:  00000000 00 11 08 01      Microphone voice search usage     sr:  00000000 00 11 08 02
        if (request.status == Protocol.VoiceSessionNotification.VOICE_STATUS_START)
            AppLog.i("Voice Session Notification: 1 START");
        else if (request.status== Protocol.VoiceSessionNotification.VOICE_STATUS_STOP)
            AppLog.i("Voice Session Notification: 2 STOP");
        else
            AppLog.e("Voice Session Notification: %d", request.status);
        return (0);
    }

    private int audio_focus_request(Protocol.AudioFocusRequestNotification notification, int channel, byte[] buf) throws InvalidProtocolBufferNanoException {                  // Audio Focus Request
        if (notification.request == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN)
            AppLog.i("Audio Focus Request: 1 AUDIO_FOCUS_GAIN");
        else if (notification.request == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN_TRANSIENT)
            AppLog.i("Audio Focus Request: 2 AUDIO_FOCUS_GAIN_TRANSIENT");
        else if (notification.request == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_UNKNOWN)
            AppLog.i("Audio Focus Request: 3 gain/release ?");
        else if (notification.request == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_RELEASE)
            AppLog.i("Audio Focus Request: 4 AUDIO_FOCUS_RELEASE");
        else
            AppLog.e("Audio Focus Request: %d", notification.request);

        buf[0] = 0;                                                        // Use request buffer for response
        buf[1] = 19;                                                       // Audio Focus Response

        mAapAudio.requestFocusChange(channel, notification.request);
        Protocol.AudioFocusNotification response = new Protocol.AudioFocusNotification();
        if (notification.request == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_RELEASE) {
            response.focusState = Protocol.AudioFocusNotification.AUDIO_FOCUS_STATE_LOSS;
        } else if (notification.request == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN_TRANSIENT) {
            response.focusState = Protocol.AudioFocusNotification.AUDIO_FOCUS_STATE_GAIN;
        } else {
            response.focusState = Protocol.AudioFocusNotification.AUDIO_FOCUS_STATE_GAIN;
        }
        // Send Audio Focus Response3
        write(response, buf, 2);
        mTransport.sendEncrypted(channel, buf, 4);
        return 0;
    }

    static void write(MessageNano msg, byte[] buf, int offset)
    {
        MessageNano.toByteArray(msg, buf, offset, msg.getSerializedSize());
    }
}