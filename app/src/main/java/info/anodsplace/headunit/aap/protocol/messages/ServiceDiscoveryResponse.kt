package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.Message
import info.anodsplace.headunit.aap.AapMessage
import info.anodsplace.headunit.aap.KeyCode
import info.anodsplace.headunit.aap.protocol.AudioConfigs
import info.anodsplace.headunit.aap.protocol.Channel
import info.anodsplace.headunit.aap.protocol.Screen
import info.anodsplace.headunit.aap.protocol.proto.Control
import info.anodsplace.headunit.aap.protocol.proto.Media
import info.anodsplace.headunit.aap.protocol.proto.Sensors
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.Settings

/**
 * @author alex gavrishev
 *
 * @date 13/02/2017.
 */
class ServiceDiscoveryResponse(settings: Settings)
    : AapMessage(Channel.ID_CTR, Control.ControlMsgType.SERVICEDISCOVERYRESPONSE_VALUE, makeProto(settings)) {

    companion object {
        private fun makeProto(settings: Settings): Message {

            val services = mutableListOf<Control.Service>()

            val sensors = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_SEN
                service.sensorSourceService = Control.Service.SensorSourceService.newBuilder().also { sources ->
                    sources.addSensors(makeSensorType(Sensors.SensorType.DRIVING_STATUS))
                    if (settings.useGpsForNavigation) {
                        sources.addSensors(makeSensorType(Sensors.SensorType.LOCATION))
                    }
                    if (settings.nightMode != Settings.NightMode.NONE){
                        sources.addSensors(makeSensorType(Sensors.SensorType.NIGHT))
                    }
                }.build()
            }.build()

            services.add(sensors)

            val video = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_VID
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.VIDEO
                    it.audioType = Media.AudioStreamType.NONE
                    it.availableWhileInCall = true
                    it.addVideoConfigs(Control.Service.MediaSinkService.VideoConfiguration.newBuilder().apply {
                        marginHeight = 0
                        marginWidth = 0
                        codecResolution = Control.Service.MediaSinkService.VideoConfiguration.VideoCodecResolutionType._800x480
                        frameRate = Control.Service.MediaSinkService.VideoConfiguration.VideoFrameRateType._60
                        density = Screen.density
                    }.build())
                }.build()
            }.build()

            services.add(video)

            val input = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_INP
                service.inputSourceService = Control.Service.InputSourceService.newBuilder().also {
                    it.touchscreen = Control.Service.InputSourceService.TouchConfig.newBuilder().apply {
                        width = Screen.width
                        height = Screen.height
                    }.build()
                    it.addAllKeycodesSupported(KeyCode.supported)
                }.build()
            }.build()

            services.add(input)

            val audio1 = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_AU1
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.AUDIO
                    it.audioType = Media.AudioStreamType.SPEECH
                    it.addAudioConfigs(AudioConfigs.get(Channel.ID_AU1))
                }.build()
            }.build()
            services.add(audio1)

            val audio2 = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_AU2
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.AUDIO
                    it.audioType = Media.AudioStreamType.SYSTEM
                    it.addAudioConfigs(AudioConfigs.get(Channel.ID_AU2))
                }.build()
            }.build()
            services.add(audio2)

            val audio0 = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_AUD
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.AUDIO
                    it.audioType = Media.AudioStreamType.MEDIA
                    it.addAudioConfigs(AudioConfigs.get(Channel.ID_AUD))
                }.build()
            }.build()
            services.add(audio0)

            val mic = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_MIC
                service.mediaSourceService = Control.Service.MediaSourceService.newBuilder().also {
                    it.type = Media.MediaCodecType.AUDIO
                    it.audioConfig = Media.AudioConfiguration.newBuilder().apply {
                        sampleRate = 16000
                        numberOfBits = 16
                        numberOfChannels = 1
                    }.build()
                }.build()
            }.build()
            services.add(mic)

            if (settings.bluetoothAddress.isNotEmpty()) {
                val bluetooth = Control.Service.newBuilder().also { service ->
                    service.id = Channel.ID_BTH
                    service.bluetoothService = Control.Service.BluetoothService.newBuilder().also {
                        it.carAddress = settings.bluetoothAddress
                        it.addAllSupportedPairingMethods(
                                listOf(Control.BluetoothPairingMethod.A2DP,
                                        Control.BluetoothPairingMethod.HFP)
                        )
                    }.build()
                }.build()
                services.add(bluetooth)
            } else {
                AppLog.i("BT MAC Address is empty. Skip bluetooth service")
            }

            val mediaPlaybackStatus = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_MPB
                service.mediaPlaybackService = Control.Service.MediaPlaybackStatusService.newBuilder().build()
            }.build()
            services.add(mediaPlaybackStatus)

            return Control.ServiceDiscoveryResponse.newBuilder().apply {
                make = "AACar"
                model = "0001"
                year = "2016"
                vehicleId = "20190810"
                headUnitModel = "ChangAn S"
                headUnitMake = "Roadrover"
                headUnitSoftwareBuild = "SWB1"
                headUnitSoftwareVersion = "SWV1"
                driverPosition = true
                canPlayNativeMediaDuringVr = false
                hideProjectedClock = false
                addAllServices(services)
            }.build()
        }

        private fun makeSensorType(type: Sensors.SensorType): Control.Service.SensorSourceService.Sensor {
            return Control.Service.SensorSourceService.Sensor.newBuilder()
                    .setType(type).build()
        }
    }
}
