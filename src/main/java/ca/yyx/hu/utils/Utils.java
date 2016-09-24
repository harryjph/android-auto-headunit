
// General utility functions

// Utils.log


package ca.yyx.hu.utils;

import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.IllegalFormatException;
import java.util.Locale;


public final class Utils {

    private static final String TAG = "Headunit";
    public static final boolean IS_LOLLIPOP = Build.VERSION.SDK_INT >= 21;

    public static void logd(String msg) {
        Log.d(TAG, format(msg));
    }

    public static void logd(final String msg, final Object... params) {
        Log.d(TAG, format(msg, params));
    }

    public static void logv(String msg) {
        Log.v(TAG, format(msg));
    }

    public static void loge(String msg) {
        Log.e(TAG, format(msg));
    }

    public static void logw(String msg) {
        Log.w(TAG, format(msg));
    }

    public static void loge(String msg, Throwable tr) {
        Log.e(TAG, format(msg), tr);
    }

    public static void loge(Throwable tr) {
        Log.e(TAG, tr.getMessage(), tr);
    }

    public static void loge(String msg, final Object... params) {
        Log.e(TAG, format(msg, params));
    }

    public static void logv(String msg, final Object... params) {
        Log.v(TAG, format(msg, params));
    }

    private static String format(final String msg, final Object... array) {
        String formatted;
        if (array == null || array.length == 0) {
            formatted = msg;
        } else {
            try {
                formatted = String.format(Locale.US, msg, array);
            } catch (IllegalFormatException ex) {
                loge("IllegalFormatException: formatString='%s' numArgs=%d", msg, array.length);
                formatted = msg + " (An error occurred while formatting the message.)";
            }
        }
        final StackTraceElement[] stackTrace = new Throwable().fillInStackTrace().getStackTrace();
        String string = "<unknown>";
        for (int i = 2; i < stackTrace.length; ++i) {
            final String className = stackTrace[i].getClassName();
            if (!className.equals(Utils.class.getName())) {
                final String substring = className.substring(1 + className.lastIndexOf(46));
                string = substring.substring(1 + substring.lastIndexOf(36)) + "." + stackTrace[i].getMethodName();
                break;
            }
        }
        return String.format(Locale.US, "[%d] %s: %s", Thread.currentThread().getId(), string, formatted);
    }

    public static long ms_sleep(long ms) {

        try {
            Thread.sleep(ms);                                                // Wait ms milliseconds
            return (ms);
        } catch (InterruptedException e) {
            //Thread.currentThread().interrupt();
            e.printStackTrace();
            Utils.loge("Exception e: " + e);
            return (0);
        }
    }

    public static long tmr_ms_get() {        // Current timestamp of the most precise timer available on the local system, in nanoseconds. Equivalent to Linux's CLOCK_MONOTONIC.
        // Values returned by this method do not have a defined correspondence to wall clock times; the zero value is typically whenever the device last booted
        //Utils.logd ("ms: " + ms);           // Changing system time will not affect results.
        return (System.nanoTime() / 1000000);
    }

    public static String hex_get(byte b) {
        byte c1 = (byte) ((b & 0x00F0) >> 4);
        byte c2 = (byte) ((b & 0x000F) >> 0);

        byte[] buffer = new byte[2];

        if (c1 < 10)
            buffer[0] = (byte) (c1 + '0');
        else
            buffer[0] = (byte) (c1 + 'A' - 10);
        if (c2 < 10)
            buffer[1] = (byte) (c2 + '0');
        else
            buffer[1] = (byte) (c2 + 'A' - 10);

        return new String(buffer);
    }

    public static String hex_get(short s) {
        byte byte_lo = (byte) (s >> 0 & 0xFF);
        byte byte_hi = (byte) (s >> 8 & 0xFF);
        return (hex_get(byte_hi) + hex_get(byte_lo));
    }

    public static int varint_encode(int val, byte[] ba, int idx) {
        if (val >= 1 << 14) {
            Utils.loge("Too big");
            return (1);
        }
        ba[idx + 0] = (byte) (0x7f & (val >> 0));
        ba[idx + 1] = (byte) (0x7f & (val >> 7));
        if (ba[idx + 1] != 0) {
            ba[idx + 0] |= 0x80;
            return (2);
        }
        return (1);
    }

    public static int varint_encode(long val, byte[] ba, int start) {
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

    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        int buffSize = 16384 * 1024; // 16M
        byte[] data = new byte[buffSize];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public static void put_time(int offset, byte[] arr, long time) {
        for (int ctr = 7; ctr >= 0; ctr--) {                           // Fill 8 bytes backwards
            arr[offset + ctr] = (byte) (time & 0xFF);
            time = time >> 8;
        }
    }
}

