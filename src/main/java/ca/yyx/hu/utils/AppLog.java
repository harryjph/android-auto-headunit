package ca.yyx.hu.utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.IllegalFormatException;
import java.util.Locale;

public final class AppLog {

    public interface Logger
    {
        void println(int priority, String tag, String msg);

        class Android implements Logger
        {
            @Override
            public void println(int priority, String tag, String msg) {
                Log.println(priority, TAG, msg);
            }
        }

        class StdOut implements Logger
        {
            @Override
            public void println(int priority, String tag, String msg) {
                System.out.println("["+tag+":"+priority+"] " + msg);
            }
        }
    }

    public static Logger LOGGER = new Logger.Android();
    private static final int LOG_LEVEL = Log.INFO;

    public static final String TAG = "CAR.HU.J";
    public static final boolean LOG_VERBOSE = LOG_LEVEL <= Log.VERBOSE;
    public static final boolean LOG_DEBUG = LOG_LEVEL <= Log.DEBUG;

    public static void i(String msg) {
        log(Log.INFO, format(msg));
    }

    public static void i(final String msg, final Object... params) {
        log(Log.INFO, format(msg, params));
    }

    public static void e(String msg) {
        loge(format(msg), null);
    }

    public static void e(String msg, Throwable tr) {
        loge(format(msg), tr);
    }

    public static void e(Throwable tr) {
        loge(tr.getMessage(), tr);
    }


    public static void e(String msg, final Object... params) {
        loge(format(msg, params), null);
    }

    public static void v(String msg, final Object... params) {
        log(Log.VERBOSE, format(msg, params));
    }

    public static void d(String msg, final Object... params) {
        log(Log.DEBUG, format(msg, params));
    }

    public static void d(String msg) {
        log(Log.DEBUG, format(msg));
    }

    private static void log(int priority, String msg)
    {
        if (priority >= LOG_LEVEL)
        {
            LOGGER.println(priority, TAG, msg);
        }
    }

    private static void loge(String message,@Nullable Throwable tr) {
        String trace = LOGGER instanceof Logger.Android ? Log.getStackTraceString(tr) : "";
        LOGGER.println(Log.ERROR, TAG, message + '\n' + trace);
    }


    private static String format(final String msg, final Object... array) {
        String formatted;
        if (array == null || array.length == 0) {
            formatted = msg;
        } else {
            try {
                formatted = String.format(Locale.US, msg, array);
            } catch (IllegalFormatException ex) {
                e("IllegalFormatException: formatString='%s' numArgs=%d", msg, array.length);
                formatted = msg + " (An error occurred while formatting the message.)";
            }
        }
        final StackTraceElement[] stackTrace = new Throwable().fillInStackTrace().getStackTrace();
        String string = "<unknown>";
        for (int i = 2; i < stackTrace.length; ++i) {
            final String className = stackTrace[i].getClassName();
            if (!className.equals(AppLog.class.getName())) {
                final String substring = className.substring(1 + className.lastIndexOf(46));
                string = substring.substring(1 + substring.lastIndexOf(36)) + "." + stackTrace[i].getMethodName();
                break;
            }
        }
        return String.format(Locale.US, "[%d] %s | %s", Thread.currentThread().getId(), string, formatted);
    }

    public static void i(Intent intent) {
        i(intent.toString());
        Bundle ex = intent.getExtras();
        if (ex != null) {
            i(ex.toString());
        }
    }
}

