package ca.yyx.hu.aap;


/**
 * @author algavris
 * @date 04/10/2016.
 */
class AapMessage extends ByteArray {

    final int channel;
    final byte flags;
    int message_type;

    AapMessage(int channel, byte flags, int msg_type, byte[] data, int size) {
        super(size);
        this.data = data;
        this.length = size;
        this.channel = channel;
        this.flags = flags;
        this.message_type = msg_type;
    }

}