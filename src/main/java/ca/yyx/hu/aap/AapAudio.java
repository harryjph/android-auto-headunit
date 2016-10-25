package ca.yyx.hu.aap;

import android.media.AudioFormat;

import ca.yyx.hu.App;
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

    private byte aud_ack[] = {(byte) 0x80, 0x04, 0x08, 0, 0x10, 1};

    AapAudio(AapTransport transport, AudioDecoder audioDecoder) {
        mTransport = transport;
        mAudioDecoder = audioDecoder;
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
                        AppLog.logv("iaap_audio_process ts: %d 0x%x  t2: %d 0x%x", ts, ts, t2, t2);
                }
                AppLog.logv("iaap_audio_process ts: %d 0x%x  t2: %d 0x%x", ts, ts, t2, t2);
            }

            decode(chan, 10, buf, len - 10); // Decode PCM audio fully re-assembled
        }

        return (0);
    }


    private void decode(int chan, int start, byte[] buf, int len) {
        if (len > AUDIO_BUFS_SIZE) {
            AppLog.loge("Error audio len: %d  aud_buf_BUFS_SIZE: %d", len, AUDIO_BUFS_SIZE);
            len = AUDIO_BUFS_SIZE;
        }

        int channel = Channel.AA_CH_AUD;
        if (len <= 2048 + 96) {
            channel = Channel.AA_CH_AU1;
        }

        if (channel != chan)
        {
            AppLog.loge("Channels are different: %d != %d",channel,chan);
        } else {
            AppLog.logv("Channels are the same: %d ", chan);
        }

        if (mAudioDecoder.getTrack(channel) == null)
        {
            if (channel == Channel.AA_CH_AUD) {
                mAudioDecoder.start(channel, true);
            } else {
                mAudioDecoder.start(channel, false);
            }
        }

        mAudioDecoder.decode(channel, buf, start, len);
    }

    void setAudioAckVal(int chan, byte value) {
        if (chan == Channel.AA_CH_AUD) {
            ack_val_aud = value;
            // Save value for audio acks
        } else if (chan == Channel.AA_CH_AU1) {
            ack_val_au1 = value;
            // Save value for audio1 acks
        } else if (chan == Channel.AA_CH_AU2) {
            ack_val_au2 = value;
            // Save value for audio2 acks
        }
    }

    void stopAudio(int chan) {
        AppLog.logd("Audio Stop: " + chan);
        mAudioDecoder.stop(chan);
    }
}

