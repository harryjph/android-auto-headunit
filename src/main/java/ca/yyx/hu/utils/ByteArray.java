package ca.yyx.hu.utils;

/**
 * @author algavris
 * @date 24/09/2016.
 */

public class ByteArray {
    public final byte[] data;
    public final int offset;
    public int length;

    public ByteArray(int offset, byte[] data, int length)
    {
        this.data = data;
        this.length = length;
        this.offset = offset;
    }

    public ByteArray(int maxSize) {
        this.data = new byte[maxSize];
        this.length = 0;
        this.offset = 0;
    }

    public void put(int value) {
        this.data[this.length++] = (byte) value;
    }

    public void put(int... values) {
        for (int i = 0; i < values.length; i++)
        {
            this.put(values[i]);
        }
    }

    public void move(int size) {
        this.length += size;
    }

    public void inc(int index, int value) {
        this.data[index] += value;
    }

    public void encodeInt(int value) {
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

    public void reset()
    {
        this.length = 0;
    }

}
