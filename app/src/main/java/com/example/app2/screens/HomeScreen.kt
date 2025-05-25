package com.example.app2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R

@Composable
fun HomeScreen(
    deviceOn: Boolean,
    onDeviceOnSwitchChange: (Boolean) -> Unit,
    onUpdateDevice: () -> Unit,
    onSyncDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(text = stringResource(R.string.device_on_off), modifier = Modifier.weight(1f))
            Switch(
                checked = deviceOn,
                onCheckedChange = onDeviceOnSwitchChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onUpdateDevice,
            modifier = Modifier.fillMaxWidth(),
            enabled = deviceOn
        ) {
            Text(text = stringResource(R.string.update_device_settings))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSyncDevice,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.sync_device_status))
        }
    }
}