package ca.yyx.hu;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import java.util.Locale;

import ca.yyx.hu.aap.AapMessage;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 26/11/2016.
 */
public class Main {
    public static void main(String[] args) throws InvalidProtocolBufferNanoException {
        System.out.println("Main");


        Protocol.Ack mediaAck = new Protocol.Ack();
        mediaAck.clear();
        mediaAck.sessionId = Integer.MAX_VALUE;
        mediaAck.ack = Integer.MAX_VALUE;

        System.out.print(mediaAck.getSerializedSize());

        Protocol.Service sensors = new Protocol.Service();
        sensors.id = 2;
        sensors.sensorSourceService = new Protocol.Service.SensorSourceService();
        sensors.sensorSourceService.sensors = new Protocol.Service.SensorSourceService.Sensor[2];
        sensors.sensorSourceService.sensors[0] = new Protocol.Service.SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[0].type = Protocol.SENSOR_TYPE_DRIVING_STATUS;
        sensors.sensorSourceService.sensors[1] = new Protocol.Service.SensorSourceService.Sensor();
        sensors.sensorSourceService.sensors[1].type = Protocol.SENSOR_TYPE_NIGHT;
//        input.inputSourceService.keycodesSupported = new int[] { 84 };

        printByteArray(MessageNano.toByteArray(sensors));
        byte rsp2[] = {0x08, 0x02, 0x12, 0x0C, 0x0A, 0x02, 0x08, 0x01, 0x0A, 0x02, 0x08, 0x0A, 0x0A, 0x02, 0x08, 0x0D};
        printByteArray(rsp2);

        Protocol.Service actual = new Protocol.Service();
        MessageNano.mergeFrom(actual, rsp2);
        printByteArray(MessageNano.toByteArray(actual));

        System.out.print(actual.toString());
    }


    private static void printByteArray(byte[] ba)
    {
        for (int i = 0; i < ba.length; i++) {
            String hex = String.format(Locale.US, "%02X", ba[i]);
            System.out.print(hex);
//            int pos = (ba[i] >> 3);
//            if (pos > 0) {
//                System.out.print("[" + pos + "]");
//            }
            System.out.print(' ');
        }
        System.out.println();
    }

}
