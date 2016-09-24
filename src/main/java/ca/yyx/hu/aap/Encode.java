package ca.yyx.hu.aap;

/**
 * @author algavris
 * @date 24/09/2016.
 */

class Encode {

    static int intToByteArray(int value, byte[] ba, int start)
    {
        byte cbyte;
        int enc_size = 0;

        do {
            cbyte = (byte) (value & 0x7f);
            value >>= 7;
            if (value != 0) {
                cbyte |= 0x80;
            }
            ba[start + enc_size] = cbyte;
            enc_size++;
        } while (value != 0);

        return enc_size;
    }

    static int longToByteArray(long val, byte[] ba, int start) {
        if (val >= 0x7fffffffffffffffL) {
            return 1;
        }

        long left = val;
        for (int idx = 0; idx < 9; idx ++) {
            ba [start+idx] = (byte) (0x7f & left);
            left = left >> 7;
            if (left == 0) {
                return (idx + 1);
            }
            else if (idx < 9 - 1) {
                ba [start+idx] |= 0x80;
            }
        }

        return 9;
    }
}
