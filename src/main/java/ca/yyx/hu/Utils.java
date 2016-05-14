
// General utility functions

// Utils.log


package ca.yyx.hu;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.IllegalFormatException;
import java.util.Locale;


public final class Utils {

    public static final String TAG = "Headunit";
    public static final int SDK_INT = Build.VERSION.SDK_INT;

    // Android Logging Levels:
    public static final boolean ena_log_verbo = false;
    private static final boolean ena_log_debug = true;//false;//true;
    private static final boolean ena_log_warni = true;//false;//true;
    private static final boolean ena_log_error = true;

    public static void logd(String msg) {
        if (ena_log_debug) Log.d(TAG, format(msg));
    }

    public static void logd(final String msg, final Object... params) {
        if (ena_log_debug) Log.d(TAG, format(msg, params));
    }

    public static void logv(String msg) {
        Log.v(TAG, format(msg));
    }

    public static void loge(String msg) {
        Log.e(TAG, format(msg));
    }

    public static void logw(String msg) {
        if (ena_log_warni) Log.w(TAG, format(msg));
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

    public static boolean main_thread_get(String source) {
        boolean ret = (Looper.myLooper() == Looper.getMainLooper());
        if (ret)
            Utils.logd("YES MAIN THREAD source: " + source);
        //else
        //  Utils.logd ("Not main thread source: " + source);
        return (ret);
    }

      public static long ms_sleep(long ms) {
        //Utils.logw ("ms: " + ms);                                          // Warning

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

    public static byte[] hexstr_to_ba(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        return (data);
    }

    public static String str_to_hexstr(String s) {
        byte[] ba = str_to_ba(s);
        return (ba_to_hexstr(ba));
    }

    public static String ba_to_hexstr(byte[] ba) {
        String hex = "";
        for (int ctr = 0; ctr < ba.length; ctr++) {
            hex += hex_get(ba[ctr]);    //hex += "" + hex_get ((byte) (ba [ctr] >> 4));
        }
        return (hex.toString());
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

    public static String hex_get(int i) {
        byte byte_0 = (byte) (i >> 0 & 0xFF);
        byte byte_1 = (byte) (i >> 8 & 0xFF);
        byte byte_2 = (byte) (i >> 16 & 0xFF);
        byte byte_3 = (byte) (i >> 24 & 0xFF);
        return (hex_get(byte_3) + hex_get(byte_2) + hex_get(byte_1) + hex_get(byte_0));
    }

    public static void hex_dump(String prefix, byte[] ba, int size) {
        int len = ba.length;
        String str = "";
        int idx = 0;
        for (idx = 0; idx < len && idx < size; idx++) {
            str += hex_get(ba[idx]) + " ";
            if (idx % 16 == 15) {
                Utils.logd(prefix + " " + hex_get((idx / 16) * 16) + ": " + str);
                str = "";
            }
        }
        if (!str.equals("")) {
            Utils.logd(prefix + " " + hex_get((idx / 16) * 16) + ": " + str);
        }
    }

    public static void hex_dump(String prefix, ByteBuffer content, int size) {
        Utils.hex_dump(prefix, content.array(), size);
    }

    private static int int_get(byte lo) {
        int ret = lo;
        if (ret < 0)
            ret += 256;
        return (ret);
    }

    public static int int_get(byte hi, byte lo) {
        int ret = int_get(lo);
        ret += 256 * int_get(hi);
        return (ret);
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

    public static int varint_encode(long val, byte[] ba, int idx) {
        if (val >= 0x7fffffffffffffffL) {
            Utils.loge("Too big");
            return (1);
        }
        long left = val;
        for (int idx2 = 0; idx2 < 9; idx2++) {
            ba[idx + idx2] = (byte) (0x7f & left);
            left = left >> 7;
            if (left == 0) {
                return (idx2 + 1);
            } else if (idx2 < 9 - 1) {
                ba[idx + idx2] |= 0x80;
            }
        }
        return (9);
    }

    public static int file_write(Context context, String filename, byte[] buf) {
        try {                                                               // File /data/data/ca.yyx.hu/hu.log contains a path separator
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
            // Create/open file for writing
            fos.write(buf);                                                  // Copy to file
            fos.close();                                                     // Close file
        } catch (Throwable t) {
            Log.e(TAG, "[hucomuti] " + t.getMessage(), t);
            t.printStackTrace();
            return (-1);
        }
        return (0);
    }

    public static byte[] str_to_ba(String s) {                          // String to byte array
        //s += "�";     // RDS test ?
        char[] buffer = s.toCharArray();
        byte[] content = new byte[buffer.length];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) buffer[i];
            //if (content [i] == -3) {            // ??
            //  loge ("s: " + s);//content [i]);
            //  content [i] = '~';
            //}
        }
        return (content);
    }


    // Strings:

    public static String str_MAN = "Android";//"Mike";                    // Manufacturer
    public static String str_MOD = "Android Auto";//"Android Open Automotive Protocol"  // Model
    public static String str_DES = "Head Unit";                           // Description
    public static String str_VER = "1.0";                                 // Version
    public static String str_URI = "http://www.android.com/";             // URI
    public static String str_SER = "0";//000000012345678";                // Serial #

    public static boolean su_installed_get() {
        boolean ret = false;
        if (Utils.file_get("/system/bin/su"))
            ret = true;
        else if (Utils.file_get("/system/xbin/su"))
            ret = true;
        Utils.logd("ret: " + ret);
        return (ret);
    }

    public static int sys_run(String cmd, boolean su) {
        //main_thread_get ("sys_run cmd: " + cmd);

        String[] cmds = {("")};
        cmds[0] = cmd;
        return (arr_sys_run(cmds, su));
    }

    private static int arr_sys_run(String[] cmds, boolean su) {         // !! Crash if any output to stderr !!
        //Utils.logd ("sys_run: " + cmds);

        try {
            Process p;
            if (su)
                p = Runtime.getRuntime().exec("su");
            else
                p = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String line : cmds) {
                Utils.logd("su: " + su + "  line: " + line);
                os.writeBytes(line + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();

            int exit_val = p.waitFor();                                      // This could hang forever ?
            if (exit_val != 0)
                Utils.logw("cmds [0]: " + cmds[0] + "  exit_val: " + exit_val);
            else
                Utils.logd("cmds [0]: " + cmds[0] + "  exit_val: " + exit_val);

            //os.flush ();
            return (exit_val);
        } catch (Exception e) {
            //e.printStackTrace ();
            Utils.loge("Exception e: " + e);
        }
        ;
        return (-1);
    }

    public static boolean file_get(String filename, boolean log) {
        //main_thread_get ("file_get filename: " + filename);
        File ppFile = null;
        boolean exists = false;
        try {
            ppFile = new File(filename);
            if (ppFile.exists())
                exists = true;
        } catch (Exception e) {
            //e.printStackTrace ();
            Utils.loge("Exception: " + e);
            exists = false;                                                   // Exception means no file or no permission for file
        }
        if (log)
            Utils.logd("exists: " + exists + "  \'" + filename + "\'");
        return (exists);
    }

    public static boolean quiet_file_get(String filename) {
        return (file_get(filename, false));
    }

    public static boolean file_get(String filename) {
        return (file_get(filename, true));
    }


   public static boolean file_delete(final String filename) {
        main_thread_get("file_delete filename: " + filename);
        java.io.File f = null;
        boolean ret = false;
        try {
            f = new File(filename);
            ret = f.delete();
        } catch (Throwable e) {
            Utils.loge(e);
        }
        Utils.logd("ret: " + ret);
        return (ret);
    }

    public static boolean file_create(final String filename) {
        main_thread_get("file_create filename: " + filename);
        java.io.File f = null;
        boolean ret = false;
        try {
            f = new File(filename);
            ret = f.createNewFile();
            Utils.logd("ret: " + ret);
        } catch (Throwable e) {
            Utils.logd("Throwable e: " + e);
            e.printStackTrace();
        }
        return (ret);
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

}

