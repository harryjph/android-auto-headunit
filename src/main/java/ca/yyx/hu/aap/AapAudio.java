package ca.yyx.hu.aap;

import android.media.AudioManager;
import android.util.SparseIntArray;

import ca.yyx.hu.aap.protocol.AudioConfigs;
import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapAudio implements AudioManager.OnAudioFocusChangeListener {
    private final AapTransport mTransport;
    private final AudioDecoder mAudioDecoder;

    private static final int AUDIO_BUFS_SIZE = 65536 * 4;      // Up to 256 Kbytes
    private final AudioManager mAudioManager;

    private SparseIntArray mSessionIds = new SparseIntArray(3);

    private byte aud_ack[] = {(byte) 0x80, 0x04, 0x08, 0, 0x10, 1};

    AapAudio(AapTransport transport, AudioDecoder audioDecoder, AudioManager audioManager) {
        mTransport = transport;
        mAudioDecoder = audioDecoder;
        mAudioManager = audioManager;
    }

    void requestFocusChange(int channel, int focusRequest)
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
        return process(message.channel, message.type, message.flags, message.data, message.length);
    }

    //int aud_ack_ctr = 0;
    private int process(int chan, int msg_type, int flags, byte[] buf, int len) {
        // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600

        //i ("iaap_audio_process chan: %d  msg_type: %d  flags: 0x%x  buf: %p  len: %d", chan, msg_type, flags, buf, len); // iaap_audio_process msg_type: 0  flags: 0xb  buf: 0xe08cbfb8  len: 8202

        aud_ack[3] = (byte) mSessionIds.get(chan);

        int ret = mTransport.sendEncrypted(chan, aud_ack, aud_ack.length);
        // Respond with ACK (for all fragments ?)

        //hex_dump ("AUDIO: ", 16, buf, len);
        if (len >= 10) {
            if (AppLog.LOG_VERBOSE) {
                int ctr = 0;
                long ts = 0, t2 = 0;
                for (ctr = 2; ctr <= 9; ctr++) {
                    ts = ts << 8;
                    t2 = t2 << 8;
                    ts += (long) buf[ctr];
                    t2 += buf[ctr];
                    if (ctr == 6)
                        AppLog.v("iaap_audio_process ts: %d 0x%x  t2: %d 0x%x", ts, ts, t2, t2);
                }
                AppLog.v("iaap_audio_process ts: %d 0x%x  t2: %d 0x%x", ts, ts, t2, t2);
            }

            decode(chan, 10, buf, len - 10); // Decode PCM audio fully re-assembled
        }

        return (0);
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

    void setSessionId(int channel, int sessionId) {
        mSessionIds.put(channel, sessionId);
    }
}

