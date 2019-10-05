package info.anodsplace.headunit.app

import android.app.Activity
import android.os.Bundle

import info.anodsplace.headunit.R
import info.anodsplace.headunit.utils.hideSystemUI


abstract class SurfaceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_headunit)
        window.decorView.setOnSystemUiVisibilityChangeListener { window.decorView.hideSystemUI() }
        window.decorView.hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.hideSystemUI()
    }
}
