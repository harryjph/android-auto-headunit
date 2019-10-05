package info.anodsplace.headunit.utils

import android.util.Log

typealias MessageProducer = () -> String

object AppLog {
    const val LOG_LEVEL = Log.INFO

    const val TAG = "HeadUnit"

    inline fun d(messageProducer: MessageProducer) {
        if (LOG_LEVEL <= Log.DEBUG) {
            Log.d(TAG, messageProducer())
        }
    }

    inline fun i(messageProducer: MessageProducer) {
        if (LOG_LEVEL <= Log.INFO) {
            Log.i(TAG, messageProducer())
        }
    }

    inline fun w(messageProducer: MessageProducer) {
        if (LOG_LEVEL <= Log.WARN) {
            Log.w(TAG, messageProducer())
        }
    }

    inline fun e(messageProducer: MessageProducer) {
        if (LOG_LEVEL <= Log.ERROR) {
            Log.e(TAG, messageProducer())
        }
    }

    fun e(t: Throwable) {
        if (LOG_LEVEL <= Log.ERROR) {
            Log.e(TAG, "Error", t)
        }
    }
}

