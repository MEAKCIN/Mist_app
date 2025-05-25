package com.example.app2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    deviceOn: Boolean,
    onDeviceOnSwitchChange: (Boolean) -> Unit,
    onUpdateDevice: () -> Unit,
    onSyncDevice: () -> Unit
    // Removed emotionSettings and related callbacks
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Center the simplified controls
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // Increased spacing
        ) {
            Text(text = "Device On/Off:", modifier = Modifier.weight(1f))
            Switch(
                checked = deviceOn,
                onCheckedChange = onDeviceOnSwitchChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onUpdateDevice,
            modifier = Modifier.fillMaxWidth(),
            enabled = deviceOn // Update button might only be enabled if device is on
        ) {
            Text(text = "Update Device Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSyncDevice,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sync Device Status")
        }
    }
}