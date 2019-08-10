package info.anodsplace.headunit.aap.protocol.messages

import com.google.protobuf.Message
import info.anodsplace.headunit.aap.protocol.proto.Sensors

/**
 * @author algavris
 * *
 * @date 13/02/2017.
 */

class NightModeEvent(enabled: Boolean)
    : SensorEvent(Sensors.SensorType.NIGHT_VALUE, makeProto(enabled)) {

    companion object {
        private fun makeProto(enabled: Boolean): Message {
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
