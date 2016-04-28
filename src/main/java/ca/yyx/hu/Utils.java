
// General utility functions

// Utils.log


package ca.yyx.hu;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;


public final class Utils {

    public static final String TAG = "Headunit";
    // Stats:
    private static int m_obinits = 0;

    public static final int android_version = android.os.Build.VERSION.SDK_INT;

    public Utils() {
        final String tag = tag_prefix_get() + "comuti";
        m_obinits++;
        Log.d(TAG, "[" + tag + "] m_obinits: " + m_obinits);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread aThread, Throwable aThrowable) {
                //Utils.loge ("!!!!!!!! Uncaught exception: " + aThrowable);
                Log.e(TAG, "!!!!!!!! Uncaught exception: " + aThrowable.getMessage(), aThrowable);
            }
        });

        Log.e(TAG, "[" + tag + "] done");
    }

    // Android Logging Levels:
    public static final boolean ena_log_verbo = false;
    private static final boolean ena_log_debug = true;//false;//true;
    private static final boolean ena_log_warni = true;//false;//true;
    private static final boolean ena_log_error = true;

    private static String tag_prefix = "";
    private static final int max_log_char = 7;//8;

    private static String tag_prefix_get() {
        try {
            if (tag_prefix != null && !tag_prefix.equals(""))
                return (tag_prefix);
            String pkg = "ca.yyx.hu";
            tag_prefix = pkg.substring(7);
            if (tag_prefix.equals(""))
                tag_prefix = "s!";
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage(), e);
            tag_prefix = "E!";
        }
        return (tag_prefix);
    }

    private static void log(int level, String text) {

        final StackTraceElement stack_trace_el = new Exception().getStackTrace()[2];
        String method = stack_trace_el.getMethodName();
        String full_txt = String.format("[%36.36s] %s", method, text);

        if (level == Log.ERROR)
            Log.e(TAG, full_txt);
        else if (level == Log.WARN)
            Log.w(TAG, full_txt);
        else if (level == Log.DEBUG)
            Log.d(TAG, full_txt);
        else if (level == Log.VERBOSE)
            Log.v(TAG, full_txt);
    }

    public static void logv(String text) {
        if (ena_log_verbo)
            log(Log.VERBOSE, text);
    }

    public static void logd(String text) {
        if (ena_log_debug)
            log(Log.DEBUG, text);
    }

    public static void logw(String text) {
        if (ena_log_warni)
            log(Log.WARN, text);
    }

    public static void loge(String text) {
        if (ena_log_error)
            log(Log.ERROR, text);
    }


    public static String app_version_get(Context act) {                                             // Get versionName (from AndroidManifest.xml)
        String version = "";
        PackageInfo package_info;
        try {
            package_info = act.getPackageManager().getPackageInfo(act.getPackageName(), 0);
            version = package_info.versionName;
        } catch (Exception e) {//NameNotFoundException e) {
            //e.printStackTrace ();
        }
        return (version);
    }


    public static boolean main_thread_get(String source) {
        boolean ret = (Looper.myLooper() == Looper.getMainLooper());
        if (ret)
            Utils.logd("YES MAIN THREAD source: " + source);
        //else
        //  Utils.logd ("Not main thread source: " + source);
        return (ret);
    }

    //public static boolean strict_mode = false;
    //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    //StrictMode.setThreadPolicy(policy);

    public static void strict_mode_set(boolean strict_mode) {
        if (!strict_mode) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .permitAll()
                    .build());

            return;
        }

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectAll()                                                     // For all detectable problems
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                //.penaltyDeath ()
                //.penaltyDialog ()   ????
                .build());
    }

    public static long ms_sleep(long ms) {
        //main_thread_get ("ms_sleep ms: " + ms);

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
        long ms = System.nanoTime() / 1000000; // Should only be used to measure a duration by comparing it against another timestamp on the same device.
        // Values returned by this method do not have a defined correspondence to wall clock times; the zero value is typically whenever the device last booted
        //Utils.logd ("ms: " + ms);           // Changing system time will not affect results.
        return (ms);
    }

    public static long utc_ms_get() {        // Current time in milliseconds since January 1, 1970 00:00:00.0 UTC.
        long ms = System.currentTimeMillis();  // Always returns UTC times, regardless of the system's time zone. This is often called "Unix time" or "epoch time".
        //Utils.logd ("ms: " + ms);           // This method shouldn't be used for measuring timeouts or other elapsed time measurements, as changing the system time can affect the results.
        return (ms);
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
        for (int ctr = 0; ctr < ba.length; ctr++)
            hex += hex_get(ba[ctr]);    //hex += "" + hex_get ((byte) (ba [ctr] >> 4));
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

        String str = new String(buffer);

        return (str);
    }

    public static String hex_get(short s) {
        byte byte_lo = (byte) (s >> 0 & 0xFF);
        byte byte_hi = (byte) (s >> 8 & 0xFF);
        String res = hex_get(byte_hi) + hex_get(byte_lo);
        return (res);
    }

    public static String hex_get(int i) {
        byte byte_0 = (byte) (i >> 0 & 0xFF);
        byte byte_1 = (byte) (i >> 8 & 0xFF);
        byte byte_2 = (byte) (i >> 16 & 0xFF);
        byte byte_3 = (byte) (i >> 24 & 0xFF);
        String res = hex_get(byte_3) + hex_get(byte_2) + hex_get(byte_1) + hex_get(byte_0);
        return (res);
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

    /*
      private static byte [] str_to_ba (String s) {                          // String to byte array
        //s += "�";     // RDS test ?
        char [] buffer = s.toCharArray ();
        byte [] content = new byte [buffer.length];
        for (int i = 0; i < content.length; i ++) {
          content [i] = (byte) buffer [i];
          //if (content [i] == -3) {            // ??
          //  Utils.loge ("s: " + s);//content [i]);
          //  content [i] = '~';
          //}
        }
        return (content);
      }
    */
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

/*
  private long varint_decode (byte [] ba, int len) {
    int idx = 0;
    long val = 0, new7 = 0;
    while (idx < len) {
      new7 =
      val |= (0x7f & ba [idx]);
      val = val
    }
  }

  private int varint_encode (long val, byte [] ba, int idx) {
    long left = val;
    while (left > 0) {
      if (left < 127)
    }
  }
*/

    public static int file_write(Context context, String filename, byte[] buf) {
        try {                                                               // File /data/data/ca.yyx.hu/hu.log contains a path separator
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
            // Create/open file for writing
            fos.write(buf);                                                  // Copy to file
            fos.close();                                                     // Close file
        } catch (Throwable t) {
            //Utils.loge ("Throwable t: " + t);
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

    public static String str_usb_perm = "ca.yyx.hu.ACTION_USB_DEVICE_PERMISSION";

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


    public static long file_size_get(String filename) {
        main_thread_get("file_size_get filename: " + filename);
        File ppFile = null;
        long ret = -1;
        try {
            ppFile = new File(filename);
            if (ppFile.exists())
                ret = ppFile.length();
        } catch (Exception e) {
            //e.printStackTrace ();
        }
        logd("ret: " + ret + "  \'" + filename + "\'");
        return (ret);
    }

    public static boolean file_delete(final String filename) {
        main_thread_get("file_delete filename: " + filename);
        java.io.File f = null;
        boolean ret = false;
        try {
            f = new File(filename);
            f.delete();
            ret = true;
        } catch (Throwable e) {
            Utils.logd("Throwable e: " + e);
            e.printStackTrace();
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

    public static String res_file_create(Context context, int id, String filename) {
        //main_thread_get ("res_file_create filename: " + filename);

        if (context == null)
            return ("");

        String full_filename = context.getFilesDir() + "/" + filename;
        try {
            InputStream ins = context.getResources().openRawResource(id);          // Open raw resource file as input
            int size = ins.available();                                      // Get input file size (actually bytes that can be read without indefinite wait)

            if (size > 0 && file_size_get(full_filename) == size) {          // If file already exists and size is unchanged... (assumes size will change on update !!)
                Utils.logd("file exists size unchanged");                            // !! Have to deal with updates !! Could check filesize, assuming filesize always changes.
                // Could use indicator file w/ version in file name... SSD running problem for update ??
                // Hypothetically, permissions may not be set for ssd due to sh failure

//!! Disable to re-write all non-EXE w/ same permissions and all EXE w/ permissions 755 !!!!        return (full_filename);                                         // Done

            }

            byte[] buffer = new byte[size];                                 // Allocate a buffer
            ins.read(buffer);                                                // Read entire file into buffer. (Largest file is s.wav = 480,044 bytes)
            ins.close();                                                     // Close input file

            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
            // Create/open output file for writing
            fos.write(buffer);                                               // Copy input to output file
            fos.close();                                                     // Close output file

            //Utils.sys_run ("chmod 755 " + full_filename + " 1>/dev/null 2>/dev/null" , false);              // Set execute permission; otherwise rw-rw----
            //perms_all (full_filename);

        } catch (Exception e) {
            //e.printStackTrace ();
            Utils.loge("Exception e: " + e);
            return (null);
        }

        return (full_filename);
    }

    public static byte[] file_read_16m(String filename) {               // Read file up to 16 megabytes into byte array
        //main_thread_get ("file_read_16m filename: " + filename);
        byte[] content = new byte[0];
        int bufSize = 16384 * 1024;
        byte[] content1 = new byte[bufSize];
        try {
            FileInputStream in = new FileInputStream(filename);
            int n = in.read(content1, 0, bufSize);
            in.close();
            content = new byte[n];
            for (int ctr = 0; ctr < n; ctr++)
                content[ctr] = content1[ctr];
        } catch (Exception e) {
            Utils.logd("Exception: " + e);
            //e.printStackTrace ();
        }
        return (content);
    }



}

