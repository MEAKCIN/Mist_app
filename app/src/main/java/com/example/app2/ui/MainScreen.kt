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
import androidx.compose.ui.res.stringResource
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
    onDarkThemeChange: (Boolean) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var deviceOn by remember { mutableStateOf(true) }

    val defaultEmotionSettings = listOf(
        EmotionSetting("Neutral", 1f, 1f, false), // Emotion names could also be localized if static
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

    // Localized strings for snackbar messages
    val syncedSuccessfullyMessage = stringResource(R.string.synced_successfully)
    val syncFailedDefaultMessage = stringResource(R.string.sync_failed_default)
    val deviceSettingsUpdatedMessage = stringResource(R.string.device_settings_updated)
    val deviceUpdateFailedMessage = stringResource(R.string.device_update_failed)
    val errorUploadingImageMessage = stringResource(R.string.error_uploading_image)
    val photoSentSuccessfullyMessage = stringResource(R.string.photo_sent_successfully)
    val sendFailedMessage = stringResource(R.string.send_failed)
    val noPhotoToSendMesssage = stringResource(R.string.no_photo_to_send)
    val deviceOffActionDisabledMessage = stringResource(R.string.device_off_action_disabled)


    fun syncDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.getDeviceStatus { success, response, status ->
                scope.launch {
                    if (success && status != null) {
                        emotionSettings = if (status.emotions.isNotEmpty()) status.emotions else defaultEmotionSettings
                        deviceOn = status.deviceOn
                        snackbarHostState.showSnackbar(syncedSuccessfullyMessage)
                    } else {
                        snackbarHostState.showSnackbar(response ?: syncFailedDefaultMessage)
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
                        response ?: if (success) deviceSettingsUpdatedMessage else deviceUpdateFailedMessage
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
                    snackbarHostState.showSnackbar(
                        errorUploadingImageMessage.format(e.localizedMessage)
                    )
                }
            }
        }
    }

    val screenHomeTitle = stringResource(R.string.home)
    val screenManualTitle = stringResource(R.string.manual_control)
    val screenImageTitle = stringResource(R.string.image_control)
    val screenSettingsTitle = stringResource(R.string.settings)

    val collapseNavDesc = stringResource(R.string.collapse_navigation)
    val expandNavDesc = stringResource(R.string.expand_navigation)


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
                                scope.launch { snackbarHostState.showSnackbar(deviceOffActionDisabledMessage) }
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
                                                    response ?: if (success) photoSentSuccessfullyMessage else sendFailedMessage
                                                )
                                                if (success) syncDevice()
                                            }
                                        }
                                    }
                                } ?: run {
                                    scope.launch { snackbarHostState.showSnackbar(noPhotoToSendMesssage) }
                                }
                            }
                        )
                    Screen.Settings ->
                        SettingsScreen(
                            darkTheme = darkTheme,
                            onDarkThemeChange = onDarkThemeChange,
                            currentLanguage = currentLanguage,
                            onLanguageChange = onLanguageChange
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
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars),
                header = {
                    IconButton(
                        onClick = { navExpanded = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = collapseNavDesc,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            ) {
                val screens = listOf(
                    Screen.Home.apply { title = screenHomeTitle },
                    Screen.Manual.apply { title = screenManualTitle },
                    Screen.Image.apply { title = screenImageTitle },
                    Screen.Settings.apply { title = screenSettingsTitle }
                )

                screens.forEach { screen ->
                    NavigationRailItem(
                        selected = selectedTab.route == screen.route,
                        onClick = {
                            selectedTab = screen
                            navExpanded = false
                        },
                        icon = {
                            val iconPainter = when (screen.route) {
                                Screen.Home.route -> painterResource(id = R.drawable.home)
                                Screen.Manual.route -> painterResource(id = R.drawable.manual_control_icon)
                                Screen.Image.route -> painterResource(id = R.drawable.image_upload)
                                Screen.Settings.route -> painterResource(id = R.drawable.settings_icon)
                                else -> painterResource(id = R.drawable.home)
                            }
                            Icon(
                                painter = iconPainter,
                                contentDescription = screen.title, // Screen titles are already localized
                                modifier = Modifier.size(36.dp),
                                tint = Color.Unspecified
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
                    contentDescription = expandNavDesc,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

//todo font büyüt
//todo renk ekle
//todo dark mode optimize olsun
//todo manual control configure kısmını sil
//todo manual kısmında açılıp kapanmalı yap