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

        Protocol.Service input = new Protocol.Service();
        input.id = 1;
        input.inputSourceService = new Protocol.Service.InputSourceService();
        input.inputSourceService.touchscreen = new Protocol.Service.InputSourceService.TouchConfig();
        input.inputSourceService.touchscreen.width = 800;
        input.inputSourceService.touchscreen.height = 480;
//        input.inputSourceService.keycodesSupported = new int[] { 84 };

        printByteArray(MessageNano.toByteArray(input));
        byte rsp2[] = {0x08, 0x01, 0x22, 0x0B, 0x0A, 0x01, 0x54, 0x12, 0x06, 0x08, (byte) 0xA0, 0x06, 0x10, (byte) 0xE0, 0x03};
        printByteArray(rsp2);

        Protocol.Service actual = new Protocol.Service();
        MessageNano.mergeFrom(actual, rsp2);
        printByteArray(MessageNano.toByteArray(actual));

        byte rsp3[] = { 0x08, 0x01, 0x22, 2+6, 0x12,  6, 0x08, -96,   6,    0x10, -32, 3};
        printByteArray(rsp3);

        Protocol.Service actual1 = new Protocol.Service();
        MessageNano.mergeFrom(actual1, rsp3);
        printByteArray(MessageNano.toByteArray(actual1));
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
