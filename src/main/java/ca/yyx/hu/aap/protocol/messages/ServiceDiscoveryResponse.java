package ca.yyx.hu.aap.protocol.messages;

import com.google.protobuf.nano.MessageNano;

import java.util.ArrayList;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.KeyCode;
import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 13/02/2017.
 */

public class ServiceDiscoveryResponse extends AapMessage {

    private static MessageNano makeProto(String btAddress) {
        Protocol.ServiceDiscoveryResponse carInfo = new Protocol.ServiceDiscoveryResponse();
        carInfo.make = "AACar";
        carInfo.model = "0001";
        carInfo.year = "2016";
        carInfo.headUnitModel = "ChangAn S";
        carInfo.headUnitMake = "Roadrover";
        carInfo.headUnitSoftwareBuild = "SWB1";
        carInfo.headUnitSoftwareVersion = "SWV1";
        carInfo.driverPosition = true;

        ArrayList<Protocol.Service> services = new ArrayList<>();

        Protocol.Service sensors = new Protocol.Service();
        sensors.id = Channel.ID_SEN;
        sensors.sensorSourceService = new Protocol.Service.SensorSourceService();
        sensors.sensorSourceService.sensors = new Protocol.Service.SensorSourceService.Sensor[2];
        sensors.sensorSourceService.sensors[0] = new Protocol.Service.SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[0].type = Protocol.SENSOR_TYPE_DRIVING_STATUS;
        sensors.sensorSourceService.sensors[1] = new Protocol.Service.SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[1].type = Protocol.SENSOR_TYPE_NIGHT;

        services.add(sensors);

        Protocol.Service video = new Protocol.Service();
        video.id = Channel.ID_VID;
        video.mediaSinkService = new Protocol.Service.MediaSinkService();
        video.mediaSinkService.availableType = Protocol.MEDIA_CODEC_VIDEO;
        video.mediaSinkService.availableWhileInCall = true;
        video.mediaSinkService.videoConfigs = new Protocol.Service.MediaSinkService.VideoConfiguration[1];
        Protocol.Service.MediaSinkService.VideoConfiguration videoConfig = new Protocol.Service.MediaSinkService.VideoConfiguration();
        videoConfig.codecResolution = Protocol.Service.MediaSinkService.VideoConfiguration.VIDEO_RESOLUTION_800x480;
        videoConfig.frameRate = Protocol.Service.MediaSinkService.VideoConfiguration.VIDEO_FPS_60;
        videoConfig.density = 140;
        video.mediaSinkService.videoConfigs[0] = videoConfig;
        services.add(video);

        Protocol.Service input = new Protocol.Service();
        input.id = Channel.ID_INP;
        input.inputSourceService = new Protocol.Service.InputSourceService();
        input.inputSourceService.touchscreen = new Protocol.Service.InputSourceService.TouchConfig();
        input.inputSourceService.touchscreen.width = 800;
        input.inputSourceService.touchscreen.height = 480;
        input.inputSourceService.keycodesSupported = KeyCode.supported();
        services.add(input);

        Protocol.Service audio1 = new Protocol.Service();
        audio1.id = Channel.ID_AU1;
        audio1.mediaSinkService = new Protocol.Service.MediaSinkService();
        audio1.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio1.mediaSinkService.audioType = Protocol.CAR_STREAM_SYSTEM;
        audio1.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio1.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AU1);
        services.add(audio1);

        Protocol.Service audio2 = new Protocol.Service();
        audio2.id = Channel.ID_AU2;
        audio2.mediaSinkService = new Protocol.Service.MediaSinkService();
        audio2.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio2.mediaSinkService.audioType = Protocol.CAR_STREAM_VOICE;
        audio2.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio2.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AU2);
        services.add(audio2);

        Protocol.Service audio0 = new Protocol.Service();
        audio0.id = Channel.ID_AUD;
        audio0.mediaSinkService = new Protocol.Service.MediaSinkService();
        audio0.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio0.mediaSinkService.audioType = Protocol.CAR_STREAM_MEDIA;
        audio0.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        audio0.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AUD);
        services.add(audio0);

        Protocol.Service mic = new Protocol.Service();
        mic.id = Channel.ID_MIC;
        mic.mediaSourceService = new Protocol.Service.MediaSourceService();
        mic.mediaSourceService.type = Protocol.MEDIA_CODEC_AUDIO;
        Protocol.AudioConfiguration micConfig = new Protocol.AudioConfiguration();
        micConfig.sampleRate = 16000;
        micConfig.numberOfBits = 16;
        micConfig.numberOfChannels = 1;
        mic.mediaSourceService.audioConfig = micConfig;
        services.add(mic);

        if (btAddress != null) {
            Protocol.Service bluetooth = new Protocol.Service();
            bluetooth.id = Channel.ID_BTH;
            bluetooth.bluetoothService = new Protocol.Service.BluetoothService();
            bluetooth.bluetoothService.carAddress = btAddress;
            bluetooth.bluetoothService.supportedPairingMethods = new int[] {
                    Protocol.Service.BluetoothService.BLUETOOTH_PARING_METHOD_HFP
            };
            services.add(bluetooth);
        } else {
            AppLog.i("BT MAC Address is null. Skip bluetooth service");
        }

        carInfo.services = services.toArray(new Protocol.Service[0]);

        return carInfo;

    }

    public ServiceDiscoveryResponse(String btAddress)
    {
        super(Channel.ID_CTR, MsgType.Control.SERVICEDISCOVERYRESPONSE, makeProto(btAddress));
    }
}
