package ca.yyx.hu.aap.protocol;

import android.util.SparseArray;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;

/**
 * @author algavris
 * @date 25/11/2016.
 */

public class AudioConfigs {
    private static SparseArray<Protocol.AudioConfig> mAudioTracks = new SparseArray<>(3);

    public static Protocol.AudioConfig get(int channel) {
        return mAudioTracks.get(channel);
    }

    static {
        Protocol.AudioConfig audioConfig0 = new Protocol.AudioConfig();
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48;
        audioConfig0.bitDepth = 16;
        audioConfig0.channelCount = 2;
        mAudioTracks.put(Channel.AA_CH_AUD, audioConfig0);

        Protocol.AudioConfig audioConfig1 = new Protocol.AudioConfig();
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig1.bitDepth = 16;
        audioConfig1.channelCount = 1;
        mAudioTracks.put(Channel.AA_CH_AU1, audioConfig1);

        Protocol.AudioConfig audioConfig2 = new Protocol.AudioConfig();
        audioConfig2.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig2.bitDepth = 16;
        audioConfig2.channelCount = 1;
        mAudioTracks.put(Channel.AA_CH_AU2, audioConfig1);
    }
}
