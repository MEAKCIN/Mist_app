package com.example.app2.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R
import com.example.app2.alarm.AlarmReceiver
import com.example.app2.data.Alarm
import com.example.app2.data.AlarmMode
import com.example.app2.data.Profile
import java.util.*

@Composable
fun AlarmScreen(
    alarms: List<Alarm>,
    profiles: List<Profile>,
    onAddAlarm: (Alarm) -> Unit,
    onUpdateAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (profiles.isEmpty()) {
                    Toast.makeText(context, "Please create a profile first.", Toast.LENGTH_SHORT).show()
                } else {
                    alarmToEdit = null
                    showEditDialog = true
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alarms) { alarm ->
                val profileName = profiles.find { it.id == alarm.profileId }?.name ?: "Unknown"
                AlarmCard(
                    alarm = alarm,
                    profileName = profileName,
                    onEdit = {
                        alarmToEdit = it
                        showEditDialog = true
                    },
                    onDelete = {
                        cancelAlarm(context, it)
                        onDeleteAlarm(it)
                    },
                    onToggle = { updatedAlarm, isEnabled ->
                        val newAlarm = updatedAlarm.copy(isEnabled = isEnabled)
                        onUpdateAlarm(newAlarm)
                        if (isEnabled) {
                            scheduleAlarm(context, newAlarm)
                        } else {
                            cancelAlarm(context, newAlarm)
                        }
                    }
                )
            }
        }

        if (showEditDialog) {
            EditAlarmDialog(
                alarm = alarmToEdit,
                profiles = profiles,
                onDismiss = { showEditDialog = false },
                onSave = { updatedAlarm ->
                    alarmToEdit?.let { cancelAlarm(context, it) }

                    if (alarmToEdit == null) {
                        onAddAlarm(updatedAlarm)
                    } else {
                        onUpdateAlarm(updatedAlarm)
                    }
                    if (updatedAlarm.isEnabled) {
                        scheduleAlarm(context, updatedAlarm)
                    }
                    showEditDialog = false
                    alarmToEdit = null
                }
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    profileName: String,
    onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(alarm.name, style = MaterialTheme.typography.titleLarge)
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { isEnabled -> onToggle(alarm, isEnabled) }
                )
            }
            Spacer(Modifier.height(8.dp))
            val timeText = if (alarm.mode == AlarmMode.SINGLE) {
                String.format("%02d:%02d", alarm.startHour, alarm.startMinute)
            } else {
                String.format(
                    "%02d:%02d - %02d:%02d",
                    alarm.startHour, alarm.startMinute,
                    alarm.endHour, alarm.endMinute
                )
            }
            Text(text = "Time: $timeText", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Profile: $profileName", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Mode: ${alarm.mode.name}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onEdit(alarm) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_alarm))
                }
                IconButton(onClick = { onDelete(alarm) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_alarm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(
    alarm: Alarm?,
    profiles: List<Profile>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    var name by remember { mutableStateOf(alarm?.name ?: "") }
    var mode by remember { mutableStateOf(alarm?.mode ?: AlarmMode.SINGLE) }

    var selectedProfile by remember {
        mutableStateOf(profiles.find { it.id == alarm?.profileId } ?: profiles.first())
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val startState = rememberTimePickerState(
        initialHour = alarm?.startHour ?: calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = alarm?.startMinute ?: calendar.get(Calendar.MINUTE)
    )
    val endState = rememberTimePickerState(
        initialHour = alarm?.endHour ?: (startState.hour + 1),
        initialMinute = alarm?.endMinute ?: startState.minute
    )

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarm == null) stringResource(R.string.add_alarm) else stringResource(R.string.edit_alarm)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.alarm_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProfile.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profile") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = {
                                    selectedProfile = profile
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(stringResource(R.string.alarm_mode), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == AlarmMode.SINGLE, onClick = { mode = AlarmMode.SINGLE })
                    Text(stringResource(R.string.alarm_mode_single), Modifier.padding(end = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == AlarmMode.RANGE, onClick = { mode = AlarmMode.RANGE })
                    Text(stringResource(R.string.alarm_mode_range))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.start_time), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { showStartTimePicker = true }) {
                        Text(String.format("%02d:%02d", startState.hour, startState.minute))
                    }
                }
                if (mode == AlarmMode.RANGE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.end_time), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { showEndTimePicker = true }) {
                            Text(String.format("%02d:%02d", endState.hour, endState.minute))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newOrUpdatedAlarm = alarm?.copy(
                    name = name,
                    profileId = selectedProfile.id,
                    mode = mode,
                    startHour = startState.hour,
                    startMinute = startState.minute,
                    endHour = if (mode == AlarmMode.RANGE) endState.hour else null,
                    endMinute = if (mode == AlarmMode.RANGE) endState.minute else null
                ) ?: Alarm(
                    name = name.ifBlank { "Alarm" },
                    profileId = selectedProfile.id,
                    mode = mode,
                    startHour = startState.hour,
                    startMinute = startState.minute,
                    endHour = if (mode == AlarmMode.RANGE) endState.hour else null,
                    endMinute = if (mode == AlarmMode.RANGE) endState.minute else null
                )
                onSave(newOrUpdatedAlarm)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )

    if (showStartTimePicker) {
        TimePickerDialog(onDismissRequest = { showStartTimePicker = false }, confirmButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("OK") }}) {
            TimePicker(state = startState)
        }
    }
    if (showEndTimePicker) {
        TimePickerDialog(onDismissRequest = { showEndTimePicker = false }, confirmButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("OK") }}) {
            TimePicker(state = endState)
        }
    }
}

private fun scheduleAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val onIntent = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_ALARM_TRIGGER
        putExtra(AlarmReceiver.EXTRA_DEVICE_ON, true)
        putExtra(AlarmReceiver.EXTRA_PROFILE_ID_TO_ACTIVATE, alarm.profileId)
    }
    val onPendingIntent = PendingIntent.getBroadcast(
        context, alarm.id.hashCode(), onIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val onCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.startHour)
        set(Calendar.MINUTE, alarm.startMinute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) { add(Calendar.DATE, 1) }
    }
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, onCalendar.timeInMillis, onPendingIntent)

    if (alarm.mode == AlarmMode.RANGE && alarm.endHour != null && alarm.endMinute != null) {
        val offIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_DEVICE_ON, false)
            putExtra(AlarmReceiver.EXTRA_PROFILE_ID_TO_ACTIVATE, alarm.profileId)
        }
        val offPendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.hashCode() + 1, offIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val offCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.endHour)
            set(Calendar.MINUTE, alarm.endMinute)
            set(Calendar.SECOND, 0)
            if (before(onCalendar)) { add(Calendar.DATE, 1) }
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, offCalendar.timeInMillis, offPendingIntent)
    }
}

private fun cancelAlarm(context: Context, alarm: Alarm) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val onIntent = Intent(context, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_ALARM_TRIGGER }
    val onPendingIntent = PendingIntent.getBroadcast(
        context, alarm.id.hashCode(), onIntent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (onPendingIntent != null) {
        alarmManager.cancel(onPendingIntent)
        onPendingIntent.cancel()
    }

    if (alarm.mode == AlarmMode.RANGE) {
        val offIntent = Intent(context, AlarmReceiver::class.java).apply { action = AlarmReceiver.ACTION_ALARM_TRIGGER }
        val offPendingIntent = PendingIntent.getBroadcast(
            context, alarm.id.hashCode() + 1, offIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (offPendingIntent != null) {
            alarmManager.cancel(offPendingIntent)
            offPendingIntent.cancel()
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}