package com.example.app2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R
import com.example.app2.data.EmotionSetting
import com.example.app2.data.Profile
import java.util.UUID

// --- इं Ensure this function is exactly as below ---
fun getDefaultEmotionSettingsForProfile(): List<EmotionSetting> {
    return listOf(
        EmotionSetting("Neutral", 1f, 1f, false),
        EmotionSetting("Happy", 10f, 5f, true),
        EmotionSetting("Angry", 5f, 2f, false), // VERIFY: Changed "Surprise" to "Angry"
        EmotionSetting("Sad", 20f, 10f, false)
    )
}
// --- End of critical section ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialProfile: Profile?,
    onSaveProfile: (Profile) -> Unit,
    onCancel: () -> Unit
) {
    var profileName by remember { mutableStateOf(initialProfile?.name ?: "") }
    var currentEmotionSettings by remember {
        mutableStateOf(
            initialProfile?.emotionSettings?.map { it.copy() } ?: getDefaultEmotionSettingsForProfile() // Uses the function above
        )
    }

    val defaultProfileNameString = stringResource(id = R.string.default_profile_name)
    val createNewProfileString = stringResource(R.string.create_new_profile)
    // Use initialProfile.name directly in the title if editing, or fallback to createNewProfileString
    val titleText = initialProfile?.name?.takeIf { it.isNotEmpty() }
        ?.let { stringResource(R.string.edit_profile_title, it) }
        ?: createNewProfileString


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = profileName,
            onValueChange = { profileName = it },
            label = { Text(stringResource(R.string.profile_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            stringResource(R.string.configure_emotion_settings_for_profile),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(currentEmotionSettings) { index, setting ->
                EmotionControlCard(
                    emotionSetting = setting,
                    onSettingChange = { updatedSetting ->
                        val newList = currentEmotionSettings.toMutableList()
                        newList[index] = updatedSetting
                        currentEmotionSettings = newList
                    },
                    isEnabled = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val profileIdToUse = initialProfile?.id ?: UUID.randomUUID().toString()
                val finalProfileName = profileName.ifBlank { defaultProfileNameString }

                val profileToSave = Profile(
                    id = profileIdToUse,
                    name = finalProfileName,
                    emotionSettings = currentEmotionSettings
                )
                onSaveProfile(profileToSave)
            }) {
                Text(stringResource(R.string.save_profile))
            }
        }
    }
}