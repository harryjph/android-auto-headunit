package ca.yyx.hu.aap;

import ca.yyx.hu.utils.Utils;

/**
 * @author algavris
 * @date 04/10/2016.
 */
class AapMessageVideo extends AapMessage {
    final int total_size;

    AapMessageVideo(byte[] data, int size) {
        super(data, size);

        int total_size = (int) data[3];
        total_size += ((int) data[2] * 256);
        total_size += ((int) data[1] * 256 * 256);
        total_size += ((int) data[0] * 256 * 256 * 256);
        this.total_size = total_size;
        Utils.logv ("First fragment total_size: %d", total_size);
    }

    @Override
    int start() {
        return 8;
    }
}
