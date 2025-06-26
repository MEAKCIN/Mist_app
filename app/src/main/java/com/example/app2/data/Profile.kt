package com.example.app2.data

import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(), // Her profil için benzersiz ID
    var name: String,
    var emotionSettings: List<EmotionSetting>
)