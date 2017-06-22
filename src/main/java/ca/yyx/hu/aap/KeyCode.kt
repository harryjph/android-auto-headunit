package ca.yyx.hu.aap

import android.view.KeyEvent

import ca.yyx.hu.aap.protocol.messages.ScrollWheelEvent

object KeyCode {

    fun supported(): IntArray {
        return intArrayOf(
                KeyEvent.KEYCODE_SOFT_LEFT,
                KeyEvent.KEYCODE_SOFT_RIGHT,
                KeyEvent.KEYCODE_BACK,

                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,

                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,

                KeyEvent.KEYCODE_SEARCH,
                KeyEvent.KEYCODE_CALL,
                ScrollWheelEvent.KEYCODE_SCROLL_WHEEL)
    }


    internal fun convert(keyCode: Int): Int {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_SOFT_LEFT,
            KeyEvent.KEYCODE_SOFT_RIGHT
                -> return keyCode
            KeyEvent.KEYCODE_ENTER -> return KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_HEADSETHOOK -> return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_STOP -> return KeyEvent.KEYCODE_MEDIA_PAUSE
        }
        return KeyEvent.KEYCODE_UNKNOWN
    }

}
