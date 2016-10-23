package ca.yyx.hu.aap;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;

import java.util.Locale;

import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

public class AapTransport extends HandlerThread implements Handler.Callback {
    private static final int POLL = 1;
    private static final int DATA_MESSAGE = 2;
    private static final int MIC_RECORD_START = 3;
    private static final int MIC_RECORD_STOP = 4;
    private final Listener mListener;
    private final AapAudio mAapAudio;
    private final AapVideo mAapVideo;

    private Handler mHandler;
    private boolean mMicRecording;

    private final AudioDecoder mAudioDecoder;
    private final MicRecorder mMicRecorder;
    private final AapControl mAapControl;

    private boolean mStopped;
    private UsbAccessoryConnection mConnection;
    private AapPoll mAapPoll;

    public interface Listener {
        void gainVideoFocus();
    }

    public AapTransport(AudioDecoder audioDecoder, VideoDecoder videoDecoder, Listener listener) {
        super("AapTransport");
        mAudioDecoder = audioDecoder;
        mMicRecorder = new MicRecorder();
        mAapAudio = new AapAudio(this, audioDecoder);
        mAapVideo = new AapVideo(this, videoDecoder);
        mAapControl = new AapControl(this, mAapAudio);
        mListener = listener;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler(getLooper(), this);
        mHandler.sendEmptyMessage(POLL);
    }

    @Override
    public boolean handleMessage(Message msg) {
        int ret;

        if (msg.what == MIC_RECORD_STOP) {
            mMicRecording = false;
        } else if (msg.what == MIC_RECORD_START) {
            mMicRecording = true;
        }

        if (mMicRecording) {
            byte[] mic_buf = Protocol.createMicBuffer();
            int mic_audio_len = mMicRecorder.mic_audio_read(mic_buf, 10, MicRecorder.MIC_BUFFER_SIZE);
            if (mic_audio_len >= 78) {                                    // If we read at least 64 bytes of audio data
                Utils.put_time(2, mic_buf, SystemClock.elapsedRealtime());
                ret = sendEncrypted(Channel.AA_CH_MIC, mic_buf, mic_audio_len);    // Send mic audio
            } else if (mic_audio_len > 0) {
                AppLog.loge("No data from microphone");
            }
        }

        if (msg.what == DATA_MESSAGE) {
            int channel = msg.arg1;
            int dataLength = msg.arg2;
            byte[] data = (byte[]) msg.obj;
            ret = sendEncrypted(channel, data, dataLength);
            if (ret < 0) {
                AppLog.loge("Send data result: " + ret);
            }
        }


        // Send a command (or null command)
        ret = mAapPoll.poll();

        if (mHandler == null) {
            return false;
        }
        if (isAlive() && !mHandler.hasMessages(POLL)) {
            mHandler.sendEmptyMessage(POLL);
        }

        if (ret < 0) {
            AppLog.loge("Error result: " + ret);
            this.quit();
        }

        return true;
    }

    @Override
    public boolean quit() {

        if (mConnection != null) {
            sendEncrypted(Channel.AA_CH_CTR, Protocol.BYEBYE_REQUEST, Protocol.BYEBYE_REQUEST.length);
        }
        Utils.ms_sleep(100);
        if (mHandler != null) {
            mHandler.removeCallbacks(this);
            mHandler = null;
        }
        mStopped = true;
        return super.quit();
    }


    public boolean connectAndStart(UsbAccessoryConnection connection) {
        AppLog.logd("Start Aap transport for " + connection);

        if (!handshake(connection))
        {
            AppLog.loge("Handshake failed");
            return false;
        }

        mConnection = connection;
        mAapPoll = new AapPoll(connection, this, mAapAudio, mAapVideo);
        this.start();                                          // Create and start Transport Thread
        return true;
    }

    private boolean handshake(UsbAccessoryConnection connection) {
        byte[] buffer = new byte[Protocol.DEF_BUFFER_LENGTH];

        // Version request
        ByteArray version = Protocol.createMessage(0, 3, 1, Protocol.VERSION_REQUEST, Protocol.VERSION_REQUEST.length); // Version Request
        int ret = connection.send(version.data, version.length, 1000);
        if (ret < 0) {
            AppLog.loge("Version request sendEncrypted ret: " + ret);
            return false;
        }

        ret = connection.recv(buffer, 1000);
        if (ret <= 0) {
            AppLog.loge("Version request recv ret: " + ret);
            return false;
        }
        AppLog.logd("Version response recv ret: %d", ret);

        // SSL
        ret = AapSsl.prepare();
        if (ret < 0) {
            AppLog.loge("SSL prepare failed: " + ret);
            return false;
        }

        int hs_ctr = 0;
        // SSL_is_init_finished (hu_ssl_ssl)

        while (hs_ctr++ < 2)
        {
            AapSsl.handshake();
            ByteArray ba = AapSsl.bioRead();
            if (ba == null) {
                return false;
            }

            ByteArray bio = Protocol.createMessage(Channel.AA_CH_CTR, 3, 3, ba.data, ba.length);
            int size = connection.send(bio.data, bio.length, 1000);
            AppLog.logd("SSL BIO sent: %d", size);

            size = connection.recv(buffer, 1000);
            AppLog.logd("SSL received: %d", size);
            if (size <= 0) {
                AppLog.logd("SSL receive error");
                return false;
            }

            ret = AapSsl.bioWrite(6, size - 6, buffer);
            AppLog.logd("SSL BIO write: %d", ret);
        }

        // Status = OK
        // byte ac_buf [] = {0, 3, 0, 4, 0, 4, 8, 0};
        ByteArray status = Protocol.createMessage(0, 3, 4, new byte[]{8, 0}, 2);
        ret = connection.send(status.data, status.length, 1000);
        if (ret < 0) {
            AppLog.loge("Status request sendEncrypted ret: " + ret);
            return false;
        }

        AppLog.logd("Status OK sent: %d", ret);

        return true;
    }

    void micStop() {
        AppLog.logd("Microphone Stop");
        mHandler.sendEmptyMessage(MIC_RECORD_STOP);
        mMicRecorder.mic_audio_stop();
    }

    void micStart() {
        AppLog.logd("Microphone Start");
        mHandler.sendEmptyMessage(MIC_RECORD_START);
    }

    void sendTouch(byte action, int x, int y) {
        if (mHandler != null) {
            long ts = SystemClock.elapsedRealtime() * 1000000L;
            ByteArray ba = Protocol.createTouchMessage(ts, action, x, y);
            Message msg = mHandler.obtainMessage(DATA_MESSAGE, Channel.AA_CH_TOU, ba.length, ba.data);
            mHandler.sendMessage(msg);
        }
    }

    public boolean isStopped() {
        return mStopped;
    }

    public void sendButton(int btnCode, boolean isPress) {
        if (mHandler != null)
        {
            long ts = SystemClock.elapsedRealtime() * 1000000L;
            // Timestamp in nanoseconds = microseconds x 1,000,000
            ByteArray ba = Protocol.createButtonMessage(ts, btnCode, isPress);
            Message msg = mHandler.obtainMessage(DATA_MESSAGE, Channel.AA_CH_TOU, ba.length, ba.data);
            mHandler.sendMessage(msg);
        }
    }

    void sendNightMode(boolean enabled) {
        if (mHandler != null) {
            byte[] modeData = Protocol.createNightModeMessage(enabled);
            Message msg = mHandler.obtainMessage(DATA_MESSAGE, Channel.AA_CH_SEN, modeData.length, modeData);
            mHandler.sendMessage(msg);
        }
    }

    int sendEncrypted(int chan, byte[] buf, int len) {
        int flags = 0x0b;                                                   // Flags = First + Last + Encrypted
        if (chan != Channel.AA_CH_CTR && buf[0] == 0) {                            // If not control channel and msg_type = 0 - 255 = control type message
            flags = 0x0f;                                                     // Set Control Flag (On non-control channels, indicates generic/"control type" messages
        }
        if (chan == Channel.AA_CH_MIC && buf[0] == 0 && buf[1] == 0) {            // If Mic PCM Data
            flags = 0x0b;                                                     // Flags = First + Last + Encrypted
        }

        String prefix = String.format(Locale.US, "SEND %d %s %01x", chan, Channel.name(chan), flags);
        AapDump.logv(prefix, "HU", chan, flags, buf, len);

        ByteArray ba = AapSsl.encrypt(4, len, buf);

        ByteArray msg = Protocol.createMessage(chan, flags, -1, ba.data, ba.length);
        int size = mConnection.send(msg.data, msg.length, 250);
        AppLog.logv("Sent size: %d", size);

        if (AppLog.LOG_VERBOSE) {
            AapDump.logvHex("US", 0, msg.data, msg.length);
        }
        return 0;
    }

    void gainVideoFocus()
    {
        mListener.gainVideoFocus();
    }

    void sendVideoFocusGained() {
        // Else if success and channel = video...
        byte rsp2[] = {(byte) 0x80, 0x08, 0x08, 1, 0x10, 1};
        // 1, 1     VideoFocus gained focusState=1 unsolicited=true     010b0000800808011001
        sendEncrypted(Channel.AA_CH_VID, rsp2, rsp2.length);
        // Respond with VideoFocus gained
    }

    void sendVideoFocusLost() {
        // Else if success and channel = video...
        byte rsp2[] = {(byte) 0x80, 0x08, 0x08, 1, 0x10, 0};
        // 1, 1     VideoFocus gained focusState=1 unsolicited=true     010b0000800808011001
        sendEncrypted(Channel.AA_CH_VID, rsp2, rsp2.length);
        // Respond with VideoFocus gained
    }
}

