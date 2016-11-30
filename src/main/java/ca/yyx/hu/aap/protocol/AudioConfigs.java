package ca.yyx.hu.aap.protocol;

import android.media.AudioManager;
import android.util.SparseArray;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;

/**
 * @author algavris
 * @date 25/11/2016.
 */

public class AudioConfigs {
    private static SparseArray<Protocol.AudioConfiguration> mAudioTracks = new SparseArray<>(2);

    public static Protocol.AudioConfiguration get(int channel) {
        return mAudioTracks.get(channel);
    }

    static {
        Protocol.AudioConfiguration audioConfig0 = new Protocol.AudioConfiguration();
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48;
        audioConfig0.numberOfBits = 16;
        audioConfig0.numberOfChannels = 2;
        mAudioTracks.put(Channel.AA_CH_AUD, audioConfig0);

        Protocol.AudioConfiguration audioConfig1 = new Protocol.AudioConfiguration();
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig1.numberOfBits = 16;
        audioConfig1.numberOfChannels = 1;
        mAudioTracks.put(Channel.AA_CH_AU1, audioConfig1);
    }
}
