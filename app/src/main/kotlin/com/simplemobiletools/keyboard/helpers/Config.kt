package com.simplemobiletools.keyboard.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.keyboard.helpers.MyKeyboard.Companion.DEFAULT_KEYBOARD
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var vibrateOnKeypress: Boolean
        get() = prefs.getBoolean(VIBRATE_ON_KEYPRESS, true)
        set(vibrateOnKeypress) = prefs.edit().putBoolean(VIBRATE_ON_KEYPRESS, vibrateOnKeypress).apply()

    var showPopupOnKeypress: Boolean
        get() = prefs.getBoolean(SHOW_POPUP_ON_KEYPRESS, true)
        set(showPopupOnKeypress) = prefs.edit().putBoolean(SHOW_POPUP_ON_KEYPRESS, showPopupOnKeypress).apply()

    var lastExportedClipsFolder: String
        get() = prefs.getString(LAST_EXPORTED_CLIPS_FOLDER, "")!!
        set(lastExportedClipsFolder) = prefs.edit().putString(LAST_EXPORTED_CLIPS_FOLDER, lastExportedClipsFolder).apply()

    var keyboardLanguage: Int
        get() = prefs.getInt(KEYBOARD_LANGUAGE, getDefaultLanguage())
        set(keyboardLanguage) = prefs.edit().putInt(KEYBOARD_LANGUAGE, keyboardLanguage).apply()

    var keyboardHeightMultiplier: Int
        get() = prefs.getInt(HEIGHT_MULTIPLIER, 1)
        set(keyboardHeightMultiplier) = prefs.edit().putInt(HEIGHT_MULTIPLIER, keyboardHeightMultiplier).apply()

    var keyboardLayouts: ArrayList<String>
        get() = ArrayList(prefs.getStringSet(KEYBOARD_LAYOUTS, setOf(DEFAULT_KEYBOARD)))
        set(keyboardLayouts) = prefs.edit().putStringSet(KEYBOARD_LAYOUTS, keyboardLayouts.toSet()).apply()


    private fun getDefaultLanguage(): Int {
        val conf = context.resources.configuration
        return if (conf.locale.toString().toLowerCase(Locale.getDefault()).startsWith("ru_")) {
            LANGUAGE_RUSSIAN
        } else {
            LANGUAGE_ENGLISH_QWERTY
        }
    }
}
