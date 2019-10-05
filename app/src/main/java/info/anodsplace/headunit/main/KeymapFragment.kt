package info.anodsplace.headunit.main

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import info.anodsplace.headunit.R
import info.anodsplace.headunit.utils.IntentFilters
import info.anodsplace.headunit.utils.Settings
import info.anodsplace.headunit.contract.KeyIntent


/**
 * @author algavris
 * @date 13/06/2017
 */
class KeymapFragment : Fragment(), MainActivity.KeyListener, View.OnClickListener {

    private val idToCode = mapOf(
        R.id.keycode_soft_left to KeyEvent.KEYCODE_SOFT_LEFT,
        R.id.keycode_soft_right to KeyEvent.KEYCODE_SOFT_RIGHT,

        R.id.keycode_dpad_up to KeyEvent.KEYCODE_DPAD_UP,
        R.id.keycode_dpad_down to KeyEvent.KEYCODE_DPAD_DOWN,
        R.id.keycode_dpad_left to KeyEvent.KEYCODE_DPAD_LEFT,
        R.id.keycode_dpad_right to KeyEvent.KEYCODE_DPAD_RIGHT,
        R.id.keycode_dpad_center to KeyEvent.KEYCODE_DPAD_CENTER,

        R.id.keycode_media_play to KeyEvent.KEYCODE_MEDIA_PLAY,
        R.id.keycode_media_pause to KeyEvent.KEYCODE_MEDIA_PAUSE,
        R.id.keycode_media_play_pause to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        R.id.keycode_media_next to KeyEvent.KEYCODE_MEDIA_NEXT,
        R.id.keycode_media_previous to KeyEvent.KEYCODE_MEDIA_PREVIOUS,

        R.id.keycode_search to KeyEvent.KEYCODE_SEARCH,
        R.id.keycode_call to KeyEvent.KEYCODE_CALL,
        R.id.keycode_music to KeyEvent.KEYCODE_MUSIC,
        R.id.keycode_nav to KeyEvent.KEYCODE_GUIDE,
        R.id.keycode_night to KeyEvent.KEYCODE_N)

    private val codeToId = mapOf(
        KeyEvent.KEYCODE_SOFT_LEFT to R.id.keycode_soft_left,
        KeyEvent.KEYCODE_SOFT_RIGHT to R.id.keycode_soft_right,

        KeyEvent.KEYCODE_DPAD_UP to R.id.keycode_dpad_up,
        KeyEvent.KEYCODE_DPAD_DOWN to R.id.keycode_dpad_down,
        KeyEvent.KEYCODE_DPAD_LEFT to R.id.keycode_dpad_left,
        KeyEvent.KEYCODE_DPAD_RIGHT to R.id.keycode_dpad_right,
        KeyEvent.KEYCODE_DPAD_CENTER to R.id.keycode_dpad_center,

        KeyEvent.KEYCODE_MEDIA_PLAY to R.id.keycode_media_play,
        KeyEvent.KEYCODE_MEDIA_PAUSE to R.id.keycode_media_pause,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to R.id.keycode_media_play_pause,
        KeyEvent.KEYCODE_MEDIA_NEXT to R.id.keycode_media_next,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS to R.id.keycode_media_previous,

        KeyEvent.KEYCODE_SEARCH to R.id.keycode_search,
        KeyEvent.KEYCODE_CALL to R.id.keycode_call,
        KeyEvent.KEYCODE_MUSIC to R.id.keycode_music,
        KeyEvent.KEYCODE_GUIDE to R.id.keycode_nav,
        KeyEvent.KEYCODE_N to R.id.keycode_night)

    private var assignCode = KeyEvent.KEYCODE_UNKNOWN
    private lateinit var settings: Settings
    private var codesMap = mutableMapOf<Int, Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View {
        val view = inflater.inflate(R.layout.fragment_keymap, container, false)

        settings = Settings(context!!)
        codesMap = settings.keyCodes

        idToCode.forEach {
            (resId, keyCode) ->
            val button = view.findViewById<Button>(resId)
            button.tag = keyCode
            button.setOnClickListener(this)
        }

        view.findViewById<Button>(R.id.reset_codes).setOnClickListener {
            codesMap = mutableMapOf()
            settings.keyCodes = codesMap
        }

        return view
    }

    private val keyCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.getParcelableExtra<KeyEvent>(KeyIntent.extraEvent)
            onKeyEvent(event)
        }
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(keyCodeReceiver, IntentFilters.keyEvent)
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(keyCodeReceiver)
    }

    override fun onClick(v: View?) {
        val button = v as? Button ?: return
        val keyCode = button.tag as Int
        this.assignCode = keyCode
        val name = KeyEvent.keyCodeToString(this.assignCode)
        Toast.makeText(activity, "Press a key to assign to '$name'", Toast.LENGTH_SHORT).show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (activity as? MainActivity)?.let {
            it.setDefaultKeyMode(Activity.DEFAULT_KEYS_DISABLE)
            it.keyListener = this
        }
    }

    override fun onDetach() {
        (activity as? MainActivity)?.let {
            it.setDefaultKeyMode(Activity.DEFAULT_KEYS_SHORTCUT)
            it.keyListener = null
        }
        super.onDetach()
    }


    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            return true
        }
        if (this.assignCode != KeyEvent.KEYCODE_UNKNOWN) {
            // clear previous values
            (codesMap.entries.find {
                it.value == event.keyCode
            })?.let {
                codesMap.remove(it.key)
            }
            codesMap.put(this.assignCode, event.keyCode)
            settings.keyCodes = codesMap

            val name = KeyEvent.keyCodeToString(this.assignCode)
            Toast.makeText(activity, "'$name' is ${this.assignCode}", Toast.LENGTH_SHORT).show()
            this.assignCode = KeyEvent.KEYCODE_UNKNOWN
        }

        buttonForKeyCode(event.keyCode)?.requestFocus()
        return true
    }

    private fun buttonForKeyCode(keyCode: Int): Button? {
        val mappedCode = (codesMap.entries.find {
            it.value == keyCode
        })?.key ?: keyCode
        val resId = codeToId[mappedCode] ?: return null
        return view?.findViewById(resId)
    }
}