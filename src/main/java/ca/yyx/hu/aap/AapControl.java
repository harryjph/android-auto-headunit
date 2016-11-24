package ca.yyx.hu.aap;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapControl {
    private static final int MSG_TYPE_2 = 32768;
    private final AapTransport mTransport;
    private final AapAudio mAapAudio;

    AapControl(AapTransport transport, AapAudio audio) {
        mTransport = transport;
        mAapAudio = audio;
    }

    int execute(AapMessage message) throws InvalidProtocolBufferNanoException {
        return execute(message.channel, message.type, message.data, message.length);
    }

    private int execute(int chan, int msg_type, byte[] buf, int len) throws InvalidProtocolBufferNanoException {

        if (msg_type == 7)
        {
            return channel_open_request(chan, buf, len);
        }

        switch (chan)
        {
            case Channel.AA_CH_CTR:
                return executeControl(chan, msg_type, buf, len);
            case Channel.AA_CH_TOU:
                return executeTouch(chan, msg_type, buf, len);
            case Channel.AA_CH_SEN:
                return executeSensor(chan, msg_type, buf, len);
            case Channel.AA_CH_VID:
                return executeVideo(chan, msg_type, buf, len);
            case Channel.AA_CH_AUD:
            case Channel.AA_CH_AU1:
            case Channel.AA_CH_AU2:
                return executeAudio(chan, msg_type, buf, len);
            case Channel.AA_CH_MIC:
                return executeMicrophone(chan, msg_type, buf, len);
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

    private int executeControl(int chan, int msg_type, byte[] buf, int len) throws InvalidProtocolBufferNanoException {

        switch (msg_type)
        {
            case 5:
                return service_discovery_request(chan, buf, len);
            case 0x0b:
                return ping_request(chan, buf, len);
            case 0x0d:
                return navigation_focus_request(chan, buf, len);
            case 0x0f:
                return byebye_request(chan, buf, len);
            case 0x10:
                return byebye_response(chan, buf, len);
            case 0x11:
                return voice_session_notification(chan, buf, len);
            case 0x12:
                return audio_focus_request(chan, buf, len);
            default:
                AppLog.e("Unsupported");
        }
        return 0;
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

    private int channel_open_request(int chan, byte[] buf, int len) {
        // Channel Open Request
        AppLog.i("Channel Open Request: %d  chan: %d %s", buf[3], buf[5], Channel.name(buf[5]));
        // R 1 SEN f 00000000 08 00 10 01   R 2 VID f 00000000 08 00 10 02   R 3 TOU f 00000000 08 00 10 03   R 4 AUD f 00000000 08 00 10 04   R 5 MIC f 00000000 08 00 10 05
        byte rsp[] = {0, 8, 8, 0};                                         // Status 0 = OK
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);                // Send Channel Open Response

        if (ret != 0)                                                            // If error, done with error
            return (ret);

        if (chan == Channel.AA_CH_SEN) {                                            // If Sensor channel...
            Utils.ms_sleep(2);//20);
            return mTransport.sendEncrypted(chan, Messages.DRIVING_STATUS, Messages.DRIVING_STATUS.length);           // Send Sensor Notification
        }
        return (ret);
    }

    private int service_discovery_request(int chan, byte[] buf, int len) throws InvalidProtocolBufferNanoException {                  // Service Discovery Request
//
//        Protocol.ServiceDiscoveryRequest request = Protocol.ServiceDiscoveryRequest.parseFrom(buf);
//
//        MessageNano.mergeFrom(new Protocol.ServiceDiscoveryRequest(), buf, offset, )

        if (len < 4 || buf[2] != 0x0a)
            AppLog.e("Service Discovery Request: %x", buf[2]);
        else
            AppLog.i("Service Discovery Request");                               // S 0 CTR b src: HU  lft:   113  msg_type:     6 Service Discovery Response    S 0 CTR b 00000000 0a 08 08 01 12 04 0a 02 08 0b 0a 13 08 02 1a 0f

        byte[] serviceDiscoveryResponse = Messages.createServiceDiscoveryResponse();
        return mTransport.sendEncrypted(chan, serviceDiscoveryResponse, serviceDiscoveryResponse.length);                // Send Service Discovery Response from sd_buf
    }

    private int ping_request(int chan, byte[] buf, int len) {
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Ping Request");
        else
            AppLog.i("Ping Request: %d", buf[3]);
        // Channel Open Response
        buf[0] = 0;
        buf[1] = 12;
        // Send Channel Open Response
        return mTransport.sendEncrypted(chan, buf, len);
    }

    private int navigation_focus_request(int chan, byte[] buf, int len) {
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Navigation Focus Request");
        else
            AppLog.i("Navigation Focus Request: %d", buf[3]);
        // Send Navigation Focus Notification
        mTransport.sendEncrypted(chan, Messages.NAVIGATION_FOCUS, Messages.NAVIGATION_FOCUS.length);
        return 0;
    }

    private int byebye_request(int chan, byte[] buf, int len) {                  // Byebye Request
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Byebye Request");
        else if (buf[3] == 1)
            AppLog.i("Byebye Request reason: 1 AA Exit Car Mode");
        else if (buf[3] == 2)
            AppLog.e("Byebye Request reason: 2 ?");
        else
            AppLog.e("Byebye Request reason: %d", buf[3]);

        // Send Byebye Response
        int ret = mTransport.sendEncrypted(chan, Messages.BYEBYE_RESPONSE, Messages.BYEBYE_RESPONSE.length);
        Utils.ms_sleep(100);                                                     // Wait a bit for response
        return -1;
    }

    private int byebye_response(int chan, byte[] buf, int len) {                  // Byebye Response
        if (len != 2)
            AppLog.e("Byebye Response");
        else
            AppLog.i("Byebye Response");                                         // R 0 CTR b src: AA  lft:     0  msg_type:    16 Byebye Response
        return -1;
    }

    private int voice_session_notification(int chan, byte[] buf, int len) {                  // sr:  00000000 00 11 08 01      Microphone voice search usage     sr:  00000000 00 11 08 02
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Voice Session Notification");
        else if (buf[3] == 1)
            AppLog.i("Voice Session Notification: 1 START");
        else if (buf[3] == 2)
            AppLog.i("Voice Session Notification: 2 STOP");
        else
            AppLog.e("Voice Session Notification: %d", buf[3]);
        return (0);
    }

    private int audio_focus_request(int chan, byte[] buf, int len) {                  // Audio Focus Request
        if (len != 4 || buf[2] != 0x08)
            AppLog.e("Audio Focus Request");
        else if (buf[3] == 1)
            AppLog.i("Audio Focus Request: 1 AUDIO_FOCUS_GAIN ?");
        else if (buf[3] == 2)
            AppLog.i("Audio Focus Request: 2 AUDIO_FOCUS_GAIN_TRANSIENT");
        else if (buf[3] == 3)
            AppLog.i("Audio Focus Request: 3 gain/release ?");
        else if (buf[3] == 4)
            AppLog.i("Audio Focus Request: 4 AUDIO_FOCUS_RELEASE");
        else
            AppLog.e("Audio Focus Request: %d", buf[3]);
        buf[0] = 0;                                                        // Use request buffer for response
        buf[1] = 19;                                                       // Audio Focus Response
        buf[2] = 0x08;
        // buf[3]: See senderprotocol/q.java:
        // 1: AUDIO_FOCUS_STATE_GAIN
        // 2: AUDIO_FOCUS_STATE_GAIN_TRANSIENT
        // 3: AUDIO_FOCUS_STATE_LOSS
        // 4: AUDIO_FOCUS_STATE_LOSS_TRANSIENT_CAN_DUCK
        // 5: AUDIO_FOCUS_STATE_LOSS_TRANSIENT
        // 6: AUDIO_FOCUS_STATE_GAIN_MEDIA_ONLY
        // 7: AUDIO_FOCUS_STATE_GAIN_TRANSIENT_GUIDANCE_ONLY
        if (buf[3] == 4) {                                                  // If AUDIO_FOCUS_RELEASE...
            buf[3] = 3;
        }   // Send AUDIO_FOCUS_STATE_LOSS
        else if (buf[3] == 2) {                                             // If AUDIO_FOCUS_GAIN_TRANSIENT...
            buf[3] = 1;//2;                                                      // Send AUDIO_FOCUS_STATE_GAIN_TRANSIENT
        } else {
            buf[3] = 1;                                                      // Send AUDIO_FOCUS_STATE_GAIN
        }
        //buf [4] = 0x10;
        //buf [5] = 0;                                                      // unsolicited:   0 = false   1 = true
        int ret = mTransport.sendEncrypted(chan, buf, 4);//6);                      // Send Audio Focus Response
        return (0);
    }

}