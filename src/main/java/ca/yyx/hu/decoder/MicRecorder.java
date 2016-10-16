package ca.yyx.hu.decoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 12/05/2016.
 */
public class MicRecorder {

    public static final int MIC_BUFFER_SIZE = 8192;
    private AudioRecord mMicAudioRecord = null;

    private byte[] mic_audio_buf = new byte[MIC_BUFFER_SIZE];
    private int mic_audio_len = 0;

    private boolean thread_mic_audio_active = false;
    private Thread thread_mic_audio = null;

    public void mic_audio_stop() {
        AppLog.logd("thread_mic_audio: " + thread_mic_audio + "  thread_mic_audio_active: " + thread_mic_audio_active);
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

    public int mic_audio_read(byte[] aud_buf, int start, int max_len) {

        if (!thread_mic_audio_active)                                      // If mic audio thread not active...
            mic_audio_start();                                               // Start it

        if (!thread_mic_audio_active)                                      // If mic audio thread STILL not active...
            return (-1);                                                      // Done with error

        if (mic_audio_len <= 0)
            return (mic_audio_len);

        int len = mic_audio_len;
        if (len > max_len) {
            len = max_len;
        }
        for (int ctr = start; ctr < len; ctr++) {
            aud_buf[ctr] = mic_audio_buf[ctr];
        }
        mic_audio_len = 0;                                                  // Reset for next read into single buffer
        return len + start;
    }

    private int phys_mic_audio_read(byte[] aud_buf, int max_len) {
        int len = 0;
        if (mMicAudioRecord == null) {
            return (len);
        }
        len = mMicAudioRecord.read(aud_buf, 0, max_len);//MIC_BUFFER_SIZE);
        if (len <= 0) {                                               // If no audio data...
            if (len == android.media.AudioRecord.ERROR_INVALID_OPERATION)   // -3
                AppLog.loge("get expected interruption error due to shutdown: " + len);
            else
                AppLog.loge("get error: " + len);
            return (len);
        }
        return (len);
    }

    private int mic_audio_start() {
        try {
            mMicAudioRecord = new AudioRecord( MediaRecorder.AudioSource.DEFAULT, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 32768);
            int rec_state = mMicAudioRecord.getState();
            AppLog.logd("rec_state: " + rec_state);

            if (rec_state == AudioRecord.STATE_INITIALIZED) {                 // If Init OK...
                AppLog.logd("Success with m_mic_src: " + MediaRecorder.AudioSource.DEFAULT);
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

                thread_mic_audio_active = true;
                thread_mic_audio.start();
                return (0);
            }
        } catch (Exception e) {
            AppLog.loge(e);  // "java.lang.IllegalArgumentException: Invalid audio source."
            mMicAudioRecord = null;
            return (-2);
        }
        mMicAudioRecord = null;
        return (-1);
    }
}
