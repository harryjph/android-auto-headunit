package ca.yyx.hu.aap.protocol.messages

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.KeyCode
import ca.yyx.hu.aap.protocol.AudioConfigs
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.nano.Protocol
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Settings
import com.google.protobuf.nano.MessageNano

/**
 * @author alex gavrishev
 *
 * @date 13/02/2017.
 */
class ServiceDiscoveryResponse(settings: Settings)
    : AapMessage(Channel.ID_CTR, MsgType.Control.SERVICEDISCOVERYRESPONSE, ServiceDiscoveryResponse.makeProto(settings)) {

    companion object {
        private fun makeProto(settings: Settings): MessageNano {
            val carInfo = Protocol.ServiceDiscoveryResponse()
            carInfo.make = "AACar"
            carInfo.model = "0001"
            carInfo.year = "2016"
            carInfo.headUnitModel = "ChangAn S"
            carInfo.headUnitMake = "Roadrover"
            carInfo.headUnitSoftwareBuild = "SWB1"
            carInfo.headUnitSoftwareVersion = "SWV1"
            carInfo.driverPosition = true

            val services = mutableListOf<Protocol.Service>()

            val sensors = Protocol.Service()
            sensors.id = Channel.ID_SEN
            sensors.sensorSourceService = Protocol.Service.SensorSourceService()

            val sensorTypes = mutableListOf<Protocol.Service.SensorSourceService.Sensor>()
            sensorTypes.add(makeSensorType(Protocol.SENSOR_TYPE_DRIVING_STATUS))
            sensorTypes.add(makeSensorType(Protocol.SENSOR_TYPE_LOCATION))
            if (settings.nightMode != Settings.NightMode.NONE){
                sensorTypes.add(makeSensorType(Protocol.SENSOR_TYPE_NIGHT))
            }

            sensors.sensorSourceService.sensors = sensorTypes.toTypedArray()

            services.add(sensors)

            val video = Protocol.Service()
            video.id = Channel.ID_VID
            video.mediaSinkService = Protocol.Service.MediaSinkService()
            video.mediaSinkService.availableType = Protocol.MEDIA_CODEC_VIDEO
            video.mediaSinkService.availableWhileInCall = true
            video.mediaSinkService.videoConfigs = arrayOfNulls<Protocol.Service.MediaSinkService.VideoConfiguration>(1)
            val videoConfig = Protocol.Service.MediaSinkService.VideoConfiguration()
            videoConfig.codecResolution = Protocol.Service.MediaSinkService.VideoConfiguration.VIDEO_RESOLUTION_800x480
            videoConfig.frameRate = Protocol.Service.MediaSinkService.VideoConfiguration.VIDEO_FPS_60
            videoConfig.density = 140
            video.mediaSinkService.videoConfigs[0] = videoConfig
            services.add(video)

            val input = Protocol.Service()
            input.id = Channel.ID_INP
            input.inputSourceService = Protocol.Service.InputSourceService()
            input.inputSourceService.touchscreen = Protocol.Service.InputSourceService.TouchConfig()
            input.inputSourceService.touchscreen.width = 800
            input.inputSourceService.touchscreen.height = 480
            input.inputSourceService.keycodesSupported = KeyCode.supported()
            services.add(input)

            val audio1 = Protocol.Service()
            audio1.id = Channel.ID_AU1
            audio1.mediaSinkService = Protocol.Service.MediaSinkService()
            audio1.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO
            audio1.mediaSinkService.audioType = Protocol.CAR_STREAM_SYSTEM
            audio1.mediaSinkService.audioConfigs = arrayOfNulls<Protocol.AudioConfiguration>(1)
            audio1.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AU1)
            services.add(audio1)

            val audio2 = Protocol.Service()
            audio2.id = Channel.ID_AU2
            audio2.mediaSinkService = Protocol.Service.MediaSinkService()
            audio2.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO
            audio2.mediaSinkService.audioType = Protocol.CAR_STREAM_VOICE
            audio2.mediaSinkService.audioConfigs = arrayOfNulls<Protocol.AudioConfiguration>(1)
            audio2.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AU2)
            services.add(audio2)

            val audio0 = Protocol.Service()
            audio0.id = Channel.ID_AUD
            audio0.mediaSinkService = Protocol.Service.MediaSinkService()
            audio0.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO
            audio0.mediaSinkService.audioType = Protocol.CAR_STREAM_MEDIA
            audio0.mediaSinkService.audioConfigs = arrayOfNulls<Protocol.AudioConfiguration>(1)
            audio0.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AUD)
            services.add(audio0)

            val mic = Protocol.Service()
            mic.id = Channel.ID_MIC
            mic.mediaSourceService = Protocol.Service.MediaSourceService()
            mic.mediaSourceService.type = Protocol.MEDIA_CODEC_AUDIO
            val micConfig = Protocol.AudioConfiguration()
            micConfig.sampleRate = 16000
            micConfig.numberOfBits = 16
            micConfig.numberOfChannels = 1
            mic.mediaSourceService.audioConfig = micConfig
            services.add(mic)

            if (settings.bluetoothAddress.isNotEmpty()) {
                val bluetooth = Protocol.Service()
                bluetooth.id = Channel.ID_BTH
                bluetooth.bluetoothService = Protocol.Service.BluetoothService()
                bluetooth.bluetoothService.carAddress = settings.bluetoothAddress
                bluetooth.bluetoothService.supportedPairingMethods = intArrayOf(Protocol.Service.BluetoothService.BLUETOOTH_PARING_METHOD_HFP)
                services.add(bluetooth)
            } else {
                AppLog.i("BT MAC Address is null. Skip bluetooth service")
            }

            carInfo.services = services.toTypedArray()

            return carInfo
        }

        private fun makeSensorType(type: Int): Protocol.Service.SensorSourceService.Sensor {
            val sensor = Protocol.Service.SensorSourceService.Sensor()
            sensor.type = type
            return sensor
        }
    }
}
