package ca.yyx.hu.aap;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapControl {
    private static final int MSG_TYPE_2 = 32768;
    private final AapTransport mTransport;
    private final AapAudio mAapAudio;
    private final AapMicrophone mAapMicrophone;

    AapControl(AapTransport transport, AapAudio audio, AapMicrophone microphone) {
        mTransport = transport;
        mAapAudio = audio;
        mAapMicrophone = microphone;
    }

    int execute(AapMessage message) {
        return execute(message.channel, message.message_type, message.data, message.length);
    }

    int execute(int chan, int msg_type, byte[] buf, int len) {

        if (chan < 0 || chan > Channel.MAX) {
            Utils.loge("chan >= 0 && chan <= AA_CH_MAX chan: %d", chan);
        }

        switch (chan)
        {
            case Channel.AA_CH_CTR:
                return executeControl(chan, msg_type, buf, len);
            case Channel.AA_CH_SEN:
                return executeSensor(chan, msg_type, buf, len);
            case Channel.AA_CH_VID:
                return executeVideo(chan, msg_type, buf, len);
            case Channel.AA_CH_TOU:
                return executeTouch(chan, msg_type, buf, len);
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
            case 7:
                return aa_pro_all_a07(chan, buf, len);
            case MSG_TYPE_2 + 0x01:
                return aa_pro_mic_b01(chan, buf, len);
            case MSG_TYPE_2 + 0x04:
                return aa_pro_mic_b04(chan, buf, len);
            case MSG_TYPE_2 + 0x05:
                return aa_pro_mic_b05(chan, buf, len);
            default:
                Utils.loge("Unsupported");
        }
        return 0;
    }

    private int executeAudio(int chan, int msg_type, byte[] buf, int len) {
        switch (msg_type)
        {
            case 7:
                return aa_pro_all_a07(chan, buf, len);
            case MSG_TYPE_2:// + 0x00:
                return aa_pro_snk_b00(chan, buf, len);
            case MSG_TYPE_2 + 0x01:
                return aa_pro_aud_b01(chan, buf, len);
            case MSG_TYPE_2 + 0x02:
                return aa_pro_aud_b02(chan, buf, len);
            default:
                Utils.loge("Unsupported");
        }
        return 0;
    }

    private int aa_pro_mic_b01(int chan, byte[] buf, int len) {                  // Media Mic Start Request...
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Media Mic Start Request ????");
        else
            Utils.loge ("Media Mic Start Request ????: %d", buf[3]);
        return 0;
    }

    private int aa_pro_mic_b04(int chan, byte[] buf, int len) {
        Utils.logd("MIC ACK");
        //hex_dump("MIC ACK: ", 16, buf, len);
        return 0;
    }

    private int aa_pro_mic_b05(int chan, byte[] buf, int len) {
        if (len == 4 && buf[2] == 0x08 && buf[3] == 0) {
            Utils.logd ("Mic Start/Stop Request: 0 STOP");
            mAapMicrophone.setStatus(1); // Stop Mic
        } else if (len != 10 || buf[2] != 0x08 || buf[3] != 1 || buf[4] != 0x10 || buf[6] != 0x18 || buf[8] != 0x20) {
            Utils.loge ("Mic Start/Stop Request");
        } else {
            Utils.logd ("Mic Start/Stop Request: 1 START %d %d %d", buf[5], buf[7], buf[9]);
            mAapMicrophone.setStatus(2); // Start Mic
        }
        return 0;
    }


    private int aa_pro_aud_b01(int chan, byte[] buf, int len) {                  // Audio Sink Start Request...     First/Second R 4 AUD b 00000000 08 00/01 10 00
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            Utils.loge ("Audio Sink Start Request");
        else
            Utils.logd ("Audio Sink Start Request: %d %d", buf[3], buf[5]);
        mAapAudio.setAudioAckVal(chan, buf[3]);
        return 0;
    }

    private int aa_pro_aud_b02(int chan, byte[] buf, int len) {                  // 08-22 20:03:09.075 D/ .... hex_dump(30767): S 4 AUD b 00000000 08 00 10 01   Only at stop ??
        if (len != 2)//4 || buf [2] != 0x08)
            Utils.loge ("Audio Sink Stop Request");
        else
            Utils.logd ("Audio Sink Stop Request");//: %d", buf [3]);
        mAapAudio.setOutState(chan, 1);
        return 0;
    }

    private int executeTouch(int chan, int msg_type, byte[] buf, int len) {

        switch (msg_type)
        {
            case 7:
                return aa_pro_all_a07(chan, buf, len);
            case MSG_TYPE_2 + 0x02:
                return aa_pro_tou_b02(chan, buf, len);
            default:
                Utils.loge("Unsupported");
        }
        return 0;
    }

    private int executeVideo(int chan, int msg_type, byte[] buf, int len) {

        switch (msg_type)
        {
            case 7:
                return aa_pro_all_a07(chan, buf, len);
            case MSG_TYPE_2:// + 0x00:
                return aa_pro_snk_b00(chan, buf, len);
            case MSG_TYPE_2 + 0x01:
                return aa_pro_vid_b01(chan, buf, len);
            case MSG_TYPE_2 + 0x07:
                return aa_pro_vid_b07(chan, buf, len);
            default:
                Utils.loge("Unsupported");
        }
        return 0;
    }

    private int executeSensor(int chan, int msg_type, byte[] buf, int len) {
        // 0 - 31, 32768-32799, 65504-65535
        switch (msg_type)
        {
            case 7:
                return aa_pro_all_a07(chan, buf, len);
            case MSG_TYPE_2 + 0x01:
                return aa_pro_sen_b01(chan, buf, len);
            default:
                Utils.loge("Unsupported");
        }
        return 0;
    }

    private int executeControl(int chan, int msg_type, byte[] buf, int len) {

        switch (msg_type)
        {
            case 5:
                return aa_pro_ctr_a05(chan, buf, len);
            case 7:
                return aa_pro_all_a07(chan, buf, len);
            case 0x0b:
                return aa_pro_ctr_a0b(chan, buf, len);
            case 0x0d:
                return aa_pro_ctr_a0d(chan, buf, len);
            case 0x0f:
                return aa_pro_ctr_a0f(chan, buf, len);
            case 0x10:
                return aa_pro_ctr_a10(chan, buf, len);
            case 0x11:
                return aa_pro_ctr_a11(chan, buf, len);
            case 0x12:
                return aa_pro_ctr_a12(chan, buf, len);
            default:
                Utils.loge("Unsupported");
        }
        return 0;
    }

    private int aa_pro_vid_b07(int chan, byte[] buf, int len) {                  // Media Video ? Request...
        if (len != 4 || buf[2] != 0x10)
            Utils.loge ("Media Video ? Request");
        else
            Utils.logd ("Media Video ? Request: %d", buf[3]);
        return 0;
    }

    private int aa_pro_vid_b01(int chan, byte[] buf, int len) {
        // Media Video Start Request...
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            Utils.loge ("Media Video Start Request");
        else
            Utils.logd ("Media Video Start Request: %d %d", buf[3], buf[5]);

//    byte rsp2 [] = {0x80, 0x08, 0x08, 1, 0x10, 1};
// 1, 1     VideoFocus gained focusState=1 unsolicited=true
//    ret = hu_aap_enc_send (chan, rsp2, sizeof (rsp2));
// Send VideoFocus Notification
//    ms_sleep (300);
/*
    //#define MAX_UNACK 8     //1;
    byte rsp [] = {0x80, 0x03, 0x08, 2, 0x10, 1, 0x18, 0};//0x1a, 4, 0x08, 1, 0x10, 2};
     // 1/2, MaxUnack, int[] 1        2, 0x08, 1};//
    ret = hu_aap_enc_send (chan, rsp, sizeof (rsp));
      // Respond with Config Response
*/
        return 0;
    }

    private int aa_pro_snk_b00(int chan, byte[] buf, int len) {
        // Media Sink Setup Request
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Media Sink Setup Request");
        else
            Utils.logd ("Media Sink Setup Request: %d", buf[3]);
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
            // Else if success and channel = video...
            byte rsp2[] = {(byte) 0x80, 0x08, 0x08, 1, 0x10, 1};
            // 1, 1     VideoFocus gained focusState=1 unsolicited=true     010b0000800808011001
            return mTransport.sendEncrypted(chan, rsp2, rsp2.length);
            // Respond with VideoFocus gained
        }
        return ret;
    }

    private int aa_pro_tou_b02(int chan, byte[] buf, int len) {
        // TouchScreen/Input Start Request...    Or "send setup, ch:X" for channel X
        if (len < 2 || len > 256)
            Utils.loge ("Touch/Input/Audio Start/Stop Request");
        else
            Utils.logd ("Touch/Input/Audio Start/Stop Request");
        // R 3 TOU b src: AA  lft:     0  msg_type: 32770 Touch/Input/Audio Start/Stop Request
        // R 3 TOU b src: AA  lft:    18  msg_type: 32770 Touch/Input/Audio Start/Stop Request
        // R 3 TOU b 00000000 0a 10 03 54 55 56 57 58 7e 7f d1 01 81 80 04 84     R 3 TOU b     0010 80 04 (Echo Key Array discovered)
        byte rsp[] = {(byte) 0x80, 0x03, 0x08, 0};
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);
        // Respond with Key Binding/Audio Response = OK
        return (ret);
    }

    private int aa_pro_sen_b01(int chan, byte[] buf, int len) {                  // Sensor Start Request...
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            Utils.loge ("Sensor Start Request");
        else
            Utils.logd ("Sensor Start Request sensor: %d   period: %d", buf[3], buf[5]);  // R 1 SEN b 00000000 08 01 10 00     Sen: 1, 10, 3, 8, 7
        // Yes: SENSOR_TYPE_COMPASS/LOCATION/RPM/DIAGNOSTICS/GEAR      No: SENSOR_TYPE_DRIVING_STATUS
        byte rsp[] = {(byte) 0x80, 0x02, 0x08, 0};
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);
        // Send Sensor Start Response
        return (ret);
    }

    private int aa_pro_all_a07(int chan, byte[] buf, int len) {                  // Channel Open Request
        if (len != 6 || buf[2] != 0x08 || buf[4] != 0x10)
            Utils.loge ("Channel Open Request");
        else
            Utils.logd ("Channel Open Request: %d  chan: %d", buf[3], buf[5]);
        // R 1 SEN f 00000000 08 00 10 01   R 2 VID f 00000000 08 00 10 02   R 3 TOU f 00000000 08 00 10 03   R 4 AUD f 00000000 08 00 10 04   R 5 MIC f 00000000 08 00 10 05
        byte rsp[] = {0, 8, 8, 0};                                         // Status 0 = OK
        int ret = mTransport.sendEncrypted(chan, rsp, rsp.length);                // Send Channel Open Response

        if (ret == 0 && chan == Channel.AA_CH_MIC) {
            //byte rspm [] = {0, 17, 0x08, 1, 0x10, 1};                         // 1, 1     Voice Session not focusState=1=AUDIO_FOCUS_STATE_GAIN unsolicited=true    050b0000001108011001
            //ret = hu_aap_enc_send (chan, rspm, sizeof (rspm));                // Send AudioFocus Notification
            //ms_sleep (200);
            //logd ("Channel Open Request AFTER ms_sleep (500)");
        }

        if (ret != 0)                                                            // If error, done with error
            return (ret);

        if (chan == Channel.AA_CH_SEN) {                                            // If Sensor channel...
            Utils.ms_sleep(2);//20);
            byte rspds[] = {(byte) 0x80, 0x03, 0x6a, 2, 8, 0};                      // Driving Status = 0 = Parked (1 = Moving)
            return mTransport.sendEncrypted(chan, rspds, rspds.length);           // Send Sensor Notification
        }
        return (ret);
    }

    private int aa_pro_ctr_a05(int chan, byte[] buf, int len) {                  // Service Discovery Request
        if (len < 4 || buf[2] != 0x0a)
            Utils.loge ("Service Discovery Request: %x", buf[2]);
        else
            Utils.logd ("Service Discovery Request");                               // S 0 CTR b src: HU  lft:   113  msg_type:     6 Service Discovery Response    S 0 CTR b 00000000 0a 08 08 01 12 04 0a 02 08 0b 0a 13 08 02 1a 0f

        return mTransport.sendEncrypted(chan, sd_buf, sd_buf.length);                // Send Service Discovery Response from sd_buf
    }

    private int aa_pro_ctr_a0b(int chan, byte[] buf, int len) {                  // Ping Request
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Ping Request");
        else
            Utils.logd ("Ping Request: %d", buf[3]);
        buf[0] = 0;                                                        // Use request buffer for response
        buf[1] = 12;                                                       // Channel Open Response
        int ret = mTransport.sendEncrypted(chan, buf, len);                         // Send Channel Open Response
        return (ret);
    }

    private int aa_pro_ctr_a0d(int chan, byte[] buf, int len) {                  // Navigation Focus Request
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Navigation Focus Request");
        else
            Utils.logd ("Navigation Focus Request: %d", buf[3]);
        buf[0] = 0;                                                        // Use request buffer for response
        buf[1] = 14;                                                       // Navigation Focus Notification
        buf[2] = 0x08;
        buf[3] = 2;                                                        // Gained / Gained Transient ?
        int ret = mTransport.sendEncrypted(chan, buf, 4);                         // Send Navigation Focus Notification
        return (0);
    }

    private int aa_pro_ctr_a0f(int chan, byte[] buf, int len) {                  // Byebye Request
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Byebye Request");
        else if (buf[3] == 1)
            Utils.logd ("Byebye Request reason: 1 AA Exit Car Mode");
        else if (buf[3] == 2)
            Utils.loge ("Byebye Request reason: 2 ?");
        else
            Utils.loge ("Byebye Request reason: %d", buf[3]);

        buf[0] = 0;                                                        // Use request buffer for response
        buf[1] = 16;                                                       // Byebye Response
        buf[2] = 0x08;
        buf[3] = 0;                                                        // Status 0 = OK
        int ret = mTransport.sendEncrypted(chan, buf, 4);                           // Send Byebye Response
        Utils.ms_sleep(100);                                                     // Wait a bit for response
        //terminate = 1;

        return (-1);
    }

    private int aa_pro_ctr_a10(int chan, byte[] buf, int len) {                  // Byebye Response
        if (len != 2)
            Utils.loge ("Byebye Response");
        else
            Utils.logd ("Byebye Response");                                         // R 0 CTR b src: AA  lft:     0  msg_type:    16 Byebye Response
        return -1;
    }

    private int aa_pro_ctr_a11(int chan, byte[] buf, int len) {                  // sr:  00000000 00 11 08 01      Microphone voice search usage     sr:  00000000 00 11 08 02
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Voice Session Notification");
        else if (buf[3] == 1)
            Utils.logd ("Voice Session Notification: 1 START");
        else if (buf[3] == 2)
            Utils.logd ("Voice Session Notification: 2 STOP");
        else
            Utils.loge ("Voice Session Notification: %d", buf[3]);
        return (0);
    }

    private int aa_pro_ctr_a12(int chan, byte[] buf, int len) {                  // Audio Focus Request
        if (len != 4 || buf[2] != 0x08)
            Utils.loge ("Audio Focus Request");
        else if (buf[3] == 1)
            Utils.logd ("Audio Focus Request: 1 AUDIO_FOCUS_GAIN ?");
        else if (buf[3] == 2)
            Utils.logd ("Audio Focus Request: 2 AUDIO_FOCUS_GAIN_TRANSIENT");
        else if (buf[3] == 3)
            Utils.logd ("Audio Focus Request: 3 gain/release ?");
        else if (buf[3] == 4)
            Utils.logd ("Audio Focus Request: 4 AUDIO_FOCUS_RELEASE");
        else
            Utils.loge ("Audio Focus Request: %d", buf[3]);
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

    private byte sd_buf [] = {0, 6,        //8, 0};                                            // Svc Disc Rsp = 6
/*
cq  (co[  (str  (str  (str  (str  (int  (str  (str  (str  (str  (boo  (boo    MsgServiceDiscoveryResponse

  co  int (cm (bd (ak (bi (m  (ce (bq (bb (cb (av (cy (ad       co[] a()      MsgAllServices

    cm  (cn[                                                                  MsgSensors
      cn  int                                                   cn[] a()      MsgSensorSourceService  Fix name to MsgSensor

    bd  (int  (int  (f[ (cz[  (boo                                            MsgMediaSinkService
       f  int   int   int                                        f[] a()      MsgAudCfg   See same below
      cz  (int  (int  (int  (int  (int  (int                    cz[] a()      MsgVidCfg

    ak  (int[ (am[  (al[                                                      MsgInputSourceService   int[] = keycodes    Graphics Points ?
      am  int   int                                             am[] a()      TouchScreen width, height
      al  int   int                                             al[] a()      TouchPad    width, height

Audio Config:
  sampleRate
  channelConfig
  audioFormat

public final class MsgMediaSinkService extends k                        // bd/MsgMediaSinkService extends k/com.google.protobuf.nano.MessageNano
{
  public int      a                 = 0;                                // a
  public int      mCodecType        = 1;                                // b
  public int      mAudioStreamType  = 1;                                // c
  public f[]      mAudioStreams     = f.a();                            // f[]:d    a:samplingRate    b:numBits     c:channels
  public cz[]     mCodecs           = cz.a();                           // cz[]:e   b:codecResolution 1=800x480 2=1280x720 3=1920x1080
                                                                                //  c:0/1 for 30/60 fps   d:widthMargin e:heightMargin f:density/fps g: ?
  private boolean f                 = false;                            // f

*/
// D/CAR.GAL ( 3804): Service id=1 type=MediaSinkService { codec type=1 { codecResolution=1 widthMargin=0 heightMargin=0 density=30}}

            // CH 1 Sensors:                      //cq/co[]
//*
            0x0A, 4 + 4 * 2,//co: int, cm/cn[]
            0x08, Channel.AA_CH_SEN,
            0x12, 4 * 2,
            0x0A, 2,
            0x08, 11, // SENSOR_TYPE_DRIVING_STATUS 12
            0x0A, 2,
            0x08, 10, // SENSOR_TYPE_NIGHT_DATA 10

//*/
/*  Requested Sensors: 10, 9, 2, 7, 6:
                        0x0A, 4 + 4*6,     //co: int, cm/cn[]
                                      0x08, AA_CH_SEN,  0x12, 4*6,
                                                          0x0A, 2,
                                                                    0x08, 11, // SENSOR_TYPE_DRIVING_STATUS 12
                                                          0x0A, 2,
                                                                    0x08,  3, // SENSOR_TYPE_RPM            2
                                                          0x0A, 2,
                                                                    0x08,  8, // SENSOR_TYPE_DIAGNOSTICS    7
                                                          0x0A, 2,
                                                                    0x08,  7, // SENSOR_TYPE_GEAR           6
                                                          0x0A, 2,
                                                                    0x08,  1, // SENSOR_TYPE_COMPASS       10
                                                          0x0A, 2,
                                                                    0x08, 10, // SENSOR_TYPE_LOCATION       9
//*/
//*
            // CH 2 Video Sink:
            0x0A, 4 + 4 + 11, 0x08, Channel.AA_CH_VID,
//800f
            0x1A, 4 + 11, // Sink: Video
            0x08, 3,    // int (codec type) 3 = Video
            //0x10, 1,    // int (audio stream type)
//                                                  0x1a, 8,    // f        //I44100 = 0xAC44 = 10    10 1  100 0   100 0100  :  -60, -40, 2
            // 48000 = 0xBB80 = 10    111 0111   000 0000     :  -128, -9, 2
            // 16000 = 0x3E80 = 11 1110 1   000 0000          :  -128, -3

            0x22, 11,   // cz                                                               // Res        FPS, WidMar, HeiMar, DPI
            // DPIs:    (FPS doesn't matter ?)
            0x08, 1, 0x10, 1, 0x18, 0, 0x20, 0, 0x28, -96, 1,   //0x30, 0,     //  800x 480, 30 fps, 0, 0, 160 dpi    0xa0 // Default 160 like 4100NEX
            //0x08, 1, 0x10, 1, 0x18, 0, 0x20, 0, 0x28, -128, 1,   //0x30, 0,     //  800x 480, 30 fps, 0, 0, 128 dpi    0x80 // 160-> 128 Small, phone/music close to outside
            //0x08, 1, 0x10, 1, 0x18, 0, 0x20, 0, 0x28,  -16, 1,   //0x30, 0,     //  800x 480, 30 fps, 0, 0, 240 dpi    0xf0 // 160-> 240 Big, phone/music close to center

            // 60 FPS makes little difference:
            //0x08, 1, 0x10, 2, 0x18, 0, 0x20, 0, 0x28,  -96, 1,   //0x30, 0,     //  800x 480, 60 fps, 0, 0, 160 dpi    0xa0

            // Higher resolutions don't seem to work as of June 10, 2015 release of AA:
            //0x08, 2, 0x10, 1, 0x18, 0, 0x20, 0, 0x28,  -96, 1,   //0x30, 0,     // 1280x 720, 30 fps, 0, 0, 160 dpi    0xa0
            //0x08, 3, 0x10, 1, 0x18, 0, 0x20, 0, 0x28,  -96, 1,   //0x30, 0,     // 1920x1080, 30 fps, 0, 0, 160 dpi    0xa0
//*/
//* Crashes on null Point reference without:
            // CH 3 TouchScreen/Input:
            0x0A, 4 + 2 + 6,//+2+16,
            0x08, Channel.AA_CH_TOU,
//                                                              0x08, -128, -9, 2,    0x10, 16,   0x18, 2,
            //0x28, 0, //1,   boolean
            0x22, 2 + 6,//+2+16, // ak  Input
            //0x0a, 16,   0x03, 0x54, 0x55, 0x56, 0x57, 0x58, 0x7e, 0x7f,   -47, 1,   -127, -128, 4,    -124, -128, 4,
            0x12, 6,        // no int[], am      // 800 = 0x0320 = 11 0    010 0000 : 32+128(-96), 6
            // 480 = 0x01e0 = 1 1     110 0000 =  96+128 (-32), 3
            0x08, -96, 6, 0x10, -32, 3,        //  800x 480
            //0x08, -128, 10,    0x10, -48, 5,        // 1280x 720     0x80, 0x0a   0xd0, 5
            //0x08, -128, 15,    0x10, -72, 8,        // 1920x1080     0x80, 0x0f   0xb8, 8
//*/
//*
            // CH 7 Microphone Audio Source:
            0x0A, 4 + 4 + 7, 0x08, Channel.AA_CH_MIC,
            0x2A, 4 + 7,   // Source: Microphone Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x12, 7,    // AudCfg   16000hz         16bits        1chan
            //0x08, 0x80, 0x7d,         0x10, 0x10,   0x18, 1,
            0x08, -128, 0x7d, 0x10, 0x10, 0x18, 1,
//*/
/*
                        0x0A, 4+4+7+1, 0x08, AA_CH_MIC,
                                       0x2A, 4+7+1, // Source: Microphone Audio
                                                  0x08, 1,    // int (codec type) 1 = Audio
                                                  0x12, 8,    // AudCfg   48000hz         16bits        2chan
                                                                //0x08, 0x80, 0xF7, 0x02,   0x10, 0x10,   0x18, 02,
                                                                0x08, -128,   -9, 0x02,   0x10, 0x10,   0x18, 02,
//*/
/*
                // MediaPlaybackService:
                        0x0A, 4,     0x08, 6,
                                     0x4a, 0,
//*/
//*
            0x12, 4, 'R', 'e', 'i', 'd',//1, 'A', // Car Manuf          Part of "remembered car"
            0x1A, 4, 'A', 'l', 'b', 'e',//1, 'B', // Car Model
            0x22, 4, '2', '0', '1', '6',//1, 'C', // Car Year           Part of "remembered car"
            0x2A, 4, '0', '0', '0', '1',//1, 'D', // Car Serial     Not Part of "remembered car" ??     (vehicleId=null)
            0x30, 1,//0,      // driverPosition
            0x3A, 4, 'M', 'i', 'k', 'e',//1, 'E', // HU  Make / Manuf
            0x42, 4, 'H', 'U', '1', '5',//1, 'F', // HU  Model
            0x4A, 4, 'S', 'W', 'B', '1',//1, 'G', // HU  SoftwareBuild
            0x52, 4, 'S', 'W', 'V', '1',//1, 'H', // HU  SoftwareVersion
            0x58, 0,//1,//1,//0,//1,       // ? bool (or int )    canPlayNativeMediaDuringVr
            0x60, 0,//1,//0,//0,//1        // mHideProjectedClock     1 = True = Hide
            //0x68, 1,
//*/

// 04-22 03:43:38.049 D/CAR.SERVICE( 4306): onCarInfo com.google.android.gms.car.CarInfoInternal[dbId=0,manufacturer=A,model=B,headUnitProtocolVersion=1.1,modelYear=C,vehicleId=null,
// bluetoothAllowed=false,hideProjectedClock=false,driverPosition=0,headUnitMake=E,headUnitModel=F,headUnitSoftwareBuild=G,headUnitSoftwareVersion=H,canPlayNativeMediaDuringVr=false]


//*
            // CH 4 Output Audio Sink:
            0x0A, 4 + 6 + 8, 0x08, Channel.AA_CH_AUD,
            0x1A, 6 + 8, // Sink: Output Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x10, 3,    // Audio Stream Type = 3 = MEDIA
            0x1A, 8,    // AudCfg   48000hz         16bits        2chan
            //0x08, 0x80, 0xF7, 0x02,   0x10, 0x10,   0x18, 02,
            0x08, -128, -9, 0x02, 0x10, 0x10, 0x18, 02,
//*/
//*
            // CH 5 Output Audio Sink1:
            0x0A, 4 + 6 + 7, 0x08, Channel.AA_CH_AU1,
            0x1A, 6 + 7, // Sink: Output Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x10, 1,    // Audio Stream Type = 1 = TTS
            0x1A, 7,    // AudCfg   16000hz         16bits        1chan
            //0x08, 0x80, 0x7d,         0x10, 0x10,   0x18, 1,
            0x08, -128, 0x7d, 0x10, 0x10, 0x18, 1,
//*/
////*
            // CH 6 Output Audio Sink2:
            0x0A, 4 + 6 + 7, 0x08, Channel.AA_CH_AU2,
            0x1A, 6 + 7, // Sink: Output Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x10, 2,    // Audio Stream Type = 2 = SYSTEM
            0x1A, 7,    // AudCfg   16000hz         16bits        1chan
            //0x08, 0x80, 0x7d,         0x10, 0x10,   0x18, 1,
            0x08, -128, 0x7d, 0x10, 0x10, 0x18, 1,
//*/

    };

}