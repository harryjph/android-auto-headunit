package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
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
class ServiceDiscoveryResponse(settings: Settings, densityDpi: Int) : AapMessage(Channel.ID_CTR, Control.ControlMsgType.SERVICEDISCOVERYRESPONSE_VALUE, makeProto(settings, densityDpi)) {

    companion object {
        private fun makeProto(settings: Settings, densityDpi: Int): MessageLite {
            val services = mutableListOf<Control.Service>()

            val sensors = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_SEN
                service.sensorSourceService = Control.Service.SensorSourceService.newBuilder().also { sources ->
                    sources.addSensors(makeSensorType(Sensors.SensorType.DRIVING_STATUS))
                    if (settings.useGpsForNavigation) sources.addSensors(makeSensorType(Sensors.SensorType.LOCATION))
                    if (settings.nightMode != Settings.NightMode.NONE) sources.addSensors(makeSensorType(Sensors.SensorType.NIGHT))
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
                        codecResolution = settings.resolution
                        frameRate = Control.Service.MediaSinkService.VideoConfiguration.VideoFrameRateType._30 // TODO settings option
                        density = densityDpi // TODO maybe this needs adjusting?
                    }.build())
                }.build()
            }.build()

            services.add(video)

            val input = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_INP
                service.inputSourceService = Control.Service.InputSourceService.newBuilder().also {
                    it.touchscreen = Control.Service.InputSourceService.TouchConfig.newBuilder().apply {
                        width = Screen.forResolution(settings.resolution).width
                        height = Screen.forResolution(settings.resolution).height
                    }.build()
                    it.addAllKeycodesSupported(KeyCode.supported)
                }.build()
            }.build()

            services.add(input)

            val mediaAudio = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_AUD
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.AUDIO
                    it.audioType = Media.AudioStreamType.MEDIA
                    it.addAudioConfigs(AudioConfigs[Channel.ID_AUD])
                }.build()
            }.build()
            services.add(mediaAudio)

            val speechAudio = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_AU1
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.AUDIO
                    it.audioType = Media.AudioStreamType.SPEECH
                    it.addAudioConfigs(AudioConfigs[Channel.ID_AU1])
                }.build()
            }.build()
            services.add(speechAudio)

            val systemAudio = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_AU2
                service.mediaSinkService = Control.Service.MediaSinkService.newBuilder().also {
                    it.availableType = Media.MediaCodecType.AUDIO
                    it.audioType = Media.AudioStreamType.SYSTEM
                    it.addAudioConfigs(AudioConfigs[Channel.ID_AU2])
                }.build()
            }.build()
            services.add(systemAudio)

            val microphone = Control.Service.newBuilder().also { service ->
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
            services.add(microphone)

            if (settings.bluetoothAddress.isNotEmpty()) { // TODO find out what exactly this does
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
                AppLog.i { "BT MAC Address is empty. Skip bluetooth service" }
            }

            val mediaPlaybackStatus = Control.Service.newBuilder().also { service ->
                service.id = Channel.ID_MPB
                service.mediaPlaybackService = Control.Service.MediaPlaybackStatusService.newBuilder().build()
            }.build()
            services.add(mediaPlaybackStatus)

            return Control.ServiceDiscoveryResponse.newBuilder().apply {
                make = "Android Auto HeadUnit"
                model = "Harry Phillips"
                year = "2019"
                vehicleId = "Harry Phillips"
                headUnitModel = "Harry Phillips"
                headUnitMake = "Android Auto HeadUnit"
                headUnitSoftwareBuild = "1.0"
                headUnitSoftwareVersion = "1.0"
                driverPosition = true // true for RHS, false for LHS
                canPlayNativeMediaDuringVr = false // TODO what does this do?
                hideProjectedClock = false
                addAllServices(services)
            }.build()
        }

        private fun makeSensorType(type: Sensors.SensorType): Control.Service.SensorSourceService.Sensor {
            return Control.Service.SensorSourceService.Sensor.newBuilder().setType(type).build()
        }
    }
}
