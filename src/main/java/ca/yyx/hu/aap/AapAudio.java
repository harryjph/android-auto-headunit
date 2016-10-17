package ca.yyx.hu.aap;

import java.util.ArrayDeque;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapAudio {
    private final AapTransport mTransport;
    private final AudioDecoder mAudioDecoder;

    private static final int AUDIO_BUFS_SIZE = 65536 * 4;      // Up to 256 Kbytes

    private byte ack_val_aud = 0;
    private byte ack_val_au1 = 0;
    private byte ack_val_au2 = 0;

    private int out_state_aud = -1;
    private int out_state_au1 = -1;
    private int out_state_au2 = -1;

    private byte aud_ack[] = {(byte) 0x80, 0x04, 0x08, 0, 0x10, 1};

    AapAudio(AapTransport transport, AudioDecoder audioDecoder) {
        mTransport = transport;
        mAudioDecoder = audioDecoder;
    }
    // Global Ack: 0, 1     Same as video ack ?

    int state(int chan) {
        int state = 0;
        if (chan == Channel.AA_CH_AUD) {
            state = out_state_aud;                                            // Get current audio output state change
            out_state_aud = -1;                                               // Reset audio output state change indication
        } else if (chan == Channel.AA_CH_AU1) {
            state = out_state_au1;                                            // Get current audio output state change
            out_state_au1 = -1;                                               // Reset audio output state change indication
        } else if (chan == Channel.AA_CH_AU2) {
            state = out_state_au2;                                            // Get current audio output state change
            out_state_au2 = -1;                                               // Reset audio output state change indication
        }
        return (state);                                                     // Return what the new state was before reset
    }

    public int process(AapMessage message) {
        return process(message.channel, message.type, message.flags, message.data, message.length);
    }

    //int aud_ack_ctr = 0;
    private int process(int chan, int msg_type, int flags, byte[] buf, int len) {
        // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600

        //logd ("iaap_audio_process chan: %d  msg_type: %d  flags: 0x%x  buf: %p  len: %d", chan, msg_type, flags, buf, len); // iaap_audio_process msg_type: 0  flags: 0xb  buf: 0xe08cbfb8  len: 8202

        if (chan == Channel.AA_CH_AU1)
            aud_ack[3] = ack_val_au1;
        else if (chan == Channel.AA_CH_AU2)
            aud_ack[3] = ack_val_au2;
        else
            aud_ack[3] = ack_val_aud;

        int ret = mTransport.sendEncrypted(chan, aud_ack, aud_ack.length);      // Respond with ACK (for all fragments ?)

        //hex_dump ("AUDIO: ", 16, buf, len);
        if (len >= 10) {
            int ctr = 0;
            long ts = 0, t2 = 0;
            for (ctr = 2; ctr <= 9; ctr++) {
                ts = ts << 8;
                t2 = t2 << 8;
                ts += (long) buf[ctr];
                t2 += buf[ctr];
                if (ctr == 6)
                    AppLog.logv("iaap_audio_process ts: %d 0x%x  t2: %d 0x%x", ts, ts, t2, t2);
            }
            AppLog.logv("iaap_audio_process ts: %d 0x%x  t2: %d 0x%x", ts, ts, t2, t2);

            decode(chan, 10, buf, len - 10);//assy, assy_size);                                                                                    // Decode PCM audio fully re-assembled
        }

        return (0);
    }


    private void decode(int chan, int start, byte[] buf, int len) {
        if (len > AUDIO_BUFS_SIZE) {
            AppLog.loge("Error audio len: %d  aud_buf_BUFS_SIZE: %d", len, AUDIO_BUFS_SIZE);
            len = AUDIO_BUFS_SIZE;
        }

        mAudioDecoder.decode(buf, start, len);
    }

    void setAudioAckVal(int chan, byte value) {
        if (chan == Channel.AA_CH_AUD)
            ack_val_aud = value;                                            // Save value for audio acks
        else if (chan == Channel.AA_CH_AU1)
            ack_val_au1 = value;                                            // Save value for audio1 acks
        else if (chan == Channel.AA_CH_AU2)
            ack_val_au2 = value;                                            // Save value for audio2 acks
    }

    void setOutState(int chan, int state) {
        if (chan == Channel.AA_CH_AUD)
            out_state_aud = state;                                                      // Signal Audio stop
        else if (chan == Channel.AA_CH_AU1)
            out_state_au1 = state;                                                      // Signal Audio1 stop
        else if (chan == Channel.AA_CH_AU2)
            out_state_au2 = state;                                                      // Signal Audio2 stop
    }
}

