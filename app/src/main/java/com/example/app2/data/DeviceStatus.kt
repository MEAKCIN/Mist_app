package com.example.app2.data

data class DeviceStatus(
    val sprayPeriod: Float,
    val sprayDuration: Float,
    val deviceOn: Boolean,
    val currentEmotion: String
)