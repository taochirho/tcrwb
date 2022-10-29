package com.taochirho.wordbox.ui.main

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.SeekBarPreference

class TCRTimePref(context: Context, attrs: AttributeSet) : SeekBarPreference(context, attrs), Preference.OnPreferenceChangeListener {


    init { onPreferenceChangeListener = this }

    override fun onAttached() {
        super.onAttached()
        summary = "Currently $value minutes"
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        summary = "Set to $newValue minutes"
        return true
    }


}