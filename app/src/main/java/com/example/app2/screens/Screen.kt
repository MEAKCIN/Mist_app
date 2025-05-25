package com.example.app2.screens

// Make title a var so it can be updated with stringResource
sealed class Screen(val route: String, var title: String) {
    object Home : Screen("home", "Home")
    object Manual : Screen("manual", "Manual Control")
    object Image : Screen("image", "Image Control")
    object Settings : Screen("settings", "Settings")
}