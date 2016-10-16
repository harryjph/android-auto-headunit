package ca.yyx.hu.aap;


/**
 * @author algavris
 * @date 04/10/2016.
 */
class AapMessage extends ByteArray {

    final int channel;
    final byte flags;
    final int type;

    AapMessage(int channel, byte flags, int msg_type, byte[] data, int size) {
        super(size);
        this.data = data;
        this.length = size;
        this.channel = channel;
        this.flags = flags;
        this.type = msg_type;
    }

    public boolean isAudio() {
        return Channel.isAudio(this.channel);
    }

    public boolean isVideo() {
        return this.channel == Channel.AA_CH_VID;
    }
}