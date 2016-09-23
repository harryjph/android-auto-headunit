package ca.yyx.hu.decoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.SparseArray;

import java.nio.ByteBuffer;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class AudioDecoder {
    public static final int AA_CH_AUD = 4;
    public static final int AA_CH_AU1 = 5;
    public static final int AA_CH_AU2 = 6;
    private static final int AA_CH_MAX = 7;

    public void decode(byte[] buffer, int size) {
        // Create content byte array
        if (size <= 2048 + 96) {
            out_audio_write(AA_CH_AU1, buffer, size);                     // Position always 0 so just use siz as len ?
        } else {
            out_audio_write(AA_CH_AUD, buffer, size);                     // Position always 0 so just use siz as len ?
        }
    }

    public void stop() {
        out_audio_stop(AA_CH_AUD);                                         // In case Byebye terminates without proper audio stop
        out_audio_stop(AA_CH_AU1);
        out_audio_stop(AA_CH_AU2);
    }

    private static final int BUFFER_SIZE_32 = 32768;
    private static final int BUFFER_SIZE_4 = 4096;
    private static final int OUT_AUDIO_STREAM = AudioManager.STREAM_MUSIC;

    private SparseArray<AudioTrack> mAudioTracks = new SparseArray<>(3);

    public void out_audio_stop(int chan) {
        AudioTrack out_audiotrack = mAudioTracks.get(chan);

        if (out_audiotrack == null) {
            Utils.logd("out_audiotrack == null");
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
            Utils.logd("curr_frames: " + curr_frames + "  last_frames: " + last_frames);
        }

        out_audiotrack.stop();
        out_audiotrack.release();                                      // Release AudioTrack resources
        mAudioTracks.put(chan, null);
    }

    private AudioTrack out_audio_start(int chan) {
        AudioTrack out_audiotrack;
        if (chan == AA_CH_AUD) {
            out_audiotrack = new AudioTrack(OUT_AUDIO_STREAM, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_32, AudioTrack.MODE_STREAM);
        } else if (chan == AA_CH_AU1) {
            out_audiotrack = new AudioTrack(OUT_AUDIO_STREAM, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_4, AudioTrack.MODE_STREAM);
        } else if (chan == AA_CH_AU2) {
            out_audiotrack = new AudioTrack(OUT_AUDIO_STREAM, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_4, AudioTrack.MODE_STREAM);
        } else {
            return null;
        }

        mAudioTracks.put(chan, out_audiotrack);
        out_audiotrack.play();                                         // Start output
        return out_audiotrack;
    }

    private void out_audio_write(int chan, byte[] aud_buf, int len) {
        AudioTrack out_audiotrack = mAudioTracks.get(chan);

        if (out_audiotrack == null) {
            out_audiotrack = out_audio_start(chan);
            if (out_audiotrack == null)
                return;
        }

        int written = out_audiotrack.write(aud_buf, 0, len);
        if (written != len) {
            Utils.loge("Error AudioTrack written: " + written + "  len: " + len);
        }
    }

}
