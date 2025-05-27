package com.example.app2.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R
import com.example.app2.data.EmotionSetting //
import kotlin.math.roundToInt

@Composable
fun ManualControlScreen(
    deviceOn: Boolean,
    emotionSettings: List<EmotionSetting>,
    onEmotionSettingChange: (List<EmotionSetting>) -> Unit,
    showDisabledMessage: () -> Unit,
    onApplyToDevice: () -> Unit // New callback
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (deviceOn) {
            Text(
                stringResource(R.string.configure_emotion_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onApplyToDevice,
                modifier = Modifier.fillMaxWidth(),
                enabled = deviceOn
            ) {
                Text(stringResource(R.string.apply_manual_settings_to_device)) // Add to strings.xml
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.device_off_title))
                Text(stringResource(R.string.device_off_subtitle))
                // Consider if "Apply to device" button should be visible but disabled here,
                // or completely hidden as it is now.
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

            AnimatedVisibility(
                visible = emotionSetting.isActive && isEnabled,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.spray_period_min, emotionSetting.sprayPeriod.roundToInt()))
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

                    Text(text = stringResource(R.string.spray_duration_sec, emotionSetting.sprayDuration.roundToInt()))
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