package com.taochirho.testvm.ui.main

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import androidx.preference.Preference


class TCRNamePref(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs),
    Preference.OnPreferenceChangeListener {

    init {
        onPreferenceChangeListener = this
    }

    override fun getSummary(): String? {
        return text
    }



    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        summary = text
        return true
    }
}