package ca.yyx.hu.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.UpdateManager

import ca.yyx.hu.App
import ca.yyx.hu.R
import ca.yyx.hu.aap.AapProjectionActivity
import ca.yyx.hu.utils.AppLog
import ca.yyx.hu.utils.NetworkUtils
import ca.yyx.hu.utils.SystemUI
import ca.yyx.hu.utils.Utils
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var mVideoButton: View

    var keyListener: KeyListener? = null

    interface KeyListener
    {
        fun onKeyEvent(event: KeyEvent): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mVideoButton = findViewById(R.id.video_button)
        mVideoButton.setOnClickListener {
            val aapIntent = Intent(this@MainActivity, AapProjectionActivity::class.java)
            aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
            startActivity(aapIntent)
        }

        findViewById<ImageButton>(R.id.usb).setOnClickListener {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, UsbListFragment())
                    .commit()
        }

        findViewById<ImageButton>(R.id.settings).setOnClickListener {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, SettingsFragment())
                    .commit()
        }

        try {
            val currentIp = NetworkUtils.getWifiIpAddress(this)
            val inet = NetworkUtils.intToInetAddress(currentIp)
            val ipView = findViewById<TextView>(R.id.ip_address)
            ipView.text = inet?.hostAddress ?: ""
        } catch (ignored: IOException) { }

        UpdateManager.register(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SystemUI.hide(window.decorView)
    }

    override fun onResume() {
        super.onResume()
        mVideoButton.isEnabled = App.provide(this).transport.isAlive
        CrashManager.register(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("onKeyDown: %d", keyCode)

        return keyListener?.onKeyEvent(event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("onKeyUp: %d", keyCode)

        return keyListener?.onKeyEvent(event) ?: super.onKeyUp(keyCode, event)
    }
}
