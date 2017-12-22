package ca.anodsplace.headunit.aap

import android.view.KeyEvent

import ca.anodsplace.headunit.aap.protocol.messages.ScrollWheelEvent

object KeyCode {

    val supported = intArrayOf(
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
        KeyEvent.KEYCODE_MUSIC,

        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,

        KeyEvent.KEYCODE_TAB,
        KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_ENTER,

        ScrollWheelEvent.KEYCODE_SCROLL_WHEEL)

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
            KeyEvent.KEYCODE_SOFT_RIGHT,
            KeyEvent.KEYCODE_MUSIC,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN
                -> return keyCode
            KeyEvent.KEYCODE_HEADSETHOOK -> return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_STOP -> return KeyEvent.KEYCODE_MEDIA_PAUSE
        }
        return KeyEvent.KEYCODE_UNKNOWN
    }

}
