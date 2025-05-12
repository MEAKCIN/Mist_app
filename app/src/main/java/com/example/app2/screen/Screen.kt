package com.example.app2.screen

sealed class Screen(val title: String) {
    object Home : Screen("Home")
    object Manual : Screen("Manual Control")
    object Image : Screen("Image Control")
    object Settings : Screen("Settings")
}