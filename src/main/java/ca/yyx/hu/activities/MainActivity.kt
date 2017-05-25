package ca.yyx.hu.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View

import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.UpdateManager

import ca.yyx.hu.App
import ca.yyx.hu.R
import ca.yyx.hu.aap.AapProjectionActivity
import ca.yyx.hu.aap.AapService
import ca.yyx.hu.fragments.NetworkListFragment
import ca.yyx.hu.fragments.UsbListFragment
import ca.yyx.hu.utils.SystemUI

class MainActivity : Activity() {
    private lateinit var mVideoButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mVideoButton = findViewById(R.id.video_button)
        mVideoButton.setOnClickListener {
            val aapIntent = Intent(this@MainActivity, AapProjectionActivity::class.java)
            aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
            startActivity(aapIntent)
        }

        findViewById(R.id.usb).setOnClickListener {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, UsbListFragment())
                    .commit()
        }

        UpdateManager.register(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SystemUI.hide(window.decorView)
    }

    override fun onResume() {
        super.onResume()
        mVideoButton.isEnabled = App.get(this).transport().isAlive
        CrashManager.register(this)
    }

}
