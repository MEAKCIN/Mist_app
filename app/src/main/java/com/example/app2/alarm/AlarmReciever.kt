package com.example.app2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.app2.data.EmotionSetting
import com.example.app2.data.Profile
import com.example.app2.network.NetworkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.app2.ACTION_ALARM_TRIGGER"
        const val EXTRA_DEVICE_ON = "EXTRA_DEVICE_ON"
        // RENAME this constant for clarity
        const val EXTRA_PROFILE_ID_TO_ACTIVATE = "EXTRA_PROFILE_ID_TO_ACTIVATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ALARM_TRIGGER) {
            val pendingResult = goAsync()
            val deviceOn = intent.getBooleanExtra(EXTRA_DEVICE_ON, false)
            // UPDATE this line to use the new constant
            val profileId = intent.getStringExtra(EXTRA_PROFILE_ID_TO_ACTIVATE)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // This logic now correctly uses the profile from the specific alarm
                    val settings = getEmotionSettingsForProfile(context, profileId)
                    NetworkManager.updateDeviceRequest(
                        deviceOn = deviceOn,
                        emotionSettings = settings,
                        onResponse = { _, _ -> /* You can log success/failure here if needed */ }
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun getEmotionSettingsForProfile(context: Context, profileId: String?): List<EmotionSetting> {
        if (profileId == null) {
            return defaultDeviceEmotionSettings()
        }
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("user_profiles", null)
        val profiles: List<Profile> = if (json != null) {
            try {
                val type = object : TypeToken<List<Profile>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return profiles.find { it.id == profileId }?.emotionSettings ?: defaultDeviceEmotionSettings()
    }

    // A default fallback for emotion settings
    private fun defaultDeviceEmotionSettings(): List<EmotionSetting> {
        return listOf(
            EmotionSetting("Neutral", 1f, 1f, false),
            EmotionSetting("Happy", 10f, 5f, true),
            EmotionSetting("Angry", 5f, 2f, false),
            EmotionSetting("Sad", 20f, 10f, false)
        )
    }
}