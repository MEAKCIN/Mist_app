package com.example.app2.data

import java.util.UUID

enum class AlarmMode {
    SINGLE, // Just turns on at a specific time
    RANGE   // Turns on at a start time, off at an end time
}

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val profileId: String, // ID of the profile to activate
    val mode: AlarmMode,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int? = null,    // Only for RANGE mode
    val endMinute: Int? = null, // Only for RANGE mode
    var isEnabled: Boolean = true
)