package com.example.app2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.app2.ui.MainScreen
import com.example.app2.ui.theme.App2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme = remember { mutableStateOf(false) }
            App2Theme(darkTheme = darkTheme.value) {
                MainScreen(
                    darkTheme = darkTheme.value,
                    onDarkThemeChange = { darkTheme.value = it }
                )
            }
        }
    }
}