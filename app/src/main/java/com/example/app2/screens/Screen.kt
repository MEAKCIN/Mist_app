package com.example.app2.screens

sealed class Screen(val route: String, var title: String) {
    object Home : Screen("home", "Home")
    object Manual : Screen("manual", "Manual Control")
    object Image : Screen("image", "Image Control")
    object Settings : Screen("settings", "Settings")
    object ProfileManagement : Screen("profile_management", "Profile Management") // Yeni Ekran
    object Alarm : Screen("alarm", "Alarm")
}