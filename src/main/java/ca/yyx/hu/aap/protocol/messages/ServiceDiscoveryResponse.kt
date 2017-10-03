package ca.yyx.hu.aap.protocol.messages

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.KeyCode
import ca.yyx.hu.aap.protocol.AudioConfigs
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.Screen
import ca.yyx.hu.aap.protocol.nano.Control
import ca.yyx.hu.aap.protocol.nano.Media
import ca.yyx.hu.aap.protocol.nano.Sensors
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.Settings
import com.google.protobuf.nano.MessageNano

/**
 * @author alex gavrishev
 *
 * @date 13/02/2017.
 */
class ServiceDiscoveryResponse(settings: Settings)
    : AapMessage(Channel.ID_CTR, Control.MSG_CONTROL_SERVICEDISCOVERYRESPONSE, ServiceDiscoveryResponse.makeProto(settings)) {

    companion object {
        private fun makeProto(settings: Settings): MessageNano {
            val carInfo = Control.ServiceDiscoveryResponse()
            carInfo.make = "AACar"
            carInfo.model = "0001"
            carInfo.year = "2016"
            carInfo.headUnitModel = "ChangAn S"
            carInfo.headUnitMake = "Roadrover"
            carInfo.headUnitSoftwareBuild = "SWB1"
            carInfo.headUnitSoftwareVersion = "SWV1"
            carInfo.driverPosition = true

            val services = mutableListOf<Control.Service>()

            val sensors = Control.Service()
            sensors.id = Channel.ID_SEN
            sensors.sensorSourceService = Control.Service.SensorSourceService()

            val sensorTypes = mutableListOf<Control.Service.SensorSourceService.Sensor>()
            sensorTypes.add(makeSensorType(Sensors.SENSOR_TYPE_DRIVING_STATUS))
            if (settings.useGpsForNavigation) {
                sensorTypes.add(makeSensorType(Sensors.SENSOR_TYPE_LOCATION))
            }
            if (settings.nightMode != Settings.NightMode.NONE){
                sensorTypes.add(makeSensorType(Sensors.SENSOR_TYPE_NIGHT))
            }

            sensors.sensorSourceService.sensors = sensorTypes.toTypedArray()

            services.add(sensors)

            val video = Control.Service()
            video.id = Channel.ID_VID
            video.mediaSinkService = Control.Service.MediaSinkService()
            video.mediaSinkService.availableType = Media.MEDIA_CODEC_VIDEO
            video.mediaSinkService.availableWhileInCall = true
            video.mediaSinkService.videoConfigs = arrayOfNulls<Control.Service.MediaSinkService.VideoConfiguration>(1)
            val videoConfig = Control.Service.MediaSinkService.VideoConfiguration()
            videoConfig.codecResolution = Control.Service.MediaSinkService.VideoConfiguration.VIDEO_RESOLUTION_800x480
            videoConfig.frameRate = Control.Service.MediaSinkService.VideoConfiguration.VIDEO_FPS_60
            videoConfig.density = Screen.density
            video.mediaSinkService.videoConfigs[0] = videoConfig
            services.add(video)

            val input = Control.Service()
            input.id = Channel.ID_INP
            input.inputSourceService = Control.Service.InputSourceService()
            input.inputSourceService.touchscreen = Control.Service.InputSourceService.TouchConfig()
            input.inputSourceService.touchscreen.width = Screen.width
            input.inputSourceService.touchscreen.height = Screen.height
            input.inputSourceService.keycodesSupported = KeyCode.supported
            services.add(input)

            val audio1 = Control.Service()
            audio1.id = Channel.ID_AU1
            audio1.mediaSinkService = Control.Service.MediaSinkService()
            audio1.mediaSinkService.availableType = Media.MEDIA_CODEC_AUDIO
            audio1.mediaSinkService.audioType = Media.CAR_STREAM_SYSTEM
            audio1.mediaSinkService.audioConfigs = arrayOfNulls<Media.AudioConfiguration>(1)
            audio1.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AU1)
            services.add(audio1)

            val audio2 = Control.Service()
            audio2.id = Channel.ID_AU2
            audio2.mediaSinkService = Control.Service.MediaSinkService()
            audio2.mediaSinkService.availableType = Media.MEDIA_CODEC_AUDIO
            audio2.mediaSinkService.audioType = Media.CAR_STREAM_VOICE
            audio2.mediaSinkService.audioConfigs = arrayOfNulls<Media.AudioConfiguration>(1)
            audio2.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AU2)
            services.add(audio2)

            val audio0 = Control.Service()
            audio0.id = Channel.ID_AUD
            audio0.mediaSinkService = Control.Service.MediaSinkService()
            audio0.mediaSinkService.availableType = Media.MEDIA_CODEC_AUDIO
            audio0.mediaSinkService.audioType = Media.CAR_STREAM_MEDIA
            audio0.mediaSinkService.audioConfigs = arrayOfNulls<Media.AudioConfiguration>(1)
            audio0.mediaSinkService.audioConfigs[0] = AudioConfigs.get(Channel.ID_AUD)
            services.add(audio0)

            val mic = Control.Service()
            mic.id = Channel.ID_MIC
            mic.mediaSourceService = Control.Service.MediaSourceService()
            mic.mediaSourceService.type = Media.MEDIA_CODEC_AUDIO
            val micConfig = Media.AudioConfiguration()
            micConfig.sampleRate = 16000
            micConfig.numberOfBits = 16
            micConfig.numberOfChannels = 1
            mic.mediaSourceService.audioConfig = micConfig
            services.add(mic)

            if (settings.bluetoothAddress.isNotEmpty()) {
                val bluetooth = Control.Service()
                bluetooth.id = Channel.ID_BTH
                bluetooth.bluetoothService = Control.Service.BluetoothService()
                bluetooth.bluetoothService.carAddress = settings.bluetoothAddress
                bluetooth.bluetoothService.supportedPairingMethods = intArrayOf(Control.Service.BluetoothService.BLUETOOTH_PARING_METHOD_HFP)
                services.add(bluetooth)
            } else {
                AppLog.i("BT MAC Address is empty. Skip bluetooth service")
            }

            val mediaPlaybackStatus = Control.Service()
            mediaPlaybackStatus.id = Channel.ID_MPB
            mediaPlaybackStatus.mediaPlaybackService = Control.Service.MediaPlaybackStatusService()
            services.add(mediaPlaybackStatus)

            carInfo.services = services.toTypedArray()

            return carInfo
        }

        private fun makeSensorType(type: Int): Control.Service.SensorSourceService.Sensor {
            val sensor = Control.Service.SensorSourceService.Sensor()
            sensor.type = type
            return sensor
        }
    }
}
