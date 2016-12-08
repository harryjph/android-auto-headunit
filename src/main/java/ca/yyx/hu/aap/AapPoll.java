package ca.yyx.hu.aap;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.util.Locale;

import ca.yyx.hu.aap.protocol.Channel;
import ca.yyx.hu.connection.AccessoryConnection;
import ca.yyx.hu.decoder.MicRecorder;
import ca.yyx.hu.utils.AppLog;
import ca.yyx.hu.utils.Utils;


/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapPoll {

    private final AccessoryConnection mConnection;
    private final AapTransport mTransport;

    private byte[] recv_buffer = new byte[Messages.DEF_BUFFER_LENGTH];
    private final Header recv_header = new Header();

    private final AapAudio mAapAudio;
    private final AapVideo mAapVideo;
    private final AapControl mAapControl;

    AapPoll(AccessoryConnection connection, AapTransport transport, MicRecorder recorder, AapAudio aapAudio, AapVideo aapVideo, String btMacAddress) {
        mConnection = connection;
        mAapAudio = aapAudio;
        mAapVideo = aapVideo;
        mTransport = transport;
        mAapControl = new AapControl(transport, recorder, mAapAudio, btMacAddress);
    }

    int poll() {
        if (mConnection == null) {
            AppLog.e("Error: No connection.");
            return -1;
        }

        if (mConnection.isSingleMessage()) {
            int header_size = mConnection.recv(recv_buffer, Header.SIZE, 150);
            if (header_size != Header.SIZE) {
                AppLog.v("Header: recv %d", header_size);
                return -1;
            }

            recv_header.decode(0, recv_buffer);

            int msg_size = mConnection.recv(recv_buffer, recv_header.enc_len, 150);
            if (msg_size != recv_header.enc_len) {
                AppLog.v("Message: recv %d", msg_size);
                return -1;
            }

            try {
                return processSingle(recv_header, 0, recv_buffer);
            } catch (InvalidProtocolBufferNanoException e) {
                AppLog.e(e);
                return -1;
            }
        }

        int size = mConnection.recv(recv_buffer, recv_buffer.length, 150);
        if (size <= 0) {
            AppLog.v("recv %d", size);
            return 0;
        }
        try {
            processBulk(size, recv_buffer);
        } catch (InvalidProtocolBufferNanoException e) {
            AppLog.e(e);
            return -1;
        }
        return 0;
    }

    private int processSingle(Header header, int offset, byte[] buf) throws InvalidProtocolBufferNanoException {
        AapMessage msg = iaap_recv_dec_process(header, offset, buf);
        // Decrypt & Process 1 received encrypted message
        if (msg == null) {
            // If error...
            AppLog.e("Error iaap_recv_dec_process: enc_len: %d chan: %d %s flags: %01x msg_type: %d", header.enc_len, header.chan, Channel.name(header.chan), header.flags, header.msg_type);
            return -1;
        }

        iaap_msg_process(msg);
        return 0;
    }

    private RecvProcessResult processBulk(int buf_len, byte[] buf) throws InvalidProtocolBufferNanoException {

        int body_start = 0;
        int have_len = buf_len;

        while (have_len > 0) {
            int msg_start = body_start;
            recv_header.decode(body_start, buf);

            // Length starting at byte 4: Unencrypted Message Type or Encrypted data start
            have_len -= 4;
            body_start +=4;

            if (recv_header.chan == Channel.AA_CH_VID && recv_header.flags == 9) {
                have_len -= 4;
            }

            int need_len = recv_header.enc_len - have_len;
            if (need_len > 0) {
                AppLog.e("Need more - offset: %d, msg_start: %d, enc_len: %d. need_len: %d, buf_len: %d, buf_limit: %d",
                        body_start + have_len, // 6436
                        msg_start, // 4
                        recv_header.enc_len, // 26816
                        need_len, // 20384
                        buf_len, // 6436
                        buf.length); // 131080


                return RecvProcessResult.NeedMore
                        .setNeedMore(msg_start, have_len, need_len);
            }

            processSingle(recv_header, body_start, buf);

            have_len -= recv_header.enc_len;
            body_start += recv_header.enc_len;
            if (have_len != 0) {
                AppLog.i("iaap_recv_dec_process() more than one message have_len: %d  enc_len: %d", have_len, recv_header.enc_len);
            }

        }

        return RecvProcessResult.Ok;
    }

    private AapMessage iaap_recv_dec_process(Header header, int offset, byte[] buf) {
        // Decrypt & Process 1 received encrypted message

        if ((header.flags & 0x08) != 0x08) {
            AppLog.e("WRONG FLAG: have_len: %d  enc_len: %d  chan: %d %s  flags: 0x%02x  msg_type: %d", header.enc_len, header.chan, Channel.name(header.chan), header.flags, header.msg_type);
            return null;
        }
        if (header.chan == Channel.AA_CH_VID && recv_header.flags == 9) {
            // If First fragment Video...
            // (Packet is encrypted so we can't get the real msg_type or check for 0, 0, 0, 1)
            int total_size = Utils.bytesToInt(buf, offset, false);
            AppLog.v("First fragment total_size: %d", total_size);
            offset += 4;
        }

        ByteArray ba = AapSsl.decrypt(offset, header.enc_len, buf);
        if (ba == null) {
            return null;
        }

        String prefix = String.format(Locale.US, "RECV %d %s %01x", header.chan, Channel.name(header.chan), header.flags);
        AapDump.logd(prefix, "AA", header.chan, header.flags, ba.data, ba.length);

        int msg_type = Utils.bytesToInt(ba.data, 0, true);
        return new AapMessage(header.chan, (byte) header.flags, msg_type, 2, ba.length, ba.data);
    }

    private int iaap_msg_process(AapMessage message) throws InvalidProtocolBufferNanoException {

        int msg_type = message.type;
        byte flags = message.flags;

        if (message.isAudio() && (msg_type == 0 || msg_type == 1)) {
            mTransport.sendMediaAck(message.channel);
            return mAapAudio.process(message);
            // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
        } else if (message.isVideo() && msg_type == 0 || msg_type == 1 || flags == 8 || flags == 9 || flags == 10) {
            mTransport.sendMediaAck(message.channel);
            return mAapVideo.process(message);
        } else if ((msg_type >= 0 && msg_type <= 31) || (msg_type >= 32768 && msg_type <= 32799) || (msg_type >= 65504 && msg_type <= 65535)) {
            mAapControl.execute(message);
        } else {
            AppLog.e("Unknown msg_type: %d", msg_type);
        }

        return 0;
    }

    static class Header
    {
        final static int SIZE = 6;

        int chan;
        int flags;
        int enc_len;
        int msg_type;

        void decode(int offset, byte[] buf) {
            this.chan = (int) buf[offset];
            this.flags = buf[offset + 1];

            // Encoded length of bytes to be decrypted (minus 4/8 byte headers)
            this.enc_len = Utils.bytesToInt(buf, offset + 2, true);

            // Message Type (or post handshake, mostly indicator of SSL encrypted data)
            this.msg_type = Utils.bytesToInt(buf, offset + 4, true);
        }

    }

    private static class RecvProcessResult
    {
        static final RecvProcessResult Error = new RecvProcessResult(-1);
        static final RecvProcessResult Ok = new RecvProcessResult(0);
        static final RecvProcessResult NeedMore = new RecvProcessResult(1);

        int result;
        int have_length;
        int need_length;
        int start;

        RecvProcessResult(int result) {
            this.result = result;
        }

        RecvProcessResult setNeedMore(int start, int have_length, int need_length) {
            this.start = start;
            this.need_length = need_length;
            this.have_length = have_length;
            return this;
        }
    }
}
