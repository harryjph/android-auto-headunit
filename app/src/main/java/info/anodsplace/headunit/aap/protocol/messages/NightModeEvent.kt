package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.MessageLite
import info.anodsplace.headunit.aap.protocol.proto.Sensors


class NightModeEvent(enabled: Boolean)
    : SensorEvent(Sensors.SensorType.NIGHT_VALUE, makeProto(enabled)) {

    companion object {
        private fun makeProto(enabled: Boolean): MessageLite {
            return Sensors.SensorBatch.newBuilder().also {
                it.addNightMode(
                        Sensors.SensorBatch.NightData.newBuilder().apply {
                            isNightMode = enabled
                        }
                )
            }.build()
        }
    }
}
