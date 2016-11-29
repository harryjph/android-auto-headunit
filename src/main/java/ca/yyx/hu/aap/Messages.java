package ca.yyx.hu.aap;

import android.bluetooth.BluetoothAdapter;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.utils.ByteArray;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.SensorSourceService;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.MediaSinkService.VideoConfig;
import ca.yyx.hu.aap.protocol.nano.Protocol.Service.InputSourceService.TouchConfig;

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
    static byte[] BYEBYE_RESPONSE = { 0x00, 16, 0x08, 0x00 };

    static byte[] createServiceDiscoveryResponse(String btAddress) {
        Protocol.ServiceDiscoveryResponse carInfo = new Protocol.ServiceDiscoveryResponse();
        carInfo.services = new Protocol.Service[8];
        carInfo.make = "AACar";
        carInfo.model = "0001";
        carInfo.year = "2016";
        carInfo.headUnitModel = "ChangAn S";
        carInfo.headUnitMake = "Roadrover";
        carInfo.headUnitSoftwareBuild = "SWB1";
        carInfo.headUnitSoftwareVersion = "SWV1";
        carInfo.driverPosition = true;

        Service sensors = new Service();
        Service video = new Service();
        Service touch = new Service();
        Service mic = new Service();
        Service audio0 = new Service();
        Service audio1 = new Service();
        Service audio2 = new Service();
        Service bluetooth = new Service();

        carInfo.services[0] = sensors;
        carInfo.services[1] = video;
        carInfo.services[2] = touch;
        carInfo.services[3] = mic;
        carInfo.services[4] = audio0;
        carInfo.services[5] = audio1;
        carInfo.services[6] = audio2;
        carInfo.services[7] = bluetooth;

        sensors.id = Channel.AA_CH_SEN;
        sensors.sensorSourceService = new SensorSourceService();
        sensors.sensorSourceService.sensors = new SensorSourceService.Sensor[2];
        sensors.sensorSourceService.sensors[0] = new SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[0].type = Protocol.SENSOR_TYPE_DRIVING_STATUS;
        sensors.sensorSourceService.sensors[1] = new SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[1].type = Protocol.SENSOR_TYPE_NIGHT_DATA;

        video.id = Channel.AA_CH_VID;
        video.mediaSinkService = new Service.MediaSinkService();
        video.mediaSinkService.availableType = Protocol.STREAM_TYPE_VIDEO;
        video.mediaSinkService.availableWhileInCall = true;
        video.mediaSinkService.videoConfigs = new VideoConfig[1];
        VideoConfig videoConfig = new VideoConfig();
        videoConfig.resolution = VideoConfig.VIDEO_RESOLUTION_800x480;
        videoConfig.frameRate = VideoConfig.VIDEO_FPS_60;
        videoConfig.dpi = 160;
        video.mediaSinkService.videoConfigs[0] = videoConfig;

        touch.id = Channel.AA_CH_TOU;
        touch.inputSourceService = new Service.InputSourceService();
        touch.inputSourceService.touchscreen = new TouchConfig();
        touch.inputSourceService.touchscreen.width = 800;
        touch.inputSourceService.touchscreen.height = 480;

        mic.id = Channel.AA_CH_MIC;
        mic.mediaSourceService = new Service.MediaSourceService();
        mic.mediaSourceService.type = Protocol.STREAM_TYPE_AUDIO;
        Protocol.AudioConfiguration micConfig = new Protocol.AudioConfiguration();
        micConfig.sampleRate = 16000;
        micConfig.numberOfBits = 16;
        micConfig.numberOfChannels = 1;
        mic.mediaSourceService.audioConfig = micConfig;

        audio0.id = Channel.AA_CH_AUD;
        audio0.mediaSinkService = new Service.MediaSinkService();
        audio0.mediaSinkService.availableType = Protocol.STREAM_TYPE_AUDIO;
        audio0.mediaSinkService.audioType = AudioConfigs.getStreamType(Channel.AA_CH_AUD);
        audio0.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio0.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.AA_CH_AUD);

        audio1.id = Channel.AA_CH_AU1;
        audio1.mediaSinkService = new Service.MediaSinkService();
        audio1.mediaSinkService.availableType = Protocol.STREAM_TYPE_AUDIO;
        audio1.mediaSinkService.audioType = AudioConfigs.getStreamType(Channel.AA_CH_AU1);
        audio1.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio1.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.AA_CH_AU1);

        audio2.id = Channel.AA_CH_AU2;
        audio2.mediaSinkService = new Service.MediaSinkService();
        audio2.mediaSinkService.availableType = Protocol.STREAM_TYPE_AUDIO;
        audio2.mediaSinkService.audioType = AudioConfigs.getStreamType(Channel.AA_CH_AU2);
        audio2.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio2.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.AA_CH_AU2);

        bluetooth.id = Channel.AA_CH_BTH;
        bluetooth.bluetoothService = new Service.BluetoothService();
        bluetooth.bluetoothService.carAddress = btAddress;
        bluetooth.bluetoothService.supportedPairingMethods = new int[] { 2, 3 };

        byte[] result = new byte[carInfo.getSerializedSize() + 2];
        // Header
        result[0] = 0x00;
        result[1] = 0x06;
        MessageNano.toByteArray(carInfo, result, 2, carInfo.getSerializedSize());


        return result;
    }

}
