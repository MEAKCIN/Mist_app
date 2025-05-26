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

fun getDefaultEmotionSettingsForProfile(): List<EmotionSetting> { // Örnek başlangıç ayarları
    return listOf(
        EmotionSetting("Neutral", 1f, 1f, false),
        EmotionSetting("Happy", 10f, 5f, true),
        EmotionSetting("Surprise", 5f, 2f, false),
        EmotionSetting("Sad", 20f, 10f, false)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialProfile: Profile?, // Null ise yeni profil, değilse düzenleme
    onSaveProfile: (Profile) -> Unit,
    onCancel: () -> Unit
) {
    var profileName by remember { mutableStateOf(initialProfile?.name ?: "") }
    var currentEmotionSettings by remember {
        mutableStateOf(
            initialProfile?.emotionSettings?.map { it.copy() } ?: getDefaultEmotionSettingsForProfile()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (initialProfile == null) stringResource(R.string.create_new_profile) else stringResource(R.string.edit_profile_title, profileName),
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
                EmotionControlCard( // Bu sizin ManualControlScreen'deki kartınızla aynı
                    emotionSetting = setting,
                    onSettingChange = { updatedSetting ->
                        val newList = currentEmotionSettings.toMutableList()
                        newList[index] = updatedSetting
                        currentEmotionSettings = newList
                    },
                    isEnabled = true // Profil düzenlerken her zaman aktif
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
                val profileToSave = initialProfile?.copy(
                    name = profileName,
                    emotionSettings = currentEmotionSettings
                ) ?: Profile(
                    id = UUID.randomUUID().toString(),
                    name = profileName,
                    emotionSettings = currentEmotionSettings
                )
                onSaveProfile(profileToSave)
            }) {
                Text(stringResource(R.string.save_profile))
            }
        }
    }
}

