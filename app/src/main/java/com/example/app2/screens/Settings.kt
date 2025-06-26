package com.example.app2.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R // Import R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val languages = listOf(
        Pair("en", stringResource(R.string.change_language_to_english)),
        Pair("tr", stringResource(R.string.change_language_to_turkish))
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedLanguageLabel by remember {
        mutableStateOf(languages.find { it.first == currentLanguage }?.second ?: languages.first().second)
    }


    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.dark_theme), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = darkTheme,
                onCheckedChange = onDarkThemeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLanguageLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_language)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor() // Important for the dropdown to anchor correctly
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { (langCode, langName) ->
                    DropdownMenuItem(
                        text = { Text(langName) },
                        onClick = {
                            selectedLanguageLabel = langName
                            onLanguageChange(langCode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}