package ca.yyx.hu;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

import java.util.Locale;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;

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
