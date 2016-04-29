package ca.yyx.hu.decoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.SparseArray;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import ca.yyx.hu.HeadUnitTransport;
import ca.yyx.hu.Utils;

/**
 * @author algavris
 * @date 28/04/2016.
 */
public class AudioDecoder {
    private boolean audio_recording = false;
    private FileOutputStream audio_record_fos = null;
    private final Context mContext;

    public AudioDecoder(Context context) {
        mContext = context;
    }


    void audio_record_stop() {
        try {
            if (audio_record_fos != null)
                audio_record_fos.close();                                                     // Close output file
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
            //return;
        }
        audio_record_fos = null;
        audio_recording = false;
    }

    void audio_record_write(ByteBuffer content) {    // ffmpeg -i 2015-04-29-00_38_16.mp4 -vcodec copy -an -bsf:v h264_mp4toannexb  aa.h264
        if (!audio_recording) {
//*  Throwable: java.lang.IllegalArgumentException: File /sdcard/hurec.pcm contains a path separator
            try {
                audio_record_fos = mContext.openFileOutput("/sdcard/hurec.pcm", Context.MODE_WORLD_READABLE);//, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
                Utils.logw("audio_record_fos: " + audio_record_fos);
            } catch (Throwable t) {
                Utils.logw("Throwable: " + t);
                //return;
            }
//*/
            try {
                if (audio_record_fos == null)     // -> /data/data/ca.yyx.hu/files/hurec.pcm
                    audio_record_fos = mContext.openFileOutput("hurec.pcm", Context.MODE_WORLD_READABLE);//, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
                Utils.logw("audio_record_fos: " + audio_record_fos);
            } catch (Throwable t) {
                //Utils.loge ("Throwable: " + t);
                Utils.loge("Throwable: " + t);
                return;
            }

            audio_recording = true;
        }

        int pos = content.position();
        int siz = content.remaining();
        int last = pos + siz - 1;
        byte[] ba = content.array();
        if (ba == null) {
            Utils.loge("ba == null...   pos: " + pos + "  siz: " + siz + " (" + Utils.hex_get(siz) + ")  last: " + last);
            return;
        }
        byte b1 = ba[pos + 3];
        byte bl = ba[last];
        if (Utils.ena_log_verbo)
            Utils.logv("pos: " + pos + "  siz: " + siz + "  last: " + last + " (" + Utils.hex_get(b1) + ")  b1: " + b1 + "  bl: " + bl + " (" + Utils.hex_get(bl) + ")");

        Utils.loge("pos: " + pos + "  siz: " + siz + "  last: " + last + " (" + Utils.hex_get(b1) + ")  b1: " + b1 + "  bl: " + bl + " (" + Utils.hex_get(bl) + ")");

        try {
            audio_record_fos.write(ba, pos, siz);                            // Copy input to output file
        } catch (Throwable t) {
            Utils.loge("Throwable: " + t);
        }
    }

    public static final int MIC_BUFFER_SIZE = 8192;
    private AudioRecord mMicAudioRecord = null;
    private static final int BUFFER_SIZE_32 = 32768;
    private static final int BUFFER_SIZE_4 = 4096;
    private static final int OUT_AUDIO_STREAM = AudioManager.STREAM_MUSIC;


    private SparseArray<AudioTrack> mAudioTracks = new SparseArray<>(3);


    private boolean thread_mic_audio_active = false;
    private Thread thread_mic_audio = null;

    public void mic_audio_stop() {
        Utils.logd("thread_mic_audio: " + thread_mic_audio + "  thread_mic_audio_active: " + thread_mic_audio_active);
        mic_audio_len = 0;
        if (thread_mic_audio_active) {
            thread_mic_audio_active = false;
            if (thread_mic_audio != null)
                thread_mic_audio.interrupt();
        }

        if (mMicAudioRecord != null) {
            mMicAudioRecord.stop();
            mMicAudioRecord.release();                                     // Release AudioTrack resources
            mMicAudioRecord = null;
        }
    }

    private int mic_audio_start() {
        try {
            mMicAudioRecord = new AudioRecord( MediaRecorder.AudioSource.DEFAULT, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 32768);
            int rec_state = mMicAudioRecord.getState();
            Utils.logd("rec_state: " + rec_state);
            if (rec_state == AudioRecord.STATE_INITIALIZED) {                 // If Init OK...
                Utils.logd("Success with m_mic_src: " + MediaRecorder.AudioSource.DEFAULT);
                mMicAudioRecord.startRecording();                            // Start input

                thread_mic_audio = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mic_audio_len = 0;                                                  // Reset for next read into single buffer
                        while (thread_mic_audio_active) {                                 // While Thread should be active...
                            while (mic_audio_len > 0)                                       // While single buffer is in use...
                                Utils.ms_sleep(3);
                            mic_audio_len = phys_mic_audio_read(mic_audio_buf, MIC_BUFFER_SIZE);
                        }
                    }
                }, "mic_audio");

                Utils.logd("thread_mic_audio: " + thread_mic_audio);
                thread_mic_audio_active = true;
                thread_mic_audio.start();
                return (0);
            }
        } catch (Exception e) {
            Utils.loge("Exception: " + e);  // "java.lang.IllegalArgumentException: Invalid audio source."
            mMicAudioRecord = null;
            return (-2);
        }
        mMicAudioRecord = null;
        return (-1);
    }

    private byte[] mic_audio_buf = new byte[MIC_BUFFER_SIZE];
    private int mic_audio_len = 0;

    public int mic_audio_read(byte[] aud_buf, int max_len) {

        if (!thread_mic_audio_active)                                      // If mic audio thread not active...
            mic_audio_start();                                               // Start it

        if (!thread_mic_audio_active)                                      // If mic audio thread STILL not active...
            return (-1);                                                      // Done with error

        if (mic_audio_len <= 0)
            return (mic_audio_len);

        int len = mic_audio_len;
        if (len > max_len)
            len = max_len;
        int ctr = 0;
        for (ctr = 0; ctr < len; ctr++)
            aud_buf[ctr] = mic_audio_buf[ctr];
        mic_audio_len = 0;                                                  // Reset for next read into single buffer
        return (len);
    }

    private int phys_mic_audio_read(byte[] aud_buf, int max_len) {
        int len = 0;
        if (mMicAudioRecord == null) {
            return (len);
        }
        len = mMicAudioRecord.read(aud_buf, 0, max_len);//MIC_BUFFER_SIZE);
        if (len <= 0) {                                               // If no audio data...
            if (len == android.media.AudioRecord.ERROR_INVALID_OPERATION)   // -3
                Utils.logd("get expected interruption error due to shutdown: " + len);
                // -2: ERROR_BAD_VALUE
            else
                Utils.loge("get error: " + len);
            return (len);
        }
        return (len);
    }


    public void out_audio_stop(int chan) {

        if (enable_audio_recycle) {
            return;
        }

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

    private boolean enable_audio_recycle = false;//true;

    private AudioTrack out_audio_start(int chan) {
        if (enable_audio_recycle) {
            if (mAudioTracks.get(chan) != null) {
               return mAudioTracks.get(chan);
            }
        }

        AudioTrack out_audiotrack;
        if (chan == HeadUnitTransport.AA_CH_AUD) {
            out_audiotrack = new AudioTrack(OUT_AUDIO_STREAM, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_32, AudioTrack.MODE_STREAM);
        } else if (chan == HeadUnitTransport.AA_CH_AU1) {
            out_audiotrack = new AudioTrack(OUT_AUDIO_STREAM, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_4, AudioTrack.MODE_STREAM);
        } else if (chan == HeadUnitTransport.AA_CH_AU2) {
            out_audiotrack = new AudioTrack(OUT_AUDIO_STREAM, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_4, AudioTrack.MODE_STREAM);
        } else {
            return null;
        }

        mAudioTracks.put(chan, out_audiotrack);
        out_audiotrack.play();                                         // Start output
        return out_audiotrack;
    }

    void out_audio_write(int chan, byte[] aud_buf, int len) {
        AudioTrack out_audiotrack = mAudioTracks.get(chan);

        if (out_audiotrack == null) {
            out_audiotrack = out_audio_start(chan);
            if (out_audiotrack == null)
                return;
        }

        int written = out_audiotrack.write(aud_buf, 0, len);
        if (written == len)
            Utils.logv("OK AudioTrack written: " + written);
        else
            Utils.loge("Error AudioTrack written: " + written + "  len: " + len);
    }

    public boolean isRecording() {
        return audio_recording;
    }
}
