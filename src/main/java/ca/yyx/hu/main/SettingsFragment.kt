package ca.yyx.hu.main

import android.app.AlertDialog
import android.content.DialogInterface
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import ca.yyx.hu.R
import ca.yyx.hu.decoder.MicRecorder
import ca.yyx.hu.utils.Settings

/**
 * @author algavris
 * @date 13/06/2017
 */
class SettingsFragment : ca.yyx.hu.app.BaseFragment() {
    lateinit var settings: Settings
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup, savedInstanceState: android.os.Bundle?): android.view.View {
        val view = inflater.inflate(ca.yyx.hu.R.layout.fragment_settings, container, false)

        view.findViewById<Button>(R.id.keymap).setOnClickListener {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, KeymapFragment())
                    .commit()
        }

        settings = Settings(activity)

        val sampleRateButton = view.findViewById<Button>(R.id.mic_sample_rate)
        val sampleRate = settings.micSampleRate
        sampleRateButton.text = getString(R.string.mic_sample_rate, sampleRate/1000)
        sampleRateButton.tag = sampleRate
        sampleRateButton.setOnClickListener {
            val newValue = Settings.MicSampleRates[it.tag]!!

            val recorder: MicRecorder? = try { MicRecorder(newValue) } catch (e: Exception) { null }

            if (recorder == null) {
                Toast.makeText(activity, "Value not supported: " + newValue, Toast.LENGTH_LONG).show()
            } else {
                settings.micSampleRate = newValue
                (it as Button).text = getString(R.string.mic_sample_rate, newValue / 1000)
                it.tag = newValue
            }
        }


        val nightModeButton = view.findViewById<Button>(R.id.night_mode)
        val nightMode = settings.nightMode
        val nightModeTitles = resources.getStringArray(R.array.night_mode)
        nightModeButton.text = getString(R.string.night_mode, nightModeTitles[nightMode.value])
        nightModeButton.tag = nightMode.value
        nightModeButton.setOnClickListener {
            val newValue = Settings.NightModes[it.tag]!!
            val newMode = Settings.NightMode.fromInt(newValue)!!
            (it as Button).text = getString(R.string.night_mode, nightModeTitles[newMode.value])
            it.tag = newValue
            settings.nightMode = newMode
        }

        val btAddressButton = view.findViewById<Button>(R.id.bt_address)
        btAddressButton.text = getString(R.string.bluetooth_address_s, settings.bluetoothAddress)
        btAddressButton.setOnClickListener {
            val editView = EditText(activity)
            editView.setText(settings.bluetoothAddress)
            AlertDialog.Builder(activity)
                .setTitle(R.string.enter_bluetooth_mac)
                .setView(editView)
                .setPositiveButton(android.R.string.ok, { dialog, _ ->
                    settings.bluetoothAddress = editView.text.toString().trim()
                    dialog.dismiss()
                }).show()
        }
        return view
    }
}