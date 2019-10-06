package info.anodsplace.headunit.utils

import android.os.Build
import android.view.View

fun View.hideSystemUI() { // TODO this is called too often...
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        this.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    } else {
        this.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}

fun bytesToHex(bytes: ByteArray): String {
    val hexChars = StringBuilder()
    repeat(bytes.size) {
        val formatted = String.format("%02X", bytes[it])
        if (formatted.length == 1) {
            hexChars.append('0')
        }
        hexChars.append(formatted)
    }
    return String(hexChars)
}
