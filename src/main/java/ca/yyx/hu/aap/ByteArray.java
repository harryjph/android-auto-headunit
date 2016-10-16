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

    void encodeInt(int value) {
        this.put((byte) (value / 256));                                            // Encode length of following data:
        this.put((byte) (value % 256));
    }

    public void put(byte[] data, int size) {
        this.put(0, data, size);
    }

    public void put(int start, byte[] data, int size) {
        System.arraycopy(data, start, this.data, this.length, size);
        this.length += size;
    }
}
