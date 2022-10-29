package com.taochirho.wordbox.ui.main

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.taochirho.wordbox.R

class TCRPrefsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}