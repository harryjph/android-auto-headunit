package ca.yyx.hu.aap;

import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.SparseIntArray;

import com.google.protobuf.nano.MessageNano;

import net.hockeyapp.android.utils.Util;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.MsgType;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;

public class AapTransport implements Handler.Callback, MicRecorder.Listener {
    private static final int POLL = 1;
    private static final int MSG_DATA = 2;

    private final Listener mListener;
    private final AapAudio mAapAudio;
    private final AapVideo mAapVideo;
    private final HandlerThread mPollThread;
    private final MicRecorder mMicRecorder;
    private final String mBtMacAddress;
    private final SparseIntArray mSessionIds = new SparseIntArray(4);

    private AccessoryConnection mConnection;
    private AapPoll mAapPoll;
    private Handler mHandler;

    public interface Listener {
        void gainVideoFocus();
    }

    public AapTransport(AudioDecoder audioDecoder, VideoDecoder videoDecoder, AudioManager audioManager, String btMacAddress, Listener listener) {

        mPollThread = new HandlerThread("AapTransport:Handler", Process.THREAD_PRIORITY_AUDIO);

        mMicRecorder = new MicRecorder(this);
        mAapAudio = new AapAudio(audioDecoder, audioManager);
        mAapVideo = new AapVideo(videoDecoder);
        mBtMacAddress = btMacAddress;
        mListener = listener;
    }

    public boolean isAlive() {
        return mPollThread.isAlive();
    }

    public boolean handleMessage(Message msg) {

        if (msg.what == MSG_DATA) {
            int size = msg.arg2;
            byte[] data = (byte[]) msg.obj;
            this.sendEncryptedMessage(data, size);
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
                this.quit();
            }
        }

        return true;
    }

    private int sendEncryptedMessage(byte[] data, int length) {
        ByteArray ba = AapSsl.encrypt(AapMessage.HEADER_SIZE, length - AapMessage.HEADER_SIZE, data);
        if (ba == null) {
            return -1;
        }

        ba.data[0] = data[0];
        ba.data[1] = data[1];
        Utils.intToBytes(ba.length - AapMessage.HEADER_SIZE, 2, ba.data);

        int size = mConnection.send(ba.data, ba.length, 250);

        if (AppLog.LOG_VERBOSE) {
            AppLog.v("Sent size: %d", size);
            AapDump.logvHex("US", 0, ba.data, ba.length);
        }
        return 0;
    }

    void quit() {
        mPollThread.quit();
        mHandler = null;
    }

    boolean connectAndStart(AccessoryConnection connection) {
        AppLog.i("Start Aap transport for " + connection);

        if (!handshake(connection)) {
            AppLog.e("Handshake failed");
            return false;
        }

        mConnection = connection;

        mAapPoll = new AapPoll(connection, this, mMicRecorder, mAapAudio, mAapVideo, mBtMacAddress);
        mPollThread.start();
        mHandler = new Handler(mPollThread.getLooper(), this);
        mHandler.sendEmptyMessage(POLL);
        // Create and start Transport Thread
        return true;
    }

    private boolean handshake(AccessoryConnection connection) {
        byte[] buffer = new byte[Messages.DEF_BUFFER_LENGTH];

        // Version request

        byte[] version = Messages.createRawMessage(0, 3, 1, Messages.VERSION_REQUEST, Messages.VERSION_REQUEST.length); // Version Request
        int ret = connection.send(version, version.length, 1000);
        if (ret < 0) {
            AppLog.e("Version request sendEncrypted ret: " + ret);
            return false;
        }

        ret = connection.recv(buffer, buffer.length, 1000);
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

            byte[] bio = Messages.createRawMessage(Channel.AA_CH_CTR, 3, 3, ba.data, ba.length);
            int size = connection.send(bio, bio.length, 1000);
            AppLog.i("SSL BIO sent: %d", size);

            size = connection.recv(buffer, buffer.length, 1000);
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
        byte[] status = Messages.createRawMessage(0, 3, 4, new byte[]{8, 0}, 2);
        ret = connection.send(status, status.length, 1000);
        if (ret < 0) {
            AppLog.e("Status request sendEncrypted ret: " + ret);
            return false;
        }

        AppLog.i("Status OK sent: %d", ret);

        return true;
    }

    void sendTouch(byte action, int x, int y) {
        long ts = SystemClock.elapsedRealtime();
        send(Messages.createTouchEvent(ts, action, x, y));
    }

    public void sendButton(int keyCode, boolean isPress) {
        long ts = SystemClock.elapsedRealtime();
        send(Messages.createButtonEvent(ts, keyCode, isPress));
    }

    void sendNightMode(boolean enabled) {
        send(Messages.createNightModeEvent(enabled));
    }

    void send(AapMessage message) {
        if (mHandler == null) {
            AppLog.e("Handler is null");
        } else {
            if (AppLog.LOG_VERBOSE) {
                AppLog.v(message.toString());
            }
            Message msg = mHandler.obtainMessage(MSG_DATA, 0, message.size, message.data);
            mHandler.sendMessage(msg);
        }
    }

    void gainVideoFocus()
    {
        mListener.gainVideoFocus();
    }

    void sendVideoFocusGained(boolean unsolicited) {
        AppLog.i("Gain video focus notification");
        send(Messages.createVideoFocus(1, unsolicited));
    }

    void sendVideoFocusLost() {
        AppLog.i("Lost video focus notification");
        send(Messages.createVideoFocus(2, true));
    }

    void sendMediaAck(int channel) {
        send(Messages.createMediaAck(channel, mSessionIds.get(channel)));
    }

    void setSessionId(int channel, int sessionId) {
        mSessionIds.put(channel, sessionId);
    }

    @Override
    public void onMicDataAvailable(byte[] mic_buf, int mic_audio_len) {
        if (mic_audio_len > 64) {  // If we read at least 64 bytes of audio data
            int length = mic_audio_len + 10;
            byte[] data = new byte[length];
            Utils.put_time(2, data, SystemClock.elapsedRealtime());
            System.arraycopy(mic_buf, 0, data, 10, mic_audio_len);
            send(new AapMessage(Channel.AA_CH_MIC, 0x0b, length, data));
        }
    }

}

