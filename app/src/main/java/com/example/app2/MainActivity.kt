package com.example.app2

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext // Required for initial dark theme check if done differently
import com.example.app2.ui.MainScreen // Ensure this import is correct
import com.example.app2.ui.theme.App2Theme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences // For language preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize SharedPreferences for language.
        // Using "app_prefs" which is consistent with what MainScreen might use for profiles.
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val currentLang = loadLanguagePreference()
        setLocale(currentLang) // Apply the loaded or default language

        setContent {
            // Basic dark theme state management. Could also be loaded from SharedPreferences if persisted.
            var darkTheme = remember { mutableStateOf(isSystemInDarkThemeInitially()) }

            App2Theme(darkTheme = darkTheme.value) {
                MainScreen(
                    darkTheme = darkTheme.value,
                    onDarkThemeChange = { newDarkThemeState ->
                        darkTheme.value = newDarkThemeState
                        // TODO: Optionally, save dark theme preference to SharedPreferences here
                    },
                    currentLanguage = currentLang, // Pass the initially determined language
                    onLanguageChange = { newLangCode ->
                        // This callback is invoked from SettingsScreen via MainScreen
                        setLocale(newLangCode) // Apply the new locale
                        saveLanguagePreference(newLangCode) // Save the new preference
                        recreate() // Recreate the activity to apply changes everywhere
                    }
                )
            }
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale) // Set default locale for the JVM

        val resources = baseContext.resources // Get resources from baseContext
        val config = Configuration(resources.configuration) // Get current configuration
        config.setLocale(locale) // Set new locale in configuration


        baseContext.resources.updateConfiguration(config, resources.displayMetrics)
        applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)
        this.resources.updateConfiguration(config, this.resources.displayMetrics)
    }

    private fun saveLanguagePreference(languageCode: String) {
        sharedPreferences.edit().putString("language", languageCode).apply()
    }

    private fun loadLanguagePreference(): String {
        // Defaults to "en" (English) if no preference is found
        return sharedPreferences.getString("language", "en") ?: "en"
    }

    private fun isSystemInDarkThemeInitially(): Boolean {
        // Checks the system's current UI mode for dark theme
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    override fun attachBaseContext(newBase: Context) {

        val prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en" // Default to English

        val locale = Locale(lang)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}
