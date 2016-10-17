package ca.yyx.hu.aap;


import android.support.v4.util.LruCache;

import ca.yyx.hu.utils.AppLog;

/**
 * @author algavris
 * @date 17/10/2016.
 */

class ByteArrayPool {

    private static final int CACHE_SIZE = 16;
    private LruCache<Integer, ByteArray> mCache = new LruCache<>(CACHE_SIZE);
    private Integer mNext = -1;

    public ByteArray obtain()
    {
        mNext++;
        if (mNext == CACHE_SIZE) {
            mNext = 0;
        }
        AppLog.logd("%d",mNext);
        ByteArray ba = mCache.get(mNext);
        if (ba == null) {
            ba = new ByteArray(Protocol.DEF_BUFFER_LENGTH);
            mCache.put(mNext, ba);
        } else {
            ba.reset();
        }
        return ba;
    }

    public ByteArray obtain(byte[] data, int length) {
        ByteArray ba = obtain();
        ba.put(data, length);
        return ba;
    }
}
