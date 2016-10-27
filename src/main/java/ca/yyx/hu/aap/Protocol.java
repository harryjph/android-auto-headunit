package ca.yyx.hu.aap;

import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.utils.ByteArray;

/**
 * @author algavris
 * @date 08/06/2016.
 */

public class Protocol {
    static final int DEF_BUFFER_LENGTH = 131080;

    static final int BTN_UP = 0x13;
    static final int BTN_DOWN = 0x14;
    static final int BTN_LEFT = 0x15;
    static final int BTN_RIGHT = 0x16;
    static final int BTN_BACK = 0x04;
    static final int BTN_ENTER = 0x17;
    static final int BTN_MIC = 0x54;
    static final int BTN_PHONE = 0x5;
    static final int BTN_START = 126;

    public static final int BTN_PLAYPAUSE = 0x55;
    public static final int BTN_NEXT = 0x57;
    public static final int BTN_PREV = 0x58;
    public static final int BTN_STOP = 127;
/*
    static final int DRIVE_STATUS_FULLY_RESTRICTED = 31;
    static final int DRIVE_STATUS_LIMIT_MESSAGE_LEN = 16;
    static final int DRIVE_STATUS_NO_CONFIG = 8;
    static final int DRIVE_STATUS_NO_KEYBOARD_INPUT = 2;
    static final int DRIVE_STATUS_NO_VIDEO = 1;
    static final int DRIVE_STATUS_NO_VOICE_INPUT = 4;
    static final int DRIVE_STATUS_UNRESTRICTED = 0;
    static final int GEAR_DRIVE = 100;
 */

    static ByteArray createMessage(int chan, int flags, int type, byte[] data, int size) {

        ByteArray buffer = new ByteArray(6 + size);

        buffer.put(chan, flags);

        if (type >= 0) {
            buffer.encodeInt(size + 2);
            // If type not negative, which indicates encrypted type should not be touched...
            buffer.encodeInt(type);
        } else {
            buffer.encodeInt(size);
        }

        buffer.put(data, size);
        return buffer;
    }

    static ByteArray createButtonMessage(long timeStamp, int button, boolean isPress)
    {
        ByteArray buffer = new ByteArray(22);

        buffer.put(0x80, 0x01, 0x08);
        int size = Encode.longToByteArray(timeStamp, buffer.data, buffer.length);
        buffer.move(size);

        int press = isPress ? 0x01 : 0x00;
        buffer.put(0x22, 0x0A, 0x0A, 0x08, 0x08, button, 0x10, press, 0x18, 0x00, 0x20, 0x00);
        return buffer;
    }

    static ByteArray createTouchMessage(long timeStamp, byte action, int x, int y) {
        ByteArray buffer = new ByteArray(32);

        buffer.put(0x80, 0x01, 0x08);

        int size = Encode.longToByteArray(timeStamp, buffer.data, buffer.length);          // Encode timestamp
        buffer.move(size);

        int size1_idx = buffer.length + 1;
        int size2_idx = buffer.length + 3;

        buffer.put(0x1a, 0x09, 0x0a, 0x03);

        /* Set magnitude of each axis */
        byte axis = 0;
        int[] coordinates = {x, y, 0};
        for (int i=0; i<3; i++) {
            axis += 0x08; //0x08, 0x10, 0x18
            buffer.put(axis);
            size = Encode.intToByteArray(coordinates[i], buffer.data, buffer.length);
            buffer.move(size);
            buffer.inc(size1_idx, size);
            buffer.inc(size2_idx, size);
        }

        buffer.put(0x10, 0x00, 0x18, action);
        return buffer;
    }

    static byte[] createNightModeMessage(boolean enabled) {
        byte[] buffer = new byte[6];

        buffer[0] = -128;
        buffer[1] = 0x03;
        buffer[2] = 0x52;
        buffer[3] = 0x02;
        buffer[4] = 0x08;
        if (enabled)
            buffer[5] = 0x01;
        else
            buffer[5]= 0x00;

        return buffer;
    }

    static byte[] VERSION_REQUEST = { 0, 1, 0, 1 };
    static byte[] BYEBYE_REQUEST = { 0x00, 0x0f, 0x08, 0x00 };
    // Driving Status: 0 = Parked, 1 = Moving
    static byte[] DRIVING_STATUS = {(byte) 0x80, 0x03, 0x6a, 2, 8, 0};
    static byte[] NIGHT_MODE = {(byte) 0x80, 0x03, 0x52, 0x02, 0x08, 0};
    static byte[] NAVIGATION_FOCUS = {0, 14, 0x08, 2};
    static byte[] BYEBYE_RESPONSE = { 0x00, 16, 0x08, 0x00 };

    static byte[] SERVICE_DISCOVERY = {0, 6,        //8, 0};                                            // Svc Disc Rsp = 6
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

            // CH 1 Sensors:
            0x0A, 4 + 4 * 2,//co: int, cm/cn[]
            0x08, Channel.AA_CH_SEN,
            0x12, 4 * 2,
            0x0A, 2,
            0x08, 11, // SENSOR_TYPE_DRIVING_STATUS 12
            0x0A, 2,
            0x08, 10, // SENSOR_TYPE_NIGHT_DATA 10

/*  Requested Sensors: 10, 9, 2, 7, 6:
            0x0A, 4 + 4*6,     //co: int, cm/cn[]
            0x08, AA_CH_SEN,  0x12, 4*6,
            0x0A, 2, 0x08, 11, // SENSOR_TYPE_DRIVING_STATUS 12
            0x0A, 2, 0x08,  3, // SENSOR_TYPE_RPM            2
            0x0A, 2, 0x08,  8, // SENSOR_TYPE_DIAGNOSTICS    7
            0x0A, 2, 0x08,  7, // SENSOR_TYPE_GEAR           6
            0x0A, 2, 0x08,  1, // SENSOR_TYPE_COMPASS       10
            0x0A, 2, 0x08, 10, // SENSOR_TYPE_LOCATION       9
*/
            // CH 2 Video Sink:
            0x0A, 4 + 4 + 11, 0x08, Channel.AA_CH_VID,
            0x1A, 4 + 11, // Sink: Video
            0x08, 3,    // int (codec type) 3 = Video
            // 0x10, 1,    // int (audio stream type)
            // 0x1a, 8,    // f        //I44100 = 0xAC44 = 10    10 1  100 0   100 0100  :  -60, -40, 2
            // 48000 = 0xBB80 = 10    111 0111   000 0000     :  -128, -9, 2
            // 16000 = 0x3E80 = 11 1110 1   000 0000          :  -128, -3

            0x22, 11,   // cz                                                               // Res        FPS, WidMar, HeiMar, DPI
            // DPIs:    (FPS doesn't matter ?)
            0x08, 1, 0x10, 1, 0x18, 0, 0x20, 0, 0x28, -96, 1,   //0x30, 0,     //  800x 480, 30 fps, 0, 0, 160 dpi    0xa0 // Default 160 like 4100NEX
            // 0x08, 1, 0x10, 1, 0x18, 0, 0x20, 0, 0x28, -128, 1,   //0x30, 0,     //  800x 480, 30 fps, 0, 0, 128 dpi    0x80 // 160-> 128 Small, phone/music close to outside
            // 0x08, 1, 0x10, 1, 0x18, 0, 0x20, 0, 0x28,  -16, 1,   //0x30, 0,     //  800x 480, 30 fps, 0, 0, 240 dpi    0xf0 // 160-> 240 Big, phone/music close to center

            // 60 FPS makes little difference:
            // 0x08, 1, 0x10, 2, 0x18, 0, 0x20, 0, 0x28,  -96, 1,   //0x30, 0,     //  800x 480, 60 fps, 0, 0, 160 dpi    0xa0

            // Higher resolutions don't seem to work as of June 10, 2015 release of AA:
            // 0x08, 2, 0x10, 1, 0x18, 0, 0x20, 0, 0x28,  -96, 1,   //0x30, 0,     // 1280x 720, 30 fps, 0, 0, 160 dpi    0xa0
            // 0x08, 3, 0x10, 1, 0x18, 0, 0x20, 0, 0x28,  -96, 1,   //0x30, 0,     // 1920x1080, 30 fps, 0, 0, 160 dpi    0xa0

            // CH 3 TouchScreen/Input:
            0x0A, 4 + 2 + 6,//+2+16,
            0x08, Channel.AA_CH_TOU,
            // 0x08, -128, -9, 2,    0x10, 16,   0x18, 2,
            // 0x28, 0, //1,   boolean
            0x22, 2 + 6,//+2+16, // ak  Input
            // 0x0a, 16,   0x03, 0x54, 0x55, 0x56, 0x57, 0x58, 0x7e, 0x7f,   -47, 1,   -127, -128, 4,    -124, -128, 4,
            0x12, 6,        // no int[], am      // 800 = 0x0320 = 11 0    010 0000 : 32+128(-96), 6
            // 480 = 0x01e0 = 1 1     110 0000 =  96+128 (-32), 3
            0x08, -96, 6, 0x10, -32, 3,        //  800x 480
            // 0x08, -128, 10,    0x10, -48, 5,        // 1280x 720     0x80, 0x0a   0xd0, 5
            // 0x08, -128, 15,    0x10, -72, 8,        // 1920x1080     0x80, 0x0f   0xb8, 8

            // CH 7 Microphone Audio Source:
            0x0A, 4 + 4 + 7, 0x08, Channel.AA_CH_MIC,
            0x2A, 4 + 7,   // Source: Microphone Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x12, 7,    // AudCfg   16000hz         16bits        1chan
            //0x08, 0x80, 0x7d,         0x10, 0x10,   0x18, 1,
            0x08, -128, 0x7d, 0x10, 0x10, 0x18, 1,

            /*
            0x0A, 4+4+7+1, 0x08, AA_CH_MIC,
            0x2A, 4+7+1, // Source: Microphone Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x12, 8,    // AudCfg   48000hz         16bits        2chan
            //0x08, 0x80, 0xF7, 0x02,   0x10, 0x10,   0x18, 02,
            0x08, -128,   -9, 0x02,   0x10, 0x10,   0x18, 02,
            */

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

// 04-22 03:43:38.049 D/CAR.SERVICE( 4306): onCarInfo com.google.android.gms.car.CarInfoInternal[dbId=0,manufacturer=A,model=B,headUnitProtocolVersion=1.1,modelYear=C,vehicleId=null,
// bluetoothAllowed=false,hideProjectedClock=false,driverPosition=0,headUnitMake=E,headUnitModel=F,headUnitSoftwareBuild=G,headUnitSoftwareVersion=H,canPlayNativeMediaDuringVr=false]

            // CH 4 Output Audio Sink:
            0x0A, 4 + 6 + 8, 0x08, Channel.AA_CH_AUD,
            0x1A, 6 + 8, // Sink: Output Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x10, 3,    // Audio Stream Type = 3 = MEDIA
            0x1A, 8,    // AudCfg   48000hz         16bits        2chan
            //0x08, 0x80, 0xF7, 0x02,   0x10, 0x10,   0x18, 02,
            0x08, -128, -9, 0x02, 0x10, 0x10, 0x18, 02,

            // CH 5 Output Audio Sink1:
            0x0A, 4 + 6 + 7, 0x08, Channel.AA_CH_AU1,
            0x1A, 6 + 7, // Sink: Output Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x10, 1,    // Audio Stream Type = 1 = TTS
            0x1A, 7,    // AudCfg   16000hz         16bits        1chan
            //0x08, 0x80, 0x7d,         0x10, 0x10,   0x18, 1,
            0x08, -128, 0x7d, 0x10, 0x10, 0x18, 1,

            // CH 6 Output Audio Sink2:
            0x0A, 4 + 6 + 7, 0x08, Channel.AA_CH_AU2,
            0x1A, 6 + 7, // Sink: Output Audio
            0x08, 1,    // int (codec type) 1 = Audio
            0x10, 2,    // Audio Stream Type = 2 = SYSTEM
            0x1A, 7,    // AudCfg   16000hz         16bits        1chan
            //0x08, 0x80, 0x7d,         0x10, 0x10,   0x18, 1,
            0x08, -128, 0x7d, 0x10, 0x10, 0x18, 1,
    };

}
