package ca.yyx.hu;

import com.google.protobuf.nano.MessageNano;

import ca.yyx.hu.aap.protocol.nano.Protocol;

/**
 * @author algavris
 * @date 26/11/2016.
 */

public class Main {
    public static void main(String[] args)
    {
        System.out.println("Main");

        Protocol.Service bluetooth = new Protocol.Service();
        bluetooth.id = 8;
        bluetooth.bluetoothService = new Protocol.Service.BluetoothService();
        bluetooth.bluetoothService.carAddress = "FC:58:FA:12:1A:0D";
        bluetooth.bluetoothService.supportedPairingMethods = new int[] { 2, 3 };
        byte[] ba = MessageNano.toByteArray(bluetooth);

        for (int i = 0; i < ba.length; i++) {
            System.out.print(ba[i]);
            System.out.print(' ');
        }

        System.out.println();
        // 0x12,2,0x02,0x03

//        00000000 0A 15 08 02 1A 11 08 03 22 0B 08 01 10 02 18 00
//            0016 20 00 28 A0 01 28 01 12 00 1A 00 22 00 2A 00 30
//            0032 00 3A 00 42 00 4A 00 52 00 58 00 60 00

        byte[] ex = { 8, 8, 32, 23, 0x0A, 17, 'F','C',':','5','8',':','F','A',':','1','2',':','1','A',':','0','D',12, 2, 2, 3 };

        for (int i = 0; i < ex.length; i++) {
            System.out.print(ex[i]);
            System.out.print(' ');
        }

        // 12 = 00 1100
        //  4 = 100
        //  1 = 1

// Video CHANNEL
//        0x0A, 0x15, 0x08, 3, 0x1A, 0x11, 0x08, 0x03, 0x22, 0x0D, 0x08, 0x01, 0x10, 0x02, 0x18, 0x00, 0x20, 0x00, 0x28, -96, 0x01, 0x30, 0x00,
//        0x0A, 18, 0x08, 4, 0x1A, 6+8, 0x08, 1,  0x10, 3, 0x1A, 8, 0x08, -128,   -9, 0x02,   0x10, 0x10,   0x18, 02,
    }

}
