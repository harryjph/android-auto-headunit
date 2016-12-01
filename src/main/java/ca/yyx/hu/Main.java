package ca.yyx.hu;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import java.util.Locale;

import ca.yyx.hu.aap.Encode;
import ca.yyx.hu.aap.Messages;
import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.utils.ByteArray;

/**
 * @author algavris
 * @date 26/11/2016.
 */

public class Main {
    public static void main(String[] args) throws InvalidProtocolBufferNanoException {
        System.out.println("Main");

        Protocol.Service audio0 = new Protocol.Service();
        audio0.id = Channel.AA_CH_AUD;
        audio0.mediaSinkService = new Protocol.Service.MediaSinkService();
        audio0.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio0.mediaSinkService.audioType = Protocol.CAR_STREAM_MEDIA;
        audio0.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        Protocol.AudioConfiguration audioConfig0 = new Protocol.AudioConfiguration();
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48;
        audioConfig0.numberOfBits = 16;
        audioConfig0.numberOfChannels = 2;
        audio0.mediaSinkService.audioConfigs[0] = audioConfig0;

        Protocol.Service audio1 = new Protocol.Service();
        audio1.id = Channel.AA_CH_AU1;
        audio1.mediaSinkService = new Protocol.Service.MediaSinkService();
        audio1.mediaSinkService.availableType = Protocol.MEDIA_CODEC_AUDIO;
        audio1.mediaSinkService.audioType = Protocol.CAR_STREAM_SYSTEM;
        audio1.mediaSinkService.audioConfigs = new Protocol.AudioConfiguration[1];
        Protocol.AudioConfiguration audioConfig1 = new Protocol.AudioConfiguration();
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig1.numberOfBits = 16;
        audioConfig1.numberOfChannels = 1;
        audio1.mediaSinkService.audioConfigs[0] = audioConfig1;

        System.out.println(audio0);
        System.out.println(audio1);

        printByteArray(MessageNano.toByteArray(audio0));
        System.out.println();
        byte[] actual1 = new byte[] {
                0x08, 6, 0x1A, 6+8, 0x08, 1,  0x10, 3, 0x1A, 8, 0x08, -128,   -9, 0x02,   0x10, 0x10,   0x18, 02,
        };
        printByteArray(actual1);

        System.out.println();
        System.out.println();

        printByteArray(MessageNano.toByteArray(audio1));
        byte[] actual2 = new byte[] {
                0x08, 4, 0x1A, 6+7, 0x08, 1,  0x10, 1, 0x1A, 7, 0x08, -128, 0x7d,         0x10, 0x10,   0x18, 1,
        };
        System.out.println();
        printByteArray(actual2);
    }

    static byte[] createNightModeMessage(boolean enabled) throws InvalidProtocolBufferNanoException {
        byte[] buffer = new byte[6];

        buffer[0] = -128;
        buffer[1] = 0x03;
        buffer[2] = 0x52;
        buffer[3] = 0x02;
        buffer[4] = 0x08;
        if (enabled)
            buffer[5] = 0x01;
        else
            buffer[5]= 0x00;



        Protocol.SensorBatch expected = MessageNano.mergeFrom(new Protocol.SensorBatch(), buffer, 2, buffer.length - 2);

        Protocol.SensorBatch sensorBatch = new Protocol.SensorBatch();
        sensorBatch.nightMode = new Protocol.SensorBatch.NightMode[1];
        sensorBatch.nightMode[0] = new Protocol.SensorBatch.NightMode();
        sensorBatch.nightMode[0].isNight = enabled;

        byte[] ba = new byte[sensorBatch.getSerializedSize() + 2];
        // Header
        ba[0] = (byte) 0x80;
        ba[1] = 0x03;
        MessageNano.toByteArray(sensorBatch, ba, 2, sensorBatch.getSerializedSize());


        System.out.println("Actual  : " + sensorBatch.toString());
        System.out.println("Expected: " + expected.toString());

        System.out.println();
        System.out.println("Bytes:");
        printByteArray(ba);
        System.out.println();
        printByteArray(buffer);
        return buffer;
    }

    static ByteArray createButtonMessage(long timeStamp, int button, boolean isPress) throws InvalidProtocolBufferNanoException {
        ByteArray buffer = new ByteArray(22);

        buffer.put(0x80, 0x01, 0x08);
        int size = Encode.longToByteArray(timeStamp, buffer.data, buffer.length);
        buffer.move(size);

        int press = isPress ? 0x01 : 0x00;
        buffer.put(0x22, 0x0A, 0x0A, 0x08, 0x08, button, 0x10, press, 0x18, 0x00, 0x20, 0x00);

        Protocol.InputReport expected = Protocol.InputReport.mergeFrom(new Protocol.InputReport(), buffer.data, 2, buffer.length - 2);

        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.KeyEvent keyEvent = new Protocol.KeyEvent();
        inputReport.timestamp = timeStamp;
        inputReport.keyEvent = keyEvent;

        keyEvent.keys = new Protocol.Key[1];
        keyEvent.keys[0] = new Protocol.Key();
        keyEvent.keys[0].keycode = button;
        keyEvent.keys[0].down = isPress;

        byte[] ba = new byte[inputReport.getSerializedSize() + 2];
        // Header
        ba[0] = (byte) 0x80;
        ba[1] = 0x01;
        MessageNano.toByteArray(inputReport, ba, 2, inputReport.getSerializedSize());


        System.out.println("Actual  : " + inputReport.toString());
        System.out.println("Expected: " + expected.toString());

        System.out.println();
        System.out.println("Bytes:");
        printByteArray(ba);
        System.out.println();
        printByteArray(buffer.data);

        return buffer;
    }

    static ByteArray createTouchMessage(long timeStamp, int action, int x, int y) throws InvalidProtocolBufferNanoException {


        ByteArray buffer = new ByteArray(32);

        buffer.put(0x80, 0x01, 0x08);

        int size = Encode.longToByteArray(timeStamp, buffer.data, buffer.length);          // Encode timestamp
        buffer.move(size);

        int size1_idx = buffer.length + 1;
        int size2_idx = buffer.length + 3;

        buffer.put(0x1a, 0x09, 0x0a, 0x03);

        /* Set magnitude of each axis */
        byte axis = 0;
        int[] coordinates = {x, y, 0};

        for (int i=0; i<3; i++) {
            axis += 0x08; //0x08, 0x10, 0x18
            buffer.put(axis);

            size = Encode.intToByteArray(coordinates[i], buffer.data, buffer.length);
            buffer.move(size);
            buffer.inc(size1_idx, size);
            buffer.inc(size2_idx, size);
        }
        buffer.put(0x10, 0x00, 0x18, action);

        Protocol.InputReport inputReport = new Protocol.InputReport();
        Protocol.TouchEvent touchEvent = new Protocol.TouchEvent();
        inputReport.timestamp = timeStamp;
        inputReport.touchEvent = touchEvent;

        touchEvent.pointerData = new Protocol.TouchEvent.Pointer[1];
        Protocol.TouchEvent.Pointer pointer = new Protocol.TouchEvent.Pointer();
        pointer.x = x;
        pointer.y = y;
        touchEvent.pointerData[0] = pointer;
        touchEvent.actionIndex = 0;
        touchEvent.action = action;

        byte[] ba = new byte[inputReport.getSerializedSize() + 2];
        // Header
        ba[0] = (byte) 0x80;
        ba[1] = 0x01;
        MessageNano.toByteArray(inputReport, ba, 2, inputReport.getSerializedSize());

        System.out.println("Actual:");
        printByteArray(ba);
        System.out.println();
        printByteArray(buffer.data);

        Protocol.InputReport report1 = Protocol.InputReport.mergeFrom(new Protocol.InputReport(), buffer.data, 2, buffer.length - 2);



        return buffer;
    }

    static void printByteArray(byte[] ba)
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
    }

}
