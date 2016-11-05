package ca.yyx.hu.decoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 12/05/2016.
 */
public class MicRecorder {
    private static final int SAMPLE_RATE_IN_HZ = 16000;
    private static final int MIC_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private AudioRecord mMicAudioRecord = null;

    private byte[] mic_audio_buf = new byte[MIC_BUFFER_SIZE];

    private boolean thread_mic_audio_active = false;
    private Thread thread_mic_audio = null;
    private final Listener mListener;

    public interface Listener
    {
        void onMicDataAvailable(byte[] mic_buf, int mic_audio_len);
    }

    public MicRecorder(Listener listener) {
        mListener = listener;
    }

    public void stop() {
        AppLog.i("thread_mic_audio: " + thread_mic_audio + "  thread_mic_audio_active: " + thread_mic_audio_active);
        if (thread_mic_audio_active) {
            thread_mic_audio_active = false;
            if (thread_mic_audio != null) {
                thread_mic_audio.interrupt();
            }
        }

        if (mMicAudioRecord != null) {
            mMicAudioRecord.stop();
            mMicAudioRecord.release();                                     // Release AudioTrack resources
            mMicAudioRecord = null;
        }
    }

    private int mic_audio_read(byte[] aud_buf, int max_len) {
        int len = 0;
        if (mMicAudioRecord == null) {
            return (len);
        }
        len = mMicAudioRecord.read(aud_buf, 0, max_len);
        if (len <= 0) {
            // If no audio data...
            if (len == android.media.AudioRecord.ERROR_INVALID_OPERATION)   // -3
                AppLog.e("get expected interruption error due to shutdown: " + len);
            else
                AppLog.e("get error: " + len);
            return (len);
        }

        mListener.onMicDataAvailable(aud_buf, len);
        return len;
    }

    public int start() {
        try {
            mMicAudioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, MIC_BUFFER_SIZE);
            mMicAudioRecord.startRecording();
            // Start input

            thread_mic_audio = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (thread_mic_audio_active) {
                            mic_audio_read(mic_audio_buf, MIC_BUFFER_SIZE);
                        }
                    }
            }, "mic_audio");

            thread_mic_audio_active = true;
            thread_mic_audio.start();
            return 0;
        } catch (Exception e) {
            AppLog.e(e);  // "java.lang.IllegalArgumentException: Invalid audio source."
            mMicAudioRecord = null;
            return -2;
        }
    }
}
