package com.example.app2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.app2.data.Profile
import com.example.app2.R

// Added a placeholder ID for representing the "Custom" state in the dropdown.
const val CUSTOM_PROFILE_ID_PLACEHOLDER = "custom_profile_id_placeholder"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    deviceOn: Boolean,
    onDeviceOnSwitchChange: (Boolean) -> Unit,
    onUpdateDevice: () -> Unit,
    onSyncDevice: () -> Unit,
    profiles: List<Profile>,
    activeProfileId: String?, // Can now be CUSTOM_PROFILE_ID_PLACEHOLDER
    onProfileSelected: (String) -> Unit,
    isCustomActive: Boolean, // New state to indicate if custom settings are active
    customProfileName: String // New parameter for the localized "Custom" name
) {
    var profileDropdownExpanded by remember { mutableStateOf(false) }

    val currentProfileDisplayName = if (isCustomActive) {
        customProfileName
    } else {
        profiles.find { it.id == activeProfileId }?.name ?: stringResource(R.string.select_profile)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.active_profile), style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = profileDropdownExpanded,
            onExpandedChange = { profileDropdownExpanded = !profileDropdownExpanded }
        ) {
            OutlinedTextField(
                value = currentProfileDisplayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_profile)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = profileDropdownExpanded,
                onDismissRequest = { profileDropdownExpanded = false }
            ) {
                // If custom is active, "Custom" is displayed in the TextField but isn't a selectable item.
                // Selecting any actual profile will turn off custom mode.
                profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = { Text(profile.name) },
                        onClick = {
                            onProfileSelected(profile.id) // This will turn off isCustomActive in MainScreen
                            profileDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.device_on_off), modifier = Modifier.weight(1f))
            Switch(
                checked = deviceOn,
                onCheckedChange = onDeviceOnSwitchChange
            )
        }

        Button(
            onClick = onUpdateDevice,
            modifier = Modifier.fillMaxWidth(),
            // Update button on Home Screen should ideally work with the *selected saved profile*.
            // If custom settings are active, this button might be disabled,
            // or it could apply the last *saved* active profile.
            // The current implementation in MainScreen tries to apply the last saved active profile if custom is active.
            enabled = deviceOn && (activeProfileId != null && activeProfileId != CUSTOM_PROFILE_ID_PLACEHOLDER || profiles.isNotEmpty())
        ) {
            Text(text = stringResource(R.string.update_device_settings))
        }

        Button(
            onClick = onSyncDevice,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.sync_device_status))
        }
    }
}