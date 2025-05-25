package com.example.app2

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.app2.ui.MainScreen
import com.example.app2.ui.theme.App2Theme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentLang = sharedPreferences.getString("language", "en") ?: "en"
        setLocale(currentLang) // Set initial locale

        setContent {
            var darkTheme = remember { mutableStateOf(false) }
            // Recompose will trigger with the new context after locale change
            App2Theme(darkTheme = darkTheme.value) {
                MainScreen(
                    darkTheme = darkTheme.value,
                    onDarkThemeChange = { darkTheme.value = it },
                    currentLanguage = currentLang,
                    onLanguageChange = { newLang ->
                        setLocale(newLang)
                        sharedPreferences.edit().putString("language", newLang).apply()
                        recreate() // Recreate activity to apply language change
                    }
                )
            }
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // It's good practice to wrap the base context for locale changes,
    // especially for older Android versions, though modern Compose handles
    // recomposition well. For robustness:
    override fun attachBaseContext(newBase: Context) {
        sharedPreferences = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = sharedPreferences.getString("language", "en") ?: "en"
        val locale = Locale(lang)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}