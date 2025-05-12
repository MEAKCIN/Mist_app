// File: src/main/java/com/example/app2/ManualControlScreen.kt
package com.example.app2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ManualControlScreen(
    deviceOn: Boolean,
    currentEmotion: String,
    onEmotionChange: (String) -> Unit,
    showDisabledMessage: () -> Unit,
    onUpdateDevice: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (deviceOn) "Current State: $currentEmotion" else "Current State: Device Off",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        val emotions = listOf("Neutral", "Happy", "Angry", "Sad")
        emotions.forEach { emotion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(text = emotion, modifier = Modifier.weight(1f))
                Switch(
                    checked = currentEmotion == emotion,
                    onCheckedChange = {
                        if (deviceOn) {
                            onEmotionChange(emotion)
                        } else {
                            showDisabledMessage()
                        }
                    },
                    enabled = deviceOn
                )
            }
        }
        Button(onClick = onUpdateDevice) {
            Text(text = "Update Device")
        }
    }
}