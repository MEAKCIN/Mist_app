package com.example.app2.data

// Assuming EmotionSetting.kt is created in the same package
// import com.example.app2.data.EmotionSetting // Add this if not automatically resolved

data class DeviceStatus(
    val emotions: List<EmotionSetting>, // Replaces sprayPeriod, sprayDuration, currentEmotion
    val deviceOn: Boolean
)