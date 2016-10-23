package ca.yyx.hu.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.SparseArray;

import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class AudioDecoder {
    public static final int BUFFER_SIZE_32 = 32768;
    public static final int BUFFER_SIZE_4 = 4096;

    private SparseArray<AudioTrack> mAudioTracks = new SparseArray<>(3);

    public AudioTrack getTrack(int channel)
    {
        return mAudioTracks.get(channel);
    }

    public void decode(int channel, byte[] buffer, int offset, int size) {
        AudioTrack audiotrack = mAudioTracks.get(channel);

        int written = audiotrack.write(buffer, offset, size);
        if (written != size) {
            AppLog.loge("Error AudioTrack written: " + written + "  len: " + size);
        }
    }

    public void stop() {

        for (int i = 0; i < mAudioTracks.size(); i++)
        {
            stop(mAudioTracks.keyAt(i));
        }
    }

    public void stop(int chan) {
        AudioTrack out_audiotrack = mAudioTracks.get(chan);

        if (out_audiotrack == null) {
            AppLog.logd("out_audiotrack == null");
            return;
        }

        long ms_tmo = Utils.tmr_ms_get() + 2000;                        // Wait for maximum of 2 seconds
        int last_frames = 0;
        int curr_frames = 1;

        out_audiotrack.flush();
        // While audio still running and 2 second wait timeout has not yet elapsed...
        while (last_frames != curr_frames && Utils.tmr_ms_get() < ms_tmo) {
            Utils.ms_sleep(150);//300);//100);                                          // 100 ms = time for about 3 KBytes (1.5 buffers of 2Bytes (1024 samples) as used for 16,000 samples per second
            last_frames = curr_frames;
            curr_frames = out_audiotrack.getPlaybackHeadPosition();
            AppLog.logd("curr_frames: " + curr_frames + "  last_frames: " + last_frames);
        }

        out_audiotrack.stop();
        out_audiotrack.release();                                      // Release AudioTrack resources
        mAudioTracks.put(chan, null);
    }

    public AudioTrack start(int chan, int sampleRateInHz, int channelConfig, int bufferSizeInBytes) {
        AudioTrack audiotrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        mAudioTracks.put(chan, audiotrack);
        audiotrack.play();
        return audiotrack;
    }
}
