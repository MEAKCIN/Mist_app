package com.example.app2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.app2.data.EmotionSetting // Ensure this import is correct
import kotlin.math.roundToInt

@Composable
fun ManualControlScreen(
    deviceOn: Boolean,
    emotionSettings: List<EmotionSetting>,
    onEmotionSettingChange: (List<EmotionSetting>) -> Unit,
    showDisabledMessage: () -> Unit // Kept if needed for other interactions
    // Removed onUpdateDevice from here, as it's on HomeScreen now
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (deviceOn) {
            Text(
                "Configure Emotion Settings:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f), // Allow column to take available space
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(emotionSettings) { index, setting ->
                    EmotionControlCard(
                        emotionSetting = setting,
                        onSettingChange = { updatedSetting ->
                            val newList = emotionSettings.toMutableList()
                            newList[index] = updatedSetting
                            onEmotionSettingChange(newList)
                        },
                        isEnabled = deviceOn
                    )
                }
            }
            // The "Update Device" button is now on the HomeScreen.
            // Changes made here are stored in MainScreen's state and sent when
            // "Update Device" on HomeScreen is pressed.
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Device is OFF.")
                Text("Turn it on from the Home screen to configure emotions.")
                // Call showDisabledMessage if a specific interaction attempt is made while off
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionControlCard(
    emotionSetting: EmotionSetting,
    onSettingChange: (EmotionSetting) -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled && emotionSetting.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emotionSetting.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = emotionSetting.isActive,
                    onCheckedChange = { isActive ->
                        onSettingChange(emotionSetting.copy(isActive = isActive))
                    },
                    enabled = isEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Spray Period (min): ${emotionSetting.sprayPeriod.roundToInt()}")
            MySlider(
                value = emotionSetting.sprayPeriod,
                onValueChange = { period ->
                    onSettingChange(emotionSetting.copy(sprayPeriod = period))
                },
                valueRange = 1f..30f,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled && emotionSetting.isActive
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Spray Duration (sec): ${emotionSetting.sprayDuration.roundToInt()}")
            MySlider(
                value = emotionSetting.sprayDuration,
                onValueChange = { duration ->
                    onSettingChange(emotionSetting.copy(sprayDuration = duration))
                },
                valueRange = 1f..59f,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled && emotionSetting.isActive
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier
    )
}