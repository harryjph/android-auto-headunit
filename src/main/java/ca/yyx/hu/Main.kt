package ca.yyx.hu

import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import com.google.protobuf.nano.MessageNano

import java.util.Locale

import ca.yyx.hu.aap.AapMessage
import ca.yyx.hu.aap.protocol.Channel
import ca.yyx.hu.aap.protocol.MsgType
import ca.yyx.hu.aap.protocol.nano.Protocol

/**
 * @author algavris
 * *
 * @date 26/11/2016.
 */
object Main {
    @Throws(InvalidProtocolBufferNanoException::class)
    @JvmStatic fun main(args: Array<String>) {
        println("Main")


        val mediaAck = Protocol.Ack()
        mediaAck.clear()
        mediaAck.sessionId = Integer.MAX_VALUE
        mediaAck.ack = Integer.MAX_VALUE

        print(mediaAck.serializedSize)

        val sensors = Protocol.Service()
        sensors.id = 2
        sensors.sensorSourceService = Protocol.Service.SensorSourceService()
        sensors.sensorSourceService.sensors = arrayOfNulls<Protocol.Service.SensorSourceService.Sensor>(2)
        sensors.sensorSourceService.sensors[0] = Protocol.Service.SensorSourceService.Sensor()
        sensors.sensorSourceService.sensors[0].type = Protocol.SENSOR_TYPE_DRIVING_STATUS
        sensors.sensorSourceService.sensors[1] = Protocol.Service.SensorSourceService.Sensor()
        sensors.sensorSourceService.sensors[1].type = Protocol.SENSOR_TYPE_NIGHT
        //        input.inputSourceService.keycodesSupported = new int[] { 84 };

        printByteArray(MessageNano.toByteArray(sensors))
        val rsp2 = byteArrayOf(0x08, 0x02, 0x12, 0x0C, 0x0A, 0x02, 0x08, 0x01, 0x0A, 0x02, 0x08, 0x0A, 0x0A, 0x02, 0x08, 0x0D)
        printByteArray(rsp2)

        val actual = Protocol.Service()
        MessageNano.mergeFrom(actual, rsp2)
        printByteArray(MessageNano.toByteArray(actual))

        print(actual.toString())
    }


    private fun printByteArray(ba: ByteArray) {
        for (i in ba.indices) {
            val hex = String.format(Locale.US, "%02X", ba[i])
            print(hex)
            //            int pos = (ba[i] >> 3);
            //            if (pos > 0) {
            //                System.out.print("[" + pos + "]");
            //            }
            print(' ')
        }
        println()
    }

}
