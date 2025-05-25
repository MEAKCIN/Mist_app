package com.example.app2.ui

import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.app2.R
import com.example.app2.data.EmotionSetting
import com.example.app2.network.NetworkManager
import com.example.app2.screens.HomeScreen
import com.example.app2.screens.ImageControlScreen
import com.example.app2.screens.ManualControlScreen
import com.example.app2.screens.SettingsScreen
import com.example.app2.screens.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var deviceOn by remember { mutableStateOf(true) }

    val defaultEmotionSettings = listOf(
        EmotionSetting("Neutral", 1f, 1f, false),
        EmotionSetting("Happy", 10f, 5f, true),
        EmotionSetting("Surprise", 5f, 2f, false),
        EmotionSetting("Sad", 20f, 10f, false)
    )
    var emotionSettings by remember { mutableStateOf<List<EmotionSetting>>(defaultEmotionSettings) }

    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var navExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun syncDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.getDeviceStatus { success, response, status ->
                scope.launch {
                    if (success && status != null) {
                        emotionSettings = if (status.emotions.isNotEmpty()) status.emotions else defaultEmotionSettings
                        deviceOn = status.deviceOn
                        snackbarHostState.showSnackbar("Synced successfully")
                    } else {
                        snackbarHostState.showSnackbar(response ?: "Sync failed. Using default/previous settings.")
                    }
                }
            }
        }
    }

    fun updateDeviceSettings() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.updateDeviceRequest(
                deviceOn,
                emotionSettings
            ) { success, response ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        response ?: if (success) "Device settings updated" else "Device update failed"
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        syncDevice()
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        photoBitmap = bitmap
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                photoBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error uploading image: ${e.localizedMessage}")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = if (!navExpanded) 72.dp else 0.dp)
            ) {
                when (selectedTab) {
                    Screen.Home ->
                        HomeScreen(
                            deviceOn = deviceOn,
                            onDeviceOnSwitchChange = { deviceOn = it },
                            onUpdateDevice = { updateDeviceSettings() },
                            onSyncDevice = { syncDevice() }
                        )
                    Screen.Manual ->
                        ManualControlScreen(
                            deviceOn = deviceOn,
                            emotionSettings = emotionSettings,
                            onEmotionSettingChange = { updatedSettings ->
                                emotionSettings = updatedSettings
                            },
                            showDisabledMessage = {
                                scope.launch { snackbarHostState.showSnackbar("Device is Off. Action disabled.") }
                            }
                        )
                    Screen.Image ->
                        ImageControlScreen(
                            photoBitmap = photoBitmap,
                            onTakePhoto = { photoLauncher.launch(null) },
                            onUploadPhoto = { uploadLauncher.launch("image/*") },
                            onSendPhoto = {
                                photoBitmap?.let { bitmap ->
                                    scope.launch(Dispatchers.IO) {
                                        NetworkManager.sendPhotoRequest(bitmap) { success, response ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    response ?: if (success) "Photo sent successfully" else "Send failed"
                                                )
                                                if (success) syncDevice()
                                            }
                                        }
                                    }
                                } ?: run {
                                    scope.launch { snackbarHostState.showSnackbar("No photo to send") }
                                }
                            }
                        )
                    Screen.Settings ->
                        SettingsScreen(
                            darkTheme = darkTheme,
                            onDarkThemeChange = onDarkThemeChange
                        )
                }
            }
        }
        if (navExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) { detectTapGestures(onTap = { navExpanded = false }) }
            )
            NavigationRail(
                modifier = Modifier
                    .width(200.dp)
                    .background(MaterialTheme.colorScheme.surface),
                header = {
                    IconButton(
                        onClick = { navExpanded = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Collapse Navigation",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            ) {
                listOf(Screen.Home, Screen.Manual, Screen.Image, Screen.Settings).forEach { screen ->
                    NavigationRailItem(
                        selected = selectedTab == screen,
                        onClick = {
                            selectedTab = screen
                            navExpanded = false
                        },
                        icon = {
                            val iconPainter = when (screen) {
                                Screen.Home -> painterResource(id = R.drawable.home)
                                Screen.Manual -> painterResource(id = R.drawable.manual_control_icon)
                                Screen.Image -> painterResource(id = R.drawable.image_upload)
                                Screen.Settings -> painterResource(id = R.drawable.settings_icon)
                            }
                            Icon(
                                painter = iconPainter,
                                contentDescription = screen.title,
                                modifier = Modifier.size(36.dp),
                                // MODIFICATION HERE:
                                tint = Color.Unspecified // Apply no explicit tint, use original icon colors
                            )
                        },
                        label = { Text(text = screen.title) },
                        alwaysShowLabel = true
                    )
                }
            }
        } else {
            IconButton(
                onClick = { navExpanded = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, start = 16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Expand Navigation",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}