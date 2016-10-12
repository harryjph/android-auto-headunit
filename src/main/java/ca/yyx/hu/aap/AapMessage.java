package ca.yyx.hu.aap;


import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 04/10/2016.
 */
class AapMessage extends ByteArray {

    final int channel;
    final byte flags;
    final int length_encrypted;
    int message_type;

    static AapMessage create(byte[] data,int size)
    {
        int channel = (int) data[0];
        byte flags = data[1];

        AapMessage message;
        if (channel == Channel.AA_CH_VID && flags == 9) {                            // If First fragment Video... (Packet is encrypted so we can't get the real msg_type or check for 0, 0, 0, 1)
            message = new AapMessageVideo(data, size);
        } else {
            message = new AapMessage(data, size);
        }

        if ((flags & 0x08) != 0x08) {
            Utils.loge ("NOT ENCRYPTED !!!!!!!!! size: %d", size);
        }

        int need_len = message.length_encrypted - size;
        if (need_len > 0) {                                         // If we need more data for the full packet...
            Utils.loge ("have_len: %d < enc_len: %d  need_len: %d", size, message.length_encrypted, need_len);
        }

        return message;
    }

    AapMessage(byte[] data, int size) {
        super(size);
        this.data = data;
        this.length = size;
        this.channel = (int) data[0];
        this.flags = data[1];

        // Encoded length of bytes to be decrypted (minus 4/8 byte headers)
        int enc_len = (int) data[3];
        enc_len += ((int) data[2] * 256);

        // Message Type (or post handshake, mostly indicator of SSL encrypted data)
        int msg_type = (int) data[5];
        msg_type += ((int) data[4] * 256);

        this.length_encrypted = enc_len;
        this.message_type = msg_type;
    }

    int start() {
        return 4;
    }

    void decrypt(byte[] data, int length) {
        this.data = data;
        this.length = length;
        int msg_type = (int) data[1];
        msg_type += ((int) data[0] * 256);
        this.message_type = msg_type;
        //System.arraycopy(data, 0, this.data, this.start(), length);
        //this.length = this.start() + length;
    }
}