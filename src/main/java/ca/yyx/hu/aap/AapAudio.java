package ca.yyx.hu.aap;

import android.media.AudioManager;
import android.util.SparseIntArray;

import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapAudio implements AudioManager.OnAudioFocusChangeListener {
    private final AudioDecoder mAudioDecoder;

    private static final int AUDIO_BUFS_SIZE = 65536 * 4;      // Up to 256 Kbytes
    private final AudioManager mAudioManager;

    AapAudio(AudioDecoder audioDecoder, AudioManager audioManager) {
        mAudioDecoder = audioDecoder;
        mAudioManager = audioManager;
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    void requestFocusChange(int focusRequest)
    {
        int stream = AudioManager.STREAM_MUSIC;
        if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_RELEASE) {
            mAudioManager.abandonAudioFocus(this);
        } else if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN) {
            mAudioManager.requestAudioFocus(this, stream, AudioManager.AUDIOFOCUS_GAIN);
        } else if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_GAIN_TRANSIENT) {
            mAudioManager.requestAudioFocus(this, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        } else if (focusRequest == Protocol.AudioFocusRequestNotification.AUDIO_FOCUS_UNKNOWN) {
            mAudioManager.requestAudioFocus(this, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }

    public int process(AapMessage message) {
        if (message.length >= 10) {
            decode(message.channel, 10, message.data, message.length - 10);
        }

        return 0;
    }

    private void decode(int channel, int start, byte[] buf, int len) {
        if (len > AUDIO_BUFS_SIZE) {
            AppLog.e("Error audio len: %d  aud_buf_BUFS_SIZE: %d", len, AUDIO_BUFS_SIZE);
            len = AUDIO_BUFS_SIZE;
        }

        if (mAudioDecoder.getTrack(channel) == null)
        {
            Protocol.AudioConfiguration config = AudioConfigs.get(channel);
            int stream = AudioManager.STREAM_MUSIC;
            mAudioDecoder.start(channel, stream, config.sampleRate, config.numberOfBits, config.numberOfChannels);
        }

        mAudioDecoder.decode(channel, buf, start, len);
    }

    void stopAudio(int chan) {
        AppLog.i("Audio Stop: " + chan);
        mAudioDecoder.stop(chan);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        AppLog.i("" + focusChange);
    }
}

