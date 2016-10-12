package ca.yyx.hu.aap;

import java.util.Locale;

import ca.yyx.hu.usb.UsbAccessoryConnection;
import ca.yyx.hu.utils.Utils;


/**
 * @author algavris
 * @date 01/10/2016.
 */

class AapPoll {
    private final UsbAccessoryConnection mConnection;
    private final AapTransport mTransport;

    private static final int DEFBUF = 131080;
    private final AapAudio mAapAudio;
    private final AapMicrophone mAapMicrophone;
    private final AapVideo mAapVideo;
    private final AapControl mAapControl;

    private byte[] fixed_res_buf = new byte[DEFBUF];

    AapPoll(UsbAccessoryConnection connection, AapTransport transport) {
        mConnection = connection;
        mTransport = transport;
        mAapAudio = new AapAudio(transport);
        mAapVideo = new AapVideo(transport);
        mAapMicrophone = new AapMicrophone();
        mAapControl = new AapControl(transport, mAapAudio, mAapMicrophone);
    }

    int poll()
    {
        if (mConnection == null)
        {
            return -1;
        }
        int size = mConnection.recv(fixed_res_buf, 150);
        if (size <= 0) {
            Utils.loge ("RECV %d", size);
            return 0;
        }
        int result = process(size, fixed_res_buf);
        mTransport.onPollResult(result, fixed_res_buf);
        return 0;
    }


    private int process(int msg_len, byte[] msg_buf) {
        int res_len = 0;
        int ret = 0;

        int vid_bufs = mAapVideo.buffersCount();
        int aud_bufs = mAapAudio.buffersCount();

        // If any queue audio or video...
        if (vid_bufs > 0 || aud_bufs > 0) {
            ret = 0;
        } else {
            // Else Process 1 message w/ iaap_tra_recv_tmo
            ret = hu_aap_recv_process(msg_len, msg_buf);
        }
        if (ret < 0) {
            return ret;
        }

        if (vid_bufs <= 0 && aud_bufs <= 0) {                               // If no queued audio or video...
            ret = mAapMicrophone.hu_aap_mic_get();
            if (ret >= 1) {// && ret <= 2) {                                  // If microphone start (2) or stop (1)...
                return ret;                                                   // Done w/ mic notification: start (2) or stop (1)
            }
            // Else if no microphone state change...

            if (mAapAudio.state(Channel.AA_CH_AUD) >= 0)                              // If audio out stop...
                return 3;                                                     // Done w/ audio out notification 0
            if (mAapAudio.state(Channel.AA_CH_AU1) >= 0)                              // If audio out stop...
                return 4;                                                     // Done w/ audio out notification 1
            if (mAapAudio.state(Channel.AA_CH_AU2) >= 0)                              // If audio out stop...
                return 5;                                                     // Done w/ audio out notification 2
        }

        ByteArray dq_buf = mAapAudio.aud_read_head_buf_get();                         // Get audio if ready
        if (dq_buf == null) {                                           // If no audio... (Audio has priority over video)
            dq_buf = mAapVideo.vid_read_head_buf_get();                       // Get video if ready
        } else {                                                              // If audio
            if (dq_buf.data[0] == 0 && dq_buf.data[1] == 0 && dq_buf.data[2] == 0 && dq_buf.data[3] == 1) {
                dq_buf.data[3] = 0;                                                   // If audio happened to have magic video signature... (rare), then 0 the 1
                Utils.loge ("magic video signature in audio");
            }
        }

        if (dq_buf == null) {
            return 0;
        }

        System.arraycopy(dq_buf.data, 0, msg_buf, 0, dq_buf.length);

        return res_len;
    }


    private int hu_aap_recv_process(int msg_len, byte[] msg_buf) {

        int idx = 0;
        int have_len = msg_len;                                                   // Length remaining to process for all sub-packets plus 4/8 byte headers

        int chan = (int) msg_buf[0];                                         // Channel
        int flags = msg_buf[1];                                              // Flags

        int enc_len = (int) msg_buf[3];                                      // Encoded length of bytes to be decrypted (minus 4/8 byte headers)
        enc_len += ((int) msg_buf[2] * 256);

        int msg_type = (int) msg_buf[5];                                     // Message Type (or post handshake, mostly indicator of SSL encrypted data)
        msg_type += ((int) msg_buf[4] * 256);

        have_len -= 4;                                                    // Length starting at byte 4: Unencrypted Message Type or Encrypted data start
        idx += 4;                                                         // buf points to data to be decrypted
        if ((flags & 0x08) != 0x08) {
            Utils.loge ("NOT ENCRYPTED !!!!!!!!! have_len: %d  enc_len: %d  buf: %p  chan: %d %s  flags: 0x%02X  msg_type: %d", have_len, enc_len, msg_buf, chan, Channel.name(chan), flags, msg_type);
            return (-1);
        }
        if (chan == Channel.AA_CH_VID && flags == 9) {                            // If First fragment Video... (Packet is encrypted so we can't get the real msg_type or check for 0, 0, 0, 1)
            int total_size = (int) msg_buf[3];
            total_size += ((int) msg_buf[2] * 256);
            total_size += ((int) msg_buf[1] * 256 * 256);
            total_size += ((int) msg_buf[0] * 256 * 256 * 256);

            Utils.logv ("First fragment total_size: %d", total_size);

            have_len -= 4;                                                  // Remove 4 length bytes inserted into first video fragment
            idx += 4;
        }
        int need_len = enc_len - have_len;
        if (need_len > 0) {                                         // If we need more data for the full packet...
            Utils.loge ("have_len: %d < enc_len: %d  need_len: %d", have_len, enc_len, need_len);
        }

        int ret = iaap_recv_dec_process(chan, flags, idx, enc_len, msg_buf);          // Decrypt & Process 1 received encrypted message
        if (ret < 0) {                                                    // If error...
            Utils.loge ("Error iaap_recv_dec_process() ret: %d  have_len: %d  enc_len: %d  buf: %p  chan: %d %s  flags: %02X  msg_type: %d", ret, have_len, enc_len, msg_buf, chan, Channel.name(chan), flags, msg_type);
            return ret;
        }

        have_len -= enc_len;                                              // Consume processed sub-packet and advance to next, if any
        if (have_len != 0) {
            Utils.logd ("iaap_recv_dec_process() more than one message have_len: %d  enc_len: %d", have_len, enc_len);
        }
        return ret;                                                       // Return value from the last iaap_recv_dec_process() call; should be 0
    }

    private int iaap_recv_dec_process(int chan, int flags, int start, int enc_len, byte[] buf) {// Decrypt & Process 1 received encrypted message

        int bytes_written = mTransport.sslBioWrite(start, enc_len, buf);
        // Write encrypted to SSL input BIO
        if (bytes_written <= 0) {
            Utils.loge ("BIO_write() bytes_written: %d", bytes_written);
            return (-1);
        }

        int bytes_read = mTransport.sslRead(fixed_res_buf, fixed_res_buf.length);
        // Read decrypted to decrypted rx buf
        if (bytes_read <= 0) {
            Utils.loge ("SSL_read bytes_read: %d", bytes_read);
            return -1;
        }

        String prefix = String.format(Locale.US, "RECV %d %s %01x", chan, Channel.name(chan), flags);
        AapDump.log(prefix, "AA", chan, flags, buf, enc_len);

        iaap_msg_process(chan, flags, fixed_res_buf, bytes_read);      // Process decrypted AA protocol message
        return 0;
    }

    private int iaap_msg_process(int chan, int flags, byte[] buf, int len) {

        int msg_type = (int) buf[1];
        msg_type += ((int) buf[0] * 256);

        if ((Channel.isAudio(chan)) && (msg_type == 0 || msg_type == 1)) {
            return (mAapAudio.process(chan, msg_type, flags, buf, len)); // 300 ms @ 48000/sec   samples = 14400     stereo 16 bit results in bytes = 57600
        } else if (chan == Channel.AA_CH_VID && msg_type == 0 || msg_type == 1 || flags == 8 || flags == 9 || flags == 10) {    // If Video...
            return (mAapVideo.process(msg_type, flags, buf, len));
        } else if ((msg_type >= 0 && msg_type <= 31) || (msg_type >= 32768 && msg_type <= 32799) || (msg_type >= 65504 && msg_type <= 65535)) {
            mAapControl.execute(chan, msg_type, buf, len);
        } else {
            Utils.loge ("Unknown msg_type: %d", msg_type);
        }

        return 0;
    }
}
