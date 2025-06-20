package com.example.app2.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    deviceOn: Boolean,
    currentEmotion: String,
    sprayPeriod: Float,
    sprayDuration: Float,
    onSwitchChange: (Boolean) -> Unit,
    onPeriodChange: (Float) -> Unit,
    onDurationChange: (Float) -> Unit,
    onUpdateDevice: () -> Unit,
    onSyncDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Current Situation: $currentEmotion")
        Text(text = "Spray Period (min): ${sprayPeriod.roundToInt()}")
        MySlider(
            value = sprayPeriod,
            onValueChange = onPeriodChange,
            valueRange = 1f..30f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = "Spray Duration (sec): ${sprayDuration.roundToInt()}")
        MySlider(
            value = sprayDuration,
            onValueChange = onDurationChange,
            valueRange = 1f..59f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Device On/Off:")
            Switch(
                checked = deviceOn,
                onCheckedChange = onSwitchChange
            )
        }
        Button(onClick = onUpdateDevice) {
            Text(text = "Update Device")
        }
        Button(onClick = onSyncDevice) {
            Text(text = "Sync Device")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val thumbRadiusDp = 10.dp
    val thumbRadiusPx = with(LocalDensity.current) { thumbRadiusDp.toPx() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place((-thumbRadiusPx).roundToInt(), 0)
            }
        }
    )
}