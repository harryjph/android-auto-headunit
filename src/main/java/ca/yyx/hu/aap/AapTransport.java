package ca.yyx.hu.aap;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

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

    private static final int AA_CH_CTR = 0;                               // Sync with AapTransport.java, hu_aap.h and hu_aap.c:aa_type_array[]
    private static final int AA_CH_TOU = 3;
    private static final int AA_CH_SEN = 1;
    private static final int AA_CH_VID = 2;

    private MessageThread mThread;

    public boolean isAapStarted() {
        return mThread != null;
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

    public void mediaSkipToNext() {

    }

    public void mediaSkipToPrevious() {

    }

    private final class MessageThread extends HandlerThread implements Handler.Callback {
        private static final int POLL = 1;
        private static final int TOUCH_SYNC = 2;
        private static final int MIC_RECORD_START = 3;
        private static final int MIC_RECORD_STOP = 4;

        private byte[] fixed_cmd_buf = new byte[256];
        private byte[] fixed_res_buf = new byte[65536 * 16];


        private Handler mHandler;
        private boolean mMicRecording;

        MessageThread() {
            super("MessageThread");
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mHandler = new Handler(getLooper(), this);
            mHandler.sendEmptyMessage(POLL);
        }

        @Override
        public boolean handleMessage(Message msg) {
            int ret = 0;

            Utils.logd(""+msg);

            if (msg.what == MIC_RECORD_STOP) {
                mMicRecording = false;
            } else if (msg.what == MIC_RECORD_START) {
                mMicRecording = true;
            }

            if (mMicRecording) {
                byte[] mic_buf = Utils.createMicBuffer();
                int mic_audio_len = mMicRecorder.mic_audio_read(mic_buf, 14, MicRecorder.MIC_BUFFER_SIZE);
                if (mic_audio_len >= 78) {                                    // If we read at least 64 bytes of audio data
                    Utils.put_time(6, mic_buf, SystemClock.elapsedRealtime());
                    ret = aa_cmd_send(mic_audio_len, mic_buf, fixed_res_buf.length, fixed_res_buf);    // Send mic audio
                } else if (mic_audio_len > 0) {
                    Utils.loge("No data from microphone");
                }
            }

            if (msg.what == TOUCH_SYNC)
            {
                int touchLength = msg.arg1;
                byte[] touchData = (byte[]) msg.obj;
                ret = aa_cmd_send(touchLength, touchData, fixed_res_buf.length, fixed_res_buf);
            }

            ret = aa_cmd_send(0, fixed_cmd_buf, fixed_res_buf.length, fixed_res_buf);
            if (isAlive()) {
                mHandler.sendEmptyMessageDelayed(POLL, 10);
            }
            return false;
        }

        @Override
        public synchronized void start() {
            super.start();
        }

        @Override
        public boolean quit() {
            byebye_send();
            return super.quit();
        }

        public void sendTouch(int len_touch, byte[] touchData)
        {
            Message msg = mHandler.obtainMessage(TOUCH_SYNC, len_touch, 0, touchData);
            mHandler.sendMessage(msg);
        }

        public void setMicRecording(boolean start)
        {
            mHandler.sendEmptyMessage(start ? MIC_RECORD_START : MIC_RECORD_STOP);
        }
    }

    public void stop() {
        if (mThread != null) {
            mThread.quit();
            mThread = null;
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

        int ret = aa_cmd_send(cmd_buf.length, cmd_buf);

        if (ret == 0) {                                                     // If started OK...
            mThread = new MessageThread();
            mThread.start();                                          // Create and start Transport Thread
            return true;
        }
        Utils.loge("Cannot start AAP ret:" + ret);
        return false;
    }

    private int byebye_send() {                                          // Send Byebye request. Called only by stop (), TransportThread:run()
        Utils.logd("");
        byte[] cmd_buf = {AA_CH_CTR, 0x0b, 0, 0, 0, 0x0f, 0x08, 0};          // Byebye Request:  000b0004000f0800  00 0b 00 04 00 0f 08 00
        int ret = aa_cmd_send(cmd_buf.length, cmd_buf);           // Send
        Utils.ms_sleep(100);                                              // Wait a bit for response
        return (ret);
    }

    private int aa_cmd_send(int cmd_len,@NonNull byte[] cmd_buf) {
        byte[] res_buf = new byte[65536 * 16];
        return aa_cmd_send(cmd_len, cmd_buf, res_buf.length, res_buf);
    }

    // Send AA packet/HU command/mic audio AND/OR receive video/output audio/audio notifications
    private int aa_cmd_send(int cmd_len,@NonNull byte[] cmd_buf, int res_len,@NonNull byte[] res_buf) {

        int ret = native_aa_cmd(cmd_len, cmd_buf, res_len, res_buf);       // Send a command (or null command)

        if (ret == 1) {                                                     // If mic stop...
            Utils.logd("Microphone Stop");
            mThread.setMicRecording(false);
            mMicRecorder.mic_audio_stop();
            return (0);
        } else if (ret == 2) {                                                // Else if mic start...
            Utils.logd("Microphone Start");
            mThread.setMicRecording(true);
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
        return ret;
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

    public void touch_send(byte action, int x, int y) {                  // Touch event send. Called only by AapActivity:touch_send()
        sendTouch(action, x, y);
    }

    private void sendTouch(byte action, int x, int y)
    {
        byte[] ba_touch = new byte[] {
                AA_CH_TOU, 0x0b, 0x00, 0x00, -128, 0x01,
                0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0, 0, 0, 0x1a, 0x0e, 0x0a, 0x08, 0x08, 0x2e,
                0, 0x10, 0x2b, 0, 0x18, 0x00, 0x10, 0x00, 0x18, 0x00
        };

        long ts = SystemClock.elapsedRealtime() * 1000000L;   // Timestamp in nanoseconds = microseconds x 1,000,000

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
        int siz_arr = Utils.varint_encode(x, ba_touch, idx);                 // Encode X
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

        int len_touch = idx;
        mThread.sendTouch(len_touch, ba_touch);
    }
}

