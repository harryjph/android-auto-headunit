package ca.yyx.hu.aap;

import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import java.util.ArrayList;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.aap.protocol.messages.KeyCodeEvent;
import ca.yyx.hu.aap.protocol.messages.MediaAck;
import ca.yyx.hu.aap.protocol.messages.Messages;
import ca.yyx.hu.aap.protocol.messages.NightModeEvent;
import ca.yyx.hu.aap.protocol.messages.ScrollWheelEvent;

import ca.yyx.hu.aap.protocol.messages.SensorEvent;
import ca.yyx.hu.aap.protocol.nano.Protocol;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.decoder.AudioDecoder;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.decoder.VideoDecoder;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.NightMode;
import ca.yyx.hu.utils.Settings;
import ca.yyx.hu.utils.Utils;

public class AapTransport implements Handler.Callback, MicRecorder.Listener {
    private static final int MSG_POLL = 1;
    private static final int MSG_SEND = 2;

    private final Listener mListener;
    private final AapAudio mAapAudio;
    private final AapVideo mAapVideo;
    private final HandlerThread mPollThread;
    private final MicRecorder mMicRecorder;
    private final Settings mSettings;
    private final SparseIntArray mSessionIds = new SparseIntArray(4);
    private final ArrayList<Integer> mStartedSensors = new ArrayList<>(4);
    private final AapSslNative mSsl = new AapSslNative();

    private AccessoryConnection mConnection;
    private AapRead mAapRead;
    private Handler mHandler;

    void startSensor(int type) {
        mStartedSensors.add(type);
        if (type == Protocol.SENSOR_TYPE_NIGHT) {
            Utils.ms_sleep(2);
            NightMode nm = new NightMode(mSettings);
            AppLog.i("Send night mode");
            send(new NightModeEvent(nm.current()));
            AppLog.i(nm.toString());
        }
    }

    public interface Listener {
        void gainVideoFocus();
    }

    public AapTransport(AudioDecoder audioDecoder, VideoDecoder videoDecoder, AudioManager audioManager, Settings settings, Listener listener) {

        mPollThread = new HandlerThread("AapTransport:Handler", Process.THREAD_PRIORITY_AUDIO);

        mMicRecorder = new MicRecorder();
        mMicRecorder.setListener(this);
        mAapAudio = new AapAudio(audioDecoder, audioManager);
        mAapVideo = new AapVideo(videoDecoder);
        mSettings = settings;
        mListener = listener;
    }

    public boolean isAlive() {
        return mPollThread.isAlive();
    }

    public boolean handleMessage(Message msg) {

        if (msg.what == MSG_SEND) {
            int size = msg.arg2;
            byte[] data = (byte[]) msg.obj;
            this.sendEncryptedMessage(data, size);
            return true;
        }

        if (msg.what == MSG_POLL) {
            int ret = mAapRead.read();
            if (mHandler == null) {
                return false;
            }
            if (!mHandler.hasMessages(MSG_POLL)) {
                mHandler.sendEmptyMessage(MSG_POLL);
            }

            if (ret < 0) {
                this.quit();
            }
        }

        return true;
    }

    private int sendEncryptedMessage(byte[] data, int length) {
        ByteArray ba = mSsl.encrypt(AapMessage.HEADER_SIZE, length - AapMessage.HEADER_SIZE, data);
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
        mMicRecorder.setListener(null);
        mPollThread.quit();
        mAapRead = null;
        mHandler = null;
    }

    boolean connectAndStart(AccessoryConnection connection) {
        AppLog.i("Start Aap transport for " + connection);

        if (!handshake(connection)) {
            AppLog.e("Handshake failed");
            return false;
        }

        mConnection = connection;

        mAapRead = AapRead.Factory.create(connection, this, mMicRecorder, mAapAudio, mAapVideo, mSettings);

        mPollThread.start();
        mHandler = new Handler(mPollThread.getLooper(), this);
        mHandler.sendEmptyMessage(MSG_POLL);
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
        ret = mSsl.prepare();
        if (ret < 0) {
            AppLog.e("SSL prepare failed: " + ret);
            return false;
        }

        int hs_ctr = 0;
        // SSL_is_init_finished (hu_ssl_ssl)

        while (hs_ctr++ < 2)
        {
            mSsl.handshake();
            ByteArray ba = mSsl.bioRead();
            if (ba == null) {
                return false;
            }

            byte[] bio = Messages.createRawMessage(Channel.ID_CTR, 3, 3, ba.data, ba.length);
            int size = connection.send(bio, bio.length, 1000);
            AppLog.i("SSL BIO sent: %d", size);

            size = connection.recv(buffer, buffer.length, 1000);
            AppLog.i("SSL received: %d", size);
            if (size <= 0) {
                AppLog.i("SSL receive error");
                return false;
            }

            ret = mSsl.bioWrite(6, size - 6, buffer);
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

    public void sendButton(int keyCode, boolean isPress) {
        long ts = SystemClock.elapsedRealtime();

        int aapKeyCode = KeyCode.convert(keyCode);

        if (aapKeyCode == KeyEvent.KEYCODE_UNKNOWN)
        {
            AppLog.i("Unknown: " + keyCode);
        }

        if (aapKeyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
        {
            if (isPress)
            {
                int delta = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? -1 : 1;
                send(new ScrollWheelEvent(ts, delta));
            }
            return;
        }

        send(new KeyCodeEvent(ts, keyCode, isPress));
    }

    public void send(SensorEvent sensor)
    {
        if (mStartedSensors.contains(sensor.type)) {
            send((AapMessage)sensor);
        } else {
            AppLog.e("Sensor "+sensor.type+" is not started yet");
        }
    }

    public void send(AapMessage message) {
        if (mHandler == null) {
            AppLog.e("Handler is null");
        } else {
            if (AppLog.LOG_VERBOSE) {
                AppLog.v(message.toString());
            }
            Message msg = mHandler.obtainMessage(MSG_SEND, 0, message.size, message.data);
            mHandler.sendMessage(msg);
        }
    }

    void gainVideoFocus()
    {
        mListener.gainVideoFocus();
    }

    void sendMediaAck(int channel) {
        send(new MediaAck(channel, mSessionIds.get(channel)));
    }

    void setSessionId(int channel, int sessionId) {
        mSessionIds.put(channel, sessionId);
    }

    @Override
    public void onMicDataAvailable(byte[] mic_buf, int mic_audio_len) {
        if (mic_audio_len > 64) {  // If we read at least 64 bytes of audio data
            int length = mic_audio_len + 10;
            byte[] data = new byte[length];
            data[0] = Channel.ID_MIC;
            data[1] = 0x0b;
            Utils.put_time(2, data, SystemClock.elapsedRealtime());
            System.arraycopy(mic_buf, 0, data, 10, mic_audio_len);
            send(new AapMessage(Channel.ID_MIC, (byte)0x0b, -1, 2, length, data));
        }
    }

}

