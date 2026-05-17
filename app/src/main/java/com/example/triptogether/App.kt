package com.example.triptogether

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.ImageLoader
import com.example.triptogether.utilities.SignalManager
import com.example.triptogether.utilities.StorageManager

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        applyNightMode()

        ImageLoader.init(this)
        SignalManager.init(this)
        StorageManager.init(this)
        FirebaseManager.init()
    }

    private fun applyNightMode() {
        val prefs = getSharedPreferences(Constants.Prefs.PREFS_NAME, MODE_PRIVATE)
        val isDarkMode: Boolean = prefs.getBoolean(Constants.Prefs.KEY_DARK_MODE, false)

        val mode: Int = if (isDarkMode)
            AppCompatDelegate.MODE_NIGHT_YES
        else
            AppCompatDelegate.MODE_NIGHT_NO

        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
