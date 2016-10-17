package ca.yyx.hu.decoder;

import java.nio.ByteBuffer;

import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 23/09/2016.
 */

class NalUnitsStore {
    private final static int TOTAL_CAPTURE_COUNT = 16;

    private byte[][] mCapturedNal = new byte[TOTAL_CAPTURE_COUNT][];
    private int mCapturedCount = 0;
    private int mTotalSize = 0;

    void capture(byte[] buffer, int offset, int size)
    {
        if (mCapturedCount == TOTAL_CAPTURE_COUNT)
        {
            return;
        }
        if (isSps(buffer, offset))
        {
            mCapturedNal[0] = new byte[size];
            System.arraycopy(buffer, offset, mCapturedNal[0], 0, size);
            AppLog.logd("SPS: %d", mCapturedNal[0].length);
            mTotalSize+=size;
            mCapturedCount++;
            return;
        }

        if (mCapturedNal[0] == null)
        {
            // NO SPS yet
            return;
        }

        mCapturedNal[mCapturedCount] = new byte[size];
        System.arraycopy(buffer, offset, mCapturedNal[mCapturedCount], 0, size);
        mTotalSize+=size;
        AppLog.logd("NAL #%d: %02x %d", mCapturedCount, getNalType(mCapturedNal[mCapturedCount], 0), mCapturedNal[mCapturedCount].length);
        mCapturedCount++;
    }

    boolean isReady()
    {
        return mCapturedCount == TOTAL_CAPTURE_COUNT;
    }

    ByteBuffer getByteBuffer()
    {
        ByteBuffer content = ByteBuffer.allocate(mTotalSize);
        for (int i = 0; i < mCapturedCount; i++)
        {
            content.put(mCapturedNal[i]);
        }
        content.limit(mTotalSize);
        content.position(0);
        return content;
    }

    public void reset()
    {
        mTotalSize = 0;
        mCapturedCount = 0;
        mCapturedNal = new byte[TOTAL_CAPTURE_COUNT][];
    }

    // For NAL units having nal_unit_type equal to 7 or 8 (indicating
    // a sequence parameter set or a picture parameter set,
    // respectively)
    private static boolean isSps(byte[] ba, int offset)
    {
        return getNalType(ba, offset) == 7;
    }

    // For coded slice NAL units of a primary
    // coded picture having nal_unit_type equal to 5 (indicating a
    // coded slice belonging to an IDR picture), an H.264 encoder
    // SHOULD set the value of NRI to 11 (in binary format).
    public static boolean isIdrSlice(byte[] ba)
    {
        return (ba[4] & 0x1f) == 5;
    }

    private static int getNalType(byte[] ba, int offset)
    {
        // nal_unit_type
        // ba[4] == 0x67
        // +---------------+
        // |0|1|1|0|0|1|1|1|
        // +-+-+-+-+-+-+-+-+
        // |F|NRI|  Type   |
        // +---------------+
        return (ba[offset + 4] & 0x1f);
    }
}
