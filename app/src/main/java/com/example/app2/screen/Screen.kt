package com.example.app2.screen

sealed class Screen(val title: String) {
    object Home : Screen("Ana Sayfa")
    object Manual : Screen("Manuel Kontrol")
    object Image : Screen("Görüntü Kontrolü")
}