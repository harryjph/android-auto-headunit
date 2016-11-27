package ca.yyx.hu.decoder;

import android.util.SparseArray;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class AudioDecoder {
    public static final int SAMPLE_RATE_HZ_48 = 48000;
    public static final int SAMPLE_RATE_HZ_16 = 16000;

    private SparseArray<AudioTrackWrapper> mAudioTracks = new SparseArray<>(3);

    public AudioTrackWrapper getTrack(int channel)
    {
        return mAudioTracks.get(channel);
    }

    public void decode(int channel, byte[] buffer, int offset, int size) {
        AudioTrackWrapper audioTrack = mAudioTracks.get(channel);
        audioTrack.write(buffer, offset, size);
    }

    public void stop() {
        for (int i = 0; i < mAudioTracks.size(); i++)
        {
            stop(mAudioTracks.keyAt(i));
        }
    }

    public void stop(int chan) {
        AudioTrackWrapper audioTrack = mAudioTracks.get(chan);
        if (audioTrack != null) {
            audioTrack.stop();
            mAudioTracks.put(chan, null);
        }
    }

    public void start(int channel, int stream, int sampleRate, int numberOfBits, int numberOfChannels) {
        AudioTrackWrapper thread = new AudioTrackWrapper(stream, sampleRate, numberOfBits, numberOfChannels);
        mAudioTracks.put(channel, thread);
    }
}
