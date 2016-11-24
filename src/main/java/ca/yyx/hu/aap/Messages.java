package ca.yyx.hu.aap;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.utils.ByteArray;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.aap.protocol.nano.Protocol.ChannelDescriptor;
import ca.yyx.hu.aap.protocol.nano.Protocol.ChannelDescriptor.SensorChannel;
import ca.yyx.hu.aap.protocol.nano.Protocol.ChannelDescriptor.OutputStreamChannel.VideoConfig;
import ca.yyx.hu.aap.protocol.nano.Protocol.ChannelDescriptor.InputEventChannel.TouchScreenConfig;

/**
 * @author algavris
 * @date 08/06/2016.
 */

public class Messages {
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

    static final int DRIVE_STATUS_FULLY_RESTRICTED = 31;
    static final int DRIVE_STATUS_LIMIT_MESSAGE_LEN = 16;
    static final int DRIVE_STATUS_NO_CONFIG = 8;
    static final int DRIVE_STATUS_NO_KEYBOARD_INPUT = 2;
    static final int DRIVE_STATUS_NO_VIDEO = 1;
    static final int DRIVE_STATUS_NO_VOICE_INPUT = 4;
    static final int DRIVE_STATUS_UNRESTRICTED = 0;
    static final int GEAR_DRIVE = 100;

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
//
//        Protocol.TouchInfo touchInfo = new Protocol.TouchInfo();
//        touchInfo.location = new Protocol.TouchInfo.Location[] {
//                new Protocol.TouchInfo.Location(), // x
//                new Protocol.TouchInfo.Location(), // y
//                new Protocol.TouchInfo.Location()  // z
//        };
//        touchInfo.location[0].x

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
    static byte[] DRIVING_STATUS = {(byte) 0x80, 0x03, 0x6a, 0x02, 0x08, 0};
    static byte[] NIGHT_MODE = {(byte) 0x80, 0x03, 0x52, 0x02, 0x08, 0};
    static byte[] NAVIGATION_FOCUS = {0, 14, 0x08, 2};
    static byte[] BYEBYE_RESPONSE = { 0x00, 16, 0x08, 0x00 };

    static byte[] createServiceDiscoveryResponse() {
        Protocol.ServiceDiscoveryResponse carInfo = new Protocol.ServiceDiscoveryResponse();
        carInfo.headUnitName = "Roadrover CS";
        carInfo.headunitModel = "ChangAn S";
        carInfo.headunitMake = "Roadrover";
        carInfo.carModel = "AACar";
        carInfo.carSerial = "0001";
        carInfo.carYear = "2016";
        carInfo.driverPos = true;
        carInfo.swBuild = "SWB1";
        carInfo.swVersion = "SWV1";
        carInfo.channels = new Protocol.ChannelDescriptor[7];

        ChannelDescriptor sensors = new ChannelDescriptor();
        ChannelDescriptor video = new ChannelDescriptor();
        ChannelDescriptor touch = new ChannelDescriptor();
        ChannelDescriptor mic = new ChannelDescriptor();
        ChannelDescriptor audio0 = new ChannelDescriptor();
        ChannelDescriptor audio1 = new ChannelDescriptor();
        ChannelDescriptor audio2 = new ChannelDescriptor();

        carInfo.channels[0] = sensors;
        carInfo.channels[1] = video;
        carInfo.channels[2] = touch;
        carInfo.channels[3] = mic;
        carInfo.channels[4] = audio0;
        carInfo.channels[5] = audio1;
        carInfo.channels[6] = audio2;

        sensors.channelId = Channel.AA_CH_SEN;
        sensors.sensorChannel = new SensorChannel();
        sensors.sensorChannel.sensorList = new SensorChannel.Sensor[2];
        sensors.sensorChannel.sensorList[0] = new SensorChannel.Sensor();
        sensors.sensorChannel.sensorList[1] = new SensorChannel.Sensor();
        sensors.sensorChannel.sensorList[0].type = Protocol.SENSOR_TYPE_DRIVING_STATUS;
        sensors.sensorChannel.sensorList[1].type = Protocol.SENSOR_TYPE_NIGHT_DATA;

        video.channelId = Channel.AA_CH_VID;
        video.outputStreamChannel = new ChannelDescriptor.OutputStreamChannel();
        video.outputStreamChannel.type = Protocol.STREAM_TYPE_VIDEO;
        video.outputStreamChannel.availableWhileInCall = true;
        video.outputStreamChannel.videoConfigs = new VideoConfig[1];
        VideoConfig videoConfig = new VideoConfig();
        videoConfig.resolution = VideoConfig.VIDEO_RESOLUTION_800x480;
        videoConfig.frameRate = VideoConfig.VIDEO_FPS_60;
        videoConfig.dpi = 160;
        video.outputStreamChannel.videoConfigs[0] = videoConfig;

        touch.channelId = Channel.AA_CH_TOU;
        touch.inputEventChannel = new ChannelDescriptor.InputEventChannel();
        touch.inputEventChannel.touchScreenConfig = new TouchScreenConfig();
        touch.inputEventChannel.touchScreenConfig.width = 800;
        touch.inputEventChannel.touchScreenConfig.height = 480;

        mic.channelId = Channel.AA_CH_MIC;
        mic.inputStreamChannel = new ChannelDescriptor.InputStreamChannel();
        mic.inputStreamChannel.type = Protocol.STREAM_TYPE_AUDIO;
        Protocol.AudioConfig micConfig = new Protocol.AudioConfig();
        micConfig.sampleRate = 16000;
        micConfig.bitDepth = 16;
        micConfig.channelCount = 1;
        mic.inputStreamChannel.audioConfig = micConfig;

        audio0.channelId = Channel.AA_CH_AUD;
        audio0.outputStreamChannel = new ChannelDescriptor.OutputStreamChannel();
        audio0.outputStreamChannel.type = Protocol.STREAM_TYPE_AUDIO;
        audio0.outputStreamChannel.audioType = Protocol.AUDIO_TYPE_MEDIA;
        audio0.outputStreamChannel.audioConfigs = new Protocol.AudioConfig[1];
        Protocol.AudioConfig audioConfig0 = new Protocol.AudioConfig();
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48;
        audioConfig0.bitDepth = 16;
        audioConfig0.channelCount = 2;
        audio0.outputStreamChannel.audioConfigs[0] = audioConfig0;

        audio1.channelId = Channel.AA_CH_AU1;
        audio1.outputStreamChannel = new ChannelDescriptor.OutputStreamChannel();
        audio1.outputStreamChannel.type = Protocol.STREAM_TYPE_AUDIO;
        audio1.outputStreamChannel.audioType = Protocol.AUDIO_TYPE_SPEECH;
        audio1.outputStreamChannel.audioConfigs = new Protocol.AudioConfig[1];
        Protocol.AudioConfig audioConfig1 = new Protocol.AudioConfig();
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig1.bitDepth = 16;
        audioConfig1.channelCount = 1;
        audio1.outputStreamChannel.audioConfigs[0] = audioConfig1;

        audio2.channelId = Channel.AA_CH_AU2;
        audio2.outputStreamChannel = new ChannelDescriptor.OutputStreamChannel();
        audio2.outputStreamChannel.type = Protocol.STREAM_TYPE_AUDIO;
        audio2.outputStreamChannel.audioType = Protocol.AUDIO_TYPE_SYSTEM;
        audio2.outputStreamChannel.audioConfigs = new Protocol.AudioConfig[1];
        Protocol.AudioConfig audioConfig2 = new Protocol.AudioConfig();
        audioConfig2.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig2.bitDepth = 16;
        audioConfig2.channelCount = 1;
        audio2.outputStreamChannel.audioConfigs[0] = audioConfig2;

        byte[] result = new byte[carInfo.getSerializedSize() + 2];
        // Header
        result[0] = 0x00;
        result[1] = 0x06;
        MessageNano.toByteArray(carInfo, result, 2, carInfo.getSerializedSize());
        return result;
    }
}
