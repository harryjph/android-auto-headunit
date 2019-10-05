package info.anodsplace.headunit.utils

import android.os.Build
import android.view.View

fun View.hideSystemUI() { // TODO this is called too often...
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        this.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE and View.SYSTEM_UI_FLAG_FULLSCREEN and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    } else {
        this.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}
