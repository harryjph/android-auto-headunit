package ca.yyx.hu.app

import android.app.Activity
import android.os.Bundle

import ca.yyx.hu.R
import ca.yyx.hu.utils.SystemUI


abstract class SurfaceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)   // !! Keep Screen on !!
        setContentView(R.layout.activity_headunit)
        window.decorView.setOnSystemUiVisibilityChangeListener { SystemUI.hide(window.decorView) }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SystemUI.hide(window.decorView)
    }

}
