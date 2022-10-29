package com.taochirho.wordbox.ui.main

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.SeekBarPreference

class TCRTilesPref(context: Context, attrs: AttributeSet) : SeekBarPreference(context, attrs), Preference.OnPreferenceChangeListener {


    init { onPreferenceChangeListener = this }

    override fun onAttached() {
        super.onAttached()
        summary = "Currently $value tiles"
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (newValue) {
            27, 28 -> {
                summary = "$newValue tiles - relatively easy"
            }
            29 -> {
                summary = "$newValue tiles - more challenging"
            }
            30 -> {
                summary = "$newValue tiles - the default: difficult but usually doable"
            }
            31 -> {
                summary = "$newValue tiles - one extra tile definitely harder"
            }
            32, 33 -> {
                summary = "$newValue tiles - there may be no solution!"
            }
        }


        return true
    }


}