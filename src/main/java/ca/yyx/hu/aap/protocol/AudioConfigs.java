package ca.yyx.hu.aap.protocol;

import android.media.AudioManager;
import android.util.SparseArray;
import android.util.SparseIntArray;

import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;

/**
 * @author algavris
 * @date 25/11/2016.
 */

public class AudioConfigs {
    private static SparseArray<Protocol.AudioConfiguration> mAudioTracks = new SparseArray<>(3);
    private static SparseIntArray mStreamTypes = new SparseIntArray(3);

    public static Protocol.AudioConfiguration get(int channel) {
        return mAudioTracks.get(channel);
    }
    public static int getStreamType(int channel) { return mStreamTypes.get(channel); }

    static {
        Protocol.AudioConfiguration audioConfig0 = new Protocol.AudioConfiguration();
        audioConfig0.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_48;
        audioConfig0.numberOfBits = 16;
        audioConfig0.numberOfChannels = 2;
        mAudioTracks.put(Channel.AA_CH_AUD, audioConfig0);
        mStreamTypes.put(Channel.AA_CH_AUD, AudioManager.STREAM_MUSIC);

        Protocol.AudioConfiguration audioConfig1 = new Protocol.AudioConfiguration();
        audioConfig1.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig1.numberOfBits = 16;
        audioConfig1.numberOfChannels = 1;
        mAudioTracks.put(Channel.AA_CH_AU1, audioConfig1);
        mStreamTypes.put(Channel.AA_CH_AU1, AudioManager.STREAM_SYSTEM);

        Protocol.AudioConfiguration audioConfig2 = new Protocol.AudioConfiguration();
        audioConfig2.sampleRate = AudioDecoder.SAMPLE_RATE_HZ_16;
        audioConfig2.numberOfBits = 16;
        audioConfig2.numberOfChannels = 1;
        mAudioTracks.put(Channel.AA_CH_AU2, audioConfig1);
        mStreamTypes.put(Channel.AA_CH_AU2, AudioManager.STREAM_RING);

    }
}
