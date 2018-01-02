package info.anodsplace.headunit.utils

import android.content.Intent
import android.util.Log

import java.util.IllegalFormatException
import java.util.Locale

object AppLog {

    interface Logger {
        fun println(priority: Int, tag: String, msg: String)

        class Android : Logger {
            override fun println(priority: Int, tag: String, msg: String) {
                Log.println(priority, TAG, msg)
            }
        }

        class StdOut : Logger {
            override fun println(priority: Int, tag: String, msg: String) {
                println("[$tag:$priority] $msg")
            }
        }
    }

    var LOGGER: Logger = Logger.Android()
    private val LOG_LEVEL = Log.INFO

    val TAG = "CAR.HU.J"
    val LOG_VERBOSE = LOG_LEVEL <= Log.VERBOSE
    val LOG_DEBUG = LOG_LEVEL <= Log.DEBUG

    fun i(msg: String) {
        log(Log.INFO, format(msg))
    }

    fun i(msg: String, vararg params: Any) {
        log(Log.INFO, format(msg, *params))
    }

    fun e(msg: String?) {
        loge(format(msg ?: "Unknown error"), null)
    }

    fun e(msg: String, tr: Throwable) {
        loge(format(msg), tr)
    }

    fun e(tr: Throwable) {
        loge(tr.message ?: "Unknown error", tr)
    }


    fun e(msg: String?, vararg params: Any) {
        loge(format(msg ?: "Unknown error", *params), null)
    }

    fun v(msg: String, vararg params: Any) {
        log(Log.VERBOSE, format(msg, *params))
    }

    fun d(msg: String, vararg params: Any) {
        log(Log.DEBUG, format(msg, *params))
    }

    fun d(msg: String) {
        log(Log.DEBUG, format(msg))
    }

    private fun log(priority: Int, msg: String) {
        if (priority >= LOG_LEVEL) {
            LOGGER.println(priority, TAG, msg)
        }
    }

    private fun loge(message: String, tr: Throwable?) {
        val trace = if (LOGGER is Logger.Android) Log.getStackTraceString(tr) else ""
        LOGGER.println(Log.ERROR, TAG, message + '\n' + trace)
    }


    private fun format(msg: String, vararg array: Any): String {
        var formatted: String
        if (array.isEmpty()) {
            formatted = msg
        } else {
            try {
                formatted = String.format(Locale.US, msg, *array)
            } catch (ex: IllegalFormatException) {
                e("IllegalFormatException: formatString='%s' numArgs=%d", msg, array.size)
                formatted = msg + " (An error occurred while formatting the message.)"
            }

        }
        val stackTrace = Throwable().fillInStackTrace().stackTrace
        var string = "<unknown>"
        for (i in 2..stackTrace.size - 1) {
            val className = stackTrace[i].className
            if (className != AppLog::class.java.name) {
                val substring = className.substring(1 + className.indexOfLast { a -> a == 46.toChar() })
                string = substring.substring(1 + substring.indexOfLast { a -> a == 36.toChar() }) + "." + stackTrace[i].methodName
                break
            }
        }
        return String.format(Locale.US, "[%d] %s | %s", Thread.currentThread().id, string, formatted)
    }

    fun i(intent: Intent) {
        i(intent.toString())
        val ex = intent.extras
        if (ex != null) {
            i(ex.toString())
        }
    }
}

