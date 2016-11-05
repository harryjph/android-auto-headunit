package ca.yyx.hu.aap;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;

import java.util.Locale;

import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.ByteArray;
import ca.yyx.hu.utils.Utils;

public class AapTransport implements Handler.Callback, MicRecorder.Listener {
    private static final int POLL = 1;
    private static final int MSG_DATA = 2;

    private final Listener mListener;
    private final AapAudio mAapAudio;
    private final AapVideo mAapVideo;
    private final HandlerThread mPollThread;
    private final MicRecorder mMicRecorder;

    private AccessoryConnection mConnection;
    private AapPoll mAapPoll;
    private Handler mHandler;

    public interface Listener {
        void gainVideoFocus();
    }

    public AapTransport(AudioDecoder audioDecoder, VideoDecoder videoDecoder, Listener listener) {

        mPollThread = new HandlerThread("AapTransport:Handler", Process.THREAD_PRIORITY_AUDIO);

        mMicRecorder = new MicRecorder(this);
        mAapAudio = new AapAudio(this, audioDecoder);
        mAapVideo = new AapVideo(this, videoDecoder);
        mListener = listener;
    }

    public boolean isAlive() {
        return mPollThread.isAlive();
    }

    public boolean handleMessage(Message msg) {

        if (msg.what == MSG_DATA) {
            int chan = msg.arg1;
            int len = msg.arg2;
            byte[] data = (byte[]) msg.obj;
            this.sendEncryptedMessage(chan, data, len);
            return true;
        }

        if (msg.what == POLL) {
            int ret = mAapPoll.poll();
            if (mHandler == null) {
                return false;
            }
            if (!mHandler.hasMessages(POLL)) {
                mHandler.sendEmptyMessage(POLL);
            }

            if (ret < 0) {
                AppLog.e("Error result: " + ret);
                this.quit();
            }
        }

        return true;
    }

    private int sendEncryptedMessage(int chan, byte[] buf, int len) {
        int flags = 0x0b;                                                   // Flags = First + Last + Encrypted
        if (chan != Channel.AA_CH_CTR && buf[0] == 0) {                            // If not control channel and msg_type = 0 - 255 = control type message
            flags = 0x0f;                                                     // Set Control Flag (On non-control channels, indicates generic/"control type" messages
        }
        if (chan == Channel.AA_CH_MIC && buf[0] == 0 && buf[1] == 0) {            // If Mic PCM Data
            flags = 0x0b;                                                     // Flags = First + Last + Encrypted
        }

        String prefix = String.format(Locale.US, "SEND %d %s %01x", chan, Channel.name(chan), flags);
        AapDump.logd(prefix, "HU", chan, flags, buf, len);

        ByteArray ba = AapSsl.encrypt(4, len, buf);

        ByteArray msg = Protocol.createMessage(chan, flags, -1, ba.data, ba.length);
        int size = mConnection.send(msg.data, msg.length, 250);
        AppLog.d("Sent size: %d", size);

        if (AppLog.LOG_VERBOSE) {
            AapDump.logvHex("US", 0, msg.data, msg.length);
        }
        return 0;
    }

    void quit() {
        if (mConnection != null) {
            sendEncrypted(Channel.AA_CH_CTR, Protocol.BYEBYE_REQUEST, Protocol.BYEBYE_REQUEST.length);
        }
        Utils.ms_sleep(100);

        mPollThread.quit();
        mHandler = null;
    }

    public boolean connectAndStart(AccessoryConnection connection) {
        AppLog.i("Start Aap transport for " + connection);

        if (!handshake(connection))
        {
            AppLog.e("Handshake failed");
            return false;
        }

        mConnection = connection;

        mAapPoll = new AapPoll(connection, this, mAapAudio, mAapVideo);
        mPollThread.start();
        mHandler = new Handler(mPollThread.getLooper(), this);
        mHandler.sendEmptyMessage(POLL);
        // Create and start Transport Thread
        return true;
    }

    private boolean handshake(AccessoryConnection connection) {
        byte[] buffer = new byte[Protocol.DEF_BUFFER_LENGTH];

        // Version request
        ByteArray version = Protocol.createMessage(0, 3, 1, Protocol.VERSION_REQUEST, Protocol.VERSION_REQUEST.length); // Version Request
        int ret = connection.send(version.data, version.length, 1000);
        if (ret < 0) {
            AppLog.e("Version request sendEncrypted ret: " + ret);
            return false;
        }

        ret = connection.recv(buffer, 1000);
        if (ret <= 0) {
            AppLog.e("Version request recv ret: " + ret);
            return false;
        }
        AppLog.i("Version response recv ret: %d", ret);

        // SSL
        ret = AapSsl.prepare();
        if (ret < 0) {
            AppLog.e("SSL prepare failed: " + ret);
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
            AppLog.i("SSL BIO sent: %d", size);

            size = connection.recv(buffer, 1000);
            AppLog.i("SSL received: %d", size);
            if (size <= 0) {
                AppLog.i("SSL receive error");
                return false;
            }

            ret = AapSsl.bioWrite(6, size - 6, buffer);
            AppLog.i("SSL BIO write: %d", ret);
        }

        // Status = OK
        // byte ac_buf [] = {0, 3, 0, 4, 0, 4, 8, 0};
        ByteArray status = Protocol.createMessage(0, 3, 4, new byte[]{8, 0}, 2);
        ret = connection.send(status.data, status.length, 1000);
        if (ret < 0) {
            AppLog.e("Status request sendEncrypted ret: " + ret);
            return false;
        }

        AppLog.i("Status OK sent: %d", ret);

        return true;
    }

    void micStop() {
        AppLog.i("Microphone Stop");
        mMicRecorder.stop();
    }

    void micStart() {
        AppLog.i("Microphone Start");
        mMicRecorder.start();
    }

    void sendTouch(byte action, int x, int y) {
        long ts = SystemClock.elapsedRealtime() * 1000000L;
        ByteArray ba = Protocol.createTouchMessage(ts, action, x, y);
        sendEncrypted(Channel.AA_CH_TOU, ba.data, ba.length);
    }

    public void sendButton(int btnCode, boolean isPress) {
        long ts = SystemClock.elapsedRealtime() * 1000000L;
        // Timestamp in nanoseconds = microseconds x 1,000,000
        ByteArray ba = Protocol.createButtonMessage(ts, btnCode, isPress);
        sendEncrypted(Channel.AA_CH_TOU, ba.data, ba.length);
    }

    void sendNightMode(boolean enabled) {
        byte[] modeData = Protocol.createNightModeMessage(enabled);
        sendEncrypted(Channel.AA_CH_SEN, modeData, modeData.length);
    }

    int sendEncrypted(int chan, byte[] buf, int len) {
        if (mHandler == null) {
            AppLog.e("Handler is null");
        } else {
            Message msg = mHandler.obtainMessage(MSG_DATA, chan, len, buf);
            mHandler.sendMessage(msg);
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
        byte rsp2[] = {(byte) 0x80, 0x08, 0x08, 1, 0x10, 0};
        sendEncrypted(Channel.AA_CH_VID, rsp2, rsp2.length);
    }

    @Override
    public void onMicDataAvailable(byte[] mic_buf, int mic_audio_len) {
        if (mic_audio_len > 64) {  // If we read at least 64 bytes of audio data
            ByteArray ba = new ByteArray(mic_audio_len + 10);
            Utils.put_time(2, ba.data, SystemClock.elapsedRealtime());
            ba.length = 10;
            ba.put(0, mic_buf, mic_audio_len);
            sendEncrypted(Channel.AA_CH_MIC, ba.data, ba.length);
        }
    }

}

