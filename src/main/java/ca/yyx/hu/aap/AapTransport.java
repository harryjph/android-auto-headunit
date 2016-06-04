
// Headunit app Transport: USB / Wifi

package ca.yyx.hu.aap;

import java.nio.ByteBuffer;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.utils.Utils;

public class AapTransport {

    private final AudioDecoder mAudioDecoder;
    private final MicRecorder mMicRecorder;
    private final VideoDecoder mVideoDecoder;
    private TransportThread mTraThread;                                 // Transport Thread

    private static final int AA_CH_CTR = 0;                               // Sync with AapTransport.java, hu_aap.h and hu_aap.c:aa_type_array[]
    private static final int AA_CH_TOU = 3;
    private static final int AA_CH_SEN = 1;
    private static final int AA_CH_VID = 2;

    private boolean aapRunning = false;

    public boolean isAapStarted() {
        return aapRunning;
    }

    public AapTransport(AudioDecoder audioDecoder, VideoDecoder videoDecoder) {
        mAudioDecoder = audioDecoder;
        mVideoDecoder = videoDecoder;
        mMicRecorder = new MicRecorder();
    }

    static {
        System.loadLibrary("hu_jni");
    }

    // Java_ca_yyx_hu_aap_AapTransport_native_1aa_1cmd
    private static native int native_aa_cmd(int cmd_len, byte[] cmd_buf, int res_len, byte[] res_buf);

    private boolean m_mic_active = false;
    private boolean touch_sync = true;//      // Touch sync times out within 200 ms on second touch with TCP for some reason.

    public void mediaSkipToNext() {

    }

    public void mediaSkipToPrevious() {

    }

    private final class TransportThread extends Thread {                       // Main Transport Thread
        private volatile boolean m_stopping = false;                        // Set true when stopping

        TransportThread() {
            super("TransportThread");                                             // Name thread
        }

        @Override
        public void run() {
            int ret = 0;

            while (!m_stopping) {                                            // Loop until stopping...

                if (m_stopping) {
                    break;
                }

                if (touch_sync && new_touch && len_touch > 0 && ba_touch != null) {
                    ret = aa_cmd_send(len_touch, ba_touch, 0, null);           // Send touch event
                    ba_touch = null;
                    new_touch = false;
                    continue;
                }

                if (ret >= 0 && m_mic_active) {                 // If Mic active...
                    byte[] ba_mic = new byte[14 + MicRecorder.MIC_BUFFER_SIZE];
                    int mic_audio_len = mMicRecorder.mic_audio_read(ba_mic, 14, MicRecorder.MIC_BUFFER_SIZE);
                    if (mic_audio_len >= 64) {                                    // If we read at least 64 bytes of audio data
                        ba_mic[0] = MicRecorder.AA_CH_MIC;// Mic channel
                        ba_mic[1] = 0x0b;  // Flag filled here
                        ba_mic[2] = 0x00;  // 2 bytes Length filled here
                        ba_mic[3] = 0x00;
                        ba_mic[4] = 0x00;  // Message Type = 0 for data, OR 32774 for Stop w/mandatory 0x08 int and optional 0x10 int (senderprotocol/aq -> com.google.android.e.b.ca)
                        ba_mic[5] = 0x00;

                        long ts = android.os.SystemClock.elapsedRealtime();        // ts = Timestamp (uptime) in microseconds
                        for (int ctr = 7; ctr >= 0; ctr--) {                           // Fill 8 bytes backwards
                            ba_mic[6 + ctr] = (byte) (ts & 0xFF);
                            ts = ts >> 8;
                        }

                        ret = aa_cmd_send(14 + mic_audio_len, ba_mic, 0, null);    // Send mic audio
                    } else if (mic_audio_len > 0) {
                        Utils.loge("No data from microphone");
                    }
                }

                if (ret >= 0) {
                    ret = aa_cmd_send(0, null, 0, null);                         // Null message to just poll
                }
                if (ret < 0) {
                    m_stopping = true;
                }
            }

            byebye_send();                                                   // If m_stopping then... Byebye
        }

        void quit() {
            m_stopping = true;                                                // Terminate thread
        }
    }

    public void stop() {                                       // USB Transport Stop. Called only by AapActivity.all_stop()
        if (aapRunning) {
            aapRunning = false;
        }

        byebye_send();                                                     // Terminate AA Protocol with ByeBye

        if (mTraThread != null) {                                           // If Transport Thread...
            mTraThread.quit();                                             // Terminate Transport Thread using it's quit() API
        }
    }

    public boolean start(UsbAccessoryConnection connection) {
        Utils.logd("Start Aap transport for "+connection);
        // Start JNI Android Auto Protocol and Main Thread.
        byte[] cmd_buf = {121, -127, 2};
        // Start Request w/ m_ep_in_addr, m_ep_out_addr
        cmd_buf[1] = (byte) connection.getEndpointInAddr();
        cmd_buf[2] = (byte) connection.getEndpointOutAddr();
        // Send: Start USB & AA
        int ret = aa_cmd_send(cmd_buf.length, cmd_buf, 0, null);

        if (ret == 0) {                                                     // If started OK...
            aapRunning = true;
            mTraThread = new TransportThread();
            mTraThread.start();                                          // Create and start Transport Thread
            return true;
        }
        Utils.loge("Cannot start AAP ret:" + ret);
        return false;
    }

    private int byebye_send() {                                          // Send Byebye request. Called only by stop (), TransportThread:run()
        Utils.logd("");
        byte[] cmd_buf = {AA_CH_CTR, 0x0b, 0, 0, 0, 0x0f, 0x08, 0};          // Byebye Request:  000b0004000f0800  00 0b 00 04 00 0f 08 00
        int ret = aa_cmd_send(cmd_buf.length, cmd_buf, 0, null);           // Send
        Utils.ms_sleep(100);                                              // Wait a bit for response
        return (ret);
    }

    private byte[] fixed_cmd_buf = new byte[256];
    private byte[] fixed_res_buf = new byte[65536 * 16];

    // Send AA packet/HU command/mic audio AND/OR receive video/output audio/audio notifications
    private int aa_cmd_send(int cmd_len, byte[] cmd_buf, int res_len, byte[] res_buf) {
        if (cmd_buf == null || cmd_len <= 0) {
            cmd_buf = fixed_cmd_buf;//new byte [256];// {0};                                  // Allocate fake buffer to avoid problems
            cmd_len = 0;//cmd_buf.length;
        }
        if (res_buf == null || res_len <= 0) {
            res_buf = fixed_res_buf;//new byte [65536 * 16];  // Seen up to 151K so far; leave at 1 megabyte
            res_len = res_buf.length;
        }

        int ret = native_aa_cmd(cmd_len, cmd_buf, res_len, res_buf);       // Send a command (or null command)

        if (ret == 1) {                                                     // If mic stop...
            Utils.logd("Microphone Stop");
            m_mic_active = false;
            mMicRecorder.mic_audio_stop();
            return (0);
        } else if (ret == 2) {                                                // Else if mic start...
            Utils.logd("Microphone Start");
            m_mic_active = true;
            return (0);
        } else if (ret == 3) {                                                // Else if audio stop...
            Utils.logd("Audio Stop");
            mAudioDecoder.out_audio_stop(AudioDecoder.AA_CH_AUD);
            return (0);
        } else if (ret == 4) {                                                // Else if audio1 stop...
            Utils.logd("Audio1 Stop");
            mAudioDecoder.out_audio_stop(AudioDecoder.AA_CH_AU1);
            return (0);
        } else if (ret == 5) {                                                // Else if audio2 stop...
            Utils.logd("Audio2 Stop");
            mAudioDecoder.out_audio_stop(AudioDecoder.AA_CH_AU2);
            return (0);
        } else if (ret > 0) {
            handleMedia(res_buf, ret);
        }
        return (ret);
    }

    private void handleMedia(byte[] buffer, int size) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.limit(size);
        bb.position(0);

        if (VideoDecoder.isH246Video(buffer)) {
            Utils.logd("Video");
            mVideoDecoder.decode(bb);
        } else {
            Utils.logd("Audio");
            mAudioDecoder.decode(bb);
        }
    }

    private long last_move_ms = 0;
    private int len_touch = 0;
    private boolean new_touch = false;
    private byte[] ba_touch = null;

    public void touch_send(byte action, int x, int y) {                  // Touch event send. Called only by AapActivity:touch_send()

        if (!aapRunning) {
            return;
        }

        int err_ctr = 0;
        while (new_touch) {                                                 // While previous touch not yet processed...
            if (err_ctr++ % 5 == 0)
                Utils.logd("Waiting for new_touch = false");
            if (err_ctr > 20) {
                Utils.loge("Timeout waiting for new_touch = false");
                boolean touch_timeout_force = true;
                if (touch_timeout_force)
                    new_touch = false;
                else
                    return;
            }
            Utils.ms_sleep(50);                                             // Wait a bit
        }

        ba_touch = new byte[]{AA_CH_TOU, 0x0b, 0x00, 0x00, -128, 0x01, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0, 0, 0, 0x1a, 0x0e, 0x0a, 0x08, 0x08, 0x2e, 0, 0x10, 0x2b, 0, 0x18, 0x00, 0x10, 0x00, 0x18, 0x00};

        long ts = android.os.SystemClock.elapsedRealtime() * 1000000L;   // Timestamp in nanoseconds = microseconds x 1,000,000

        int siz_arr = 0;

        int idx = 1 + 6 + Utils.varint_encode(ts, ba_touch, 1 + 6);          // Encode timestamp

        ba_touch[idx++] = 0x1a;                                           // Value 3 array
        int size1_idx = idx;                                                // Save size1_idx
        ba_touch[idx++] = 0x0a;                                           // Default size 10
//
        ba_touch[idx++] = 0x0a;                                           // Contents = 1 array
        int size2_idx = idx;                                                // Save size2_idx
        ba_touch[idx++] = 0x04;                                           // Default size 4
        //
        ba_touch[idx++] = 0x08;                                             // Value 1
        siz_arr = Utils.varint_encode(x, ba_touch, idx);                 // Encode X
        idx += siz_arr;
        ba_touch[size1_idx] += siz_arr;                                    // Adjust array sizes for X
        ba_touch[size2_idx] += siz_arr;

        ba_touch[idx++] = 0x10;                                             // Value 2
        siz_arr = Utils.varint_encode(y, ba_touch, idx);                 // Encode Y
        idx += siz_arr;
        ba_touch[size1_idx] += siz_arr;                                    // Adjust array sizes for Y
        ba_touch[size2_idx] += siz_arr;

        ba_touch[idx++] = 0x18;                                             // Value 3
        ba_touch[idx++] = 0x00;                                           // Encode Z ?
        //
        ba_touch[idx++] = 0x10;
        ba_touch[idx++] = 0x00;

        ba_touch[idx++] = 0x18;
        ba_touch[idx++] = action;
//
        int ret = 0;
        if (!touch_sync) {                                               // If allow sending from different thread
            ret = aa_cmd_send(idx, ba_touch, 0, null);                     // Send directly
            return;
        }

        len_touch = idx;
        new_touch = true;
    }
}

