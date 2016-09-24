package ca.yyx.hu.aap;

/**
 * @author algavris
 * @date 24/09/2016.
 */

class ByteArray {
    byte[] data;
    int length;

    ByteArray(int maxSize) {
        this.data = new byte[maxSize];
        this.length = 0;
    }

    void put(int value) {
        this.data[this.length++] = (byte) value;
    }

    void put(int... values) {
        for (int i = 0; i < values.length; i++)
        {
            this.put(values[i]);
        }
    }

    void move(int size) {
        this.length += size;
    }

    void inc(int index, int value) {
        this.data[index] += value;
    }
}
