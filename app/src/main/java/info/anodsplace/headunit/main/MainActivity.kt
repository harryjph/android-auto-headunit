package info.anodsplace.headunit.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView

import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.UpdateManager

import info.anodsplace.headunit.App
import info.anodsplace.headunit.R
import info.anodsplace.headunit.aap.AapProjectionActivity
import info.anodsplace.headunit.utils.AppLog
import info.anodsplace.headunit.utils.NetworkUtils
import info.anodsplace.headunit.utils.SystemUI
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : Activity() {

    var keyListener: KeyListener? = null

    interface KeyListener
    {
        fun onKeyEvent(event: KeyEvent): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        video_button.setOnClickListener {
            val aapIntent = Intent(this@MainActivity, AapProjectionActivity::class.java)
            aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
            startActivity(aapIntent)
        }

        usb.setOnClickListener {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, UsbListFragment())
                    .commit()
        }

        settings.setOnClickListener {
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
        video_button.isEnabled = App.provide(this).transport.isAlive
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
