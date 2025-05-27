package com.example.app2.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder // Required for API 28+
import android.net.Uri // Required for Uri
import android.os.Build // Required for Build.VERSION.SDK_INT
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch // Required for launch without input
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.app2.data.Profile
import com.example.app2.network.NetworkManager
import com.example.app2.screens.EditProfileScreen
import com.example.app2.screens.HomeScreen
import com.example.app2.screens.ImageControlScreen
import com.example.app2.screens.ManualControlScreen
import com.example.app2.screens.ProfileManagementScreen
import com.example.app2.screens.Screen
import com.example.app2.screens.SettingsScreen
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


// Helper functions for SharedPreferences and Profile Management
private val gson = Gson()
private const val PROFILES_KEY = "user_profiles"
private const val ACTIVE_PROFILE_ID_KEY = "active_profile_id"

fun defaultDeviceEmotionSettings(): List<EmotionSetting> {
    return listOf(
        EmotionSetting("Neutral", 1f, 1f, false),
        EmotionSetting("Happy", 10f, 5f, true),
        EmotionSetting("Surprise", 5f, 2f, false),
        EmotionSetting("Sad", 20f, 10f, false)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    fun saveProfilesToPrefs(profiles: List<Profile>) {
        val json = gson.toJson(profiles)
        sharedPreferences.edit().putString(PROFILES_KEY, json).apply()
    }

    fun loadProfilesFromPrefs(): MutableList<Profile> {
        val json = sharedPreferences.getString(PROFILES_KEY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<Profile>>() {}.type
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    var userProfiles by remember { mutableStateOf(loadProfilesFromPrefs()) }
    var activeProfileId by remember {
        mutableStateOf(sharedPreferences.getString(ACTIVE_PROFILE_ID_KEY, null))
    }
    var isCustomSettingsActive by remember { mutableStateOf(false) }

    val defaultProfileNameText = stringResource(id = R.string.default_profile_name)
    val customProfileNameText = stringResource(id = R.string.custom_profile_name)

    LaunchedEffect(Unit) {
        if (userProfiles.isEmpty()) {
            val defaultProfile = Profile(name = defaultProfileNameText, emotionSettings = defaultDeviceEmotionSettings())
            userProfiles = mutableListOf(defaultProfile)
            saveProfilesToPrefs(userProfiles)
            if (activeProfileId == null) {
                activeProfileId = defaultProfile.id
                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                isCustomSettingsActive = false
            }
        } else if (activeProfileId == null && userProfiles.isNotEmpty()) {
            activeProfileId = userProfiles.first().id
            sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
            isCustomSettingsActive = false
        }
    }

    fun getActiveProfile(): Profile? {
        if (isCustomSettingsActive) return null
        return userProfiles.find { it.id == activeProfileId }
    }

    var currentDeviceEmotionSettings by remember {
        mutableStateOf(getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings())
    }
    var deviceOn by remember { mutableStateOf(true) }

    // State for the photo in ImageControlScreen
    var photoBitmapForImageScreen by remember { mutableStateOf<Bitmap?>(null) }


    LaunchedEffect(activeProfileId, userProfiles, isCustomSettingsActive) {
        if (!isCustomSettingsActive) {
            val newActiveProfile = getActiveProfile()
            currentDeviceEmotionSettings = newActiveProfile?.emotionSettings ?: defaultDeviceEmotionSettings()
        }
    }

    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var navExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditProfileScreenFor by remember { mutableStateOf<Profile?>(null) }
    var isEditingNewProfile by remember { mutableStateOf(false) }

    val syncedSuccessfullyMessage = stringResource(R.string.synced_successfully)
    val syncFailedDefaultMessage = stringResource(R.string.sync_failed_default)
    val deviceSettingsUpdatedMessage = stringResource(R.string.device_settings_updated)
    val deviceUpdateFailedMessage = stringResource(R.string.device_update_failed)
    val errorUploadingImageMessage = stringResource(R.string.error_uploading_image)
    val photoSentSuccessfullyMessage = stringResource(R.string.photo_sent_successfully)
    val sendFailedMessage = stringResource(R.string.send_failed)
    val noPhotoToSendMesssage = stringResource(R.string.no_photo_to_send)
    val deviceOffActionDisabledMessage = stringResource(R.string.device_off_action_disabled)
    val profileSavedMessage = stringResource(R.string.profile_saved_successfully)
    val profileDeletedMessage = stringResource(R.string.profile_deleted_successfully)
    val manualSettingsAppliedMessage = stringResource(R.string.manual_settings_applied)
    val noProfilesToUpdateDeviceMessage = stringResource(R.string.no_profiles_to_update_device)
    val syncedSettingsAppliedAsCustomMessage = stringResource(R.string.synced_settings_applied_as_custom)
    val syncedSettingsMatchedProfileMessage = stringResource(R.string.synced_settings_matched_profile)
    val errorSelectingImageMessage = stringResource(R.string.error_selecting_image)


    fun syncDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.getDeviceStatus { success, response, syncedStatus ->
                scope.launch {
                    if (success && syncedStatus != null) {
                        deviceOn = syncedStatus.deviceOn

                        val syncedEmotions = syncedStatus.emotions
                        val matchingProfile = userProfiles.find { profile ->
                            profile.emotionSettings.toSet() == syncedEmotions.toSet()
                        }

                        if (matchingProfile != null) {
                            activeProfileId = matchingProfile.id
                            sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                            isCustomSettingsActive = false
                            // currentDeviceEmotionSettings updated by LaunchedEffect
                            snackbarHostState.showSnackbar(syncedSettingsMatchedProfileMessage.format(matchingProfile.name))
                        } else {
                            currentDeviceEmotionSettings = syncedEmotions
                            isCustomSettingsActive = true
                            snackbarHostState.showSnackbar(syncedSettingsAppliedAsCustomMessage)
                        }
                    } else {
                        snackbarHostState.showSnackbar(response ?: syncFailedDefaultMessage)
                    }
                }
            }
        }
    }

    fun updateDeviceSettingsWithProfile() {
        val profileToUse = getActiveProfile()
        if (profileToUse == null) {
            if(isCustomSettingsActive) {
                val lastSavedActiveProfileId = sharedPreferences.getString(ACTIVE_PROFILE_ID_KEY, null)
                val lastSavedProfile = userProfiles.find { it.id == lastSavedActiveProfileId }
                if (lastSavedProfile != null) {
                    scope.launch(Dispatchers.IO) {
                        NetworkManager.updateDeviceRequest(
                            deviceOn,
                            lastSavedProfile.emotionSettings
                        ) { success, response ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    response ?: if (success) deviceSettingsUpdatedMessage else deviceUpdateFailedMessage
                                )
                            }
                        }
                    }
                } else {
                    scope.launch { snackbarHostState.showSnackbar(noProfilesToUpdateDeviceMessage) }
                }
                return
            } else {
                scope.launch { snackbarHostState.showSnackbar(noProfilesToUpdateDeviceMessage) }
                return
            }
        }
        scope.launch(Dispatchers.IO) {
            NetworkManager.updateDeviceRequest(
                deviceOn,
                profileToUse.emotionSettings
            ) { success, response ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        response ?: if (success) deviceSettingsUpdatedMessage else deviceUpdateFailedMessage
                    )
                }
            }
        }
    }

    fun applyManualSettingsToDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.updateDeviceRequest(
                deviceOn,
                currentDeviceEmotionSettings
            ) { success, response ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        response ?: if (success) manualSettingsAppliedMessage else deviceUpdateFailedMessage
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
        photoBitmapForImageScreen = bitmap // Update the state with the taken photo
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                photoBitmapForImageScreen = bitmap // Update the state with the uploaded photo
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        errorSelectingImageMessage.format(e.localizedMessage ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun sendPhotoFromImageScreen() {
        photoBitmapForImageScreen?.let { bitmap ->
            scope.launch(Dispatchers.IO) {
                NetworkManager.sendPhotoRequest(bitmap) { success, response ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            response ?: if (success) photoSentSuccessfullyMessage else sendFailedMessage
                        )
                        if (success) {
                            photoBitmapForImageScreen = null // Clear photo after successful send
                        }
                    }
                }
            }
        } ?: run {
            scope.launch {
                snackbarHostState.showSnackbar(noPhotoToSendMesssage)
            }
        }
    }


    val screenHomeTitle = stringResource(R.string.home)
    val screenManualTitle = stringResource(R.string.manual_control)
    val screenImageTitle = stringResource(R.string.image_control)
    val screenSettingsTitle = stringResource(R.string.settings)
    val screenProfileManagementTitle = stringResource(R.string.profile_management)

    val collapseNavDesc = stringResource(R.string.collapse_navigation)
    val expandNavDesc = stringResource(R.string.expand_navigation)

    if (isEditingNewProfile || showEditProfileScreenFor != null) {
        val profileBeingEdited = if (isEditingNewProfile) null else showEditProfileScreenFor
        val initialSettingsForEdit = if (isEditingNewProfile && isCustomSettingsActive) {
            currentDeviceEmotionSettings
        } else {
            profileBeingEdited?.emotionSettings ?: defaultDeviceEmotionSettings()
        }

        EditProfileScreen(
            initialProfile = if (isEditingNewProfile) {
                Profile(id = UUID.randomUUID().toString(), name = "", emotionSettings = initialSettingsForEdit)
            } else {
                showEditProfileScreenFor?.copy(emotionSettings = initialSettingsForEdit) // Ensure a copy is passed
            },
            onSaveProfile = { profileToSave ->
                val updatedProfiles = userProfiles.toMutableList()
                val existingIndex = updatedProfiles.indexOfFirst { it.id == profileToSave.id }

                if (existingIndex != -1) {
                    updatedProfiles[existingIndex] = profileToSave
                } else {
                    updatedProfiles.add(profileToSave)
                }
                userProfiles = updatedProfiles
                saveProfilesToPrefs(userProfiles)

                activeProfileId = profileToSave.id
                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                isCustomSettingsActive = false
                // currentDeviceEmotionSettings will be updated by LaunchedEffect

                scope.launch { snackbarHostState.showSnackbar(profileSavedMessage) }
                showEditProfileScreenFor = null
                isEditingNewProfile = false
            },
            onCancel = {
                showEditProfileScreenFor = null
                isEditingNewProfile = false
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
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
                            onDeviceOnSwitchChange = { changedDeviceOn ->
                                deviceOn = changedDeviceOn
                                if (isCustomSettingsActive) {
                                    applyManualSettingsToDevice()
                                } else if (activeProfileId != null) {
                                    updateDeviceSettingsWithProfile()
                                }
                            },
                            onUpdateDevice = {
                                updateDeviceSettingsWithProfile()
                            },
                            onSyncDevice = { syncDevice() },
                            profiles = userProfiles,
                            activeProfileId = activeProfileId,
                            onProfileSelected = { profileId ->
                                activeProfileId = profileId
                                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, profileId).apply()
                                isCustomSettingsActive = false
                                updateDeviceSettingsWithProfile()
                            },
                            isCustomActive = isCustomSettingsActive,
                            customProfileName = customProfileNameText
                        )
                    Screen.Manual ->
                        ManualControlScreen(
                            deviceOn = deviceOn,
                            emotionSettings = currentDeviceEmotionSettings,
                            onEmotionSettingChange = { updatedSettings ->
                                currentDeviceEmotionSettings = updatedSettings
                                isCustomSettingsActive = true
                            },
                            showDisabledMessage = {
                                scope.launch { snackbarHostState.showSnackbar(deviceOffActionDisabledMessage) }
                            },
                            onApplyToDevice = {
                                applyManualSettingsToDevice()
                            }
                        )
                    Screen.ProfileManagement -> ProfileManagementScreen(
                        profiles = userProfiles,
                        onAddProfile = { isEditingNewProfile = true; showEditProfileScreenFor = null },
                        onUpdateProfile = { profileToEdit -> showEditProfileScreenFor = profileToEdit; isEditingNewProfile = false },
                        onDeleteProfile = { profileToDelete ->
                            val wasDeletedProfileActive = activeProfileId == profileToDelete.id
                            userProfiles = userProfiles.filterNot { it.id == profileToDelete.id }.toMutableList()
                            saveProfilesToPrefs(userProfiles)

                            if (wasDeletedProfileActive || isCustomSettingsActive) {
                                activeProfileId = userProfiles.firstOrNull()?.id
                                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                                isCustomSettingsActive = false
                                if (activeProfileId != null) {
                                    updateDeviceSettingsWithProfile()
                                } else {
                                    currentDeviceEmotionSettings = defaultDeviceEmotionSettings()
                                }
                            }
                            scope.launch { snackbarHostState.showSnackbar(profileDeletedMessage) }
                        },
                        onNavigateToEditProfile = { profile ->
                            if (profile == null) {
                                isEditingNewProfile = true
                                showEditProfileScreenFor = null
                            } else {
                                isEditingNewProfile = false
                                showEditProfileScreenFor = profile
                            }
                        }
                    )
                    Screen.Image ->
                        ImageControlScreen(
                            photoBitmap = photoBitmapForImageScreen,
                            onTakePhoto = { photoLauncher.launch() }, // Use launch() for TakePicturePreview
                            onUploadPhoto = { uploadLauncher.launch("image/*") },
                            onSendPhoto = { sendPhotoFromImageScreen() } // Call the new send function
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

            if (navExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(Unit) { detectTapGestures(onTap = { navExpanded = false }) }
                )
                NavigationRail(
                    modifier = Modifier
                        .width(220.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.statusBars),
                    header = {
                        IconButton(
                            onClick = { navExpanded = false },
                            modifier = Modifier.size(48.dp).padding(top = 8.dp, start = 8.dp)
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
                        Screen.ProfileManagement.apply { title = screenProfileManagementTitle },
                        Screen.Manual.apply { title = screenManualTitle },
                        Screen.Image.apply { title = screenImageTitle },
                        Screen.Settings.apply { title = screenSettingsTitle }
                    )

                    screens.forEach { screen ->
                        NavigationRailItem(
                            selected = selectedTab.route == screen.route,
                            onClick = {
                                if (selectedTab.route != Screen.Image.route && screen.route == Screen.Image.route) {
                                    // Clear previous photo when navigating to Image screen if it wasn't already there
                                    // photoBitmapForImageScreen = null // Optional: Clear photo when navigating to this screen
                                } else if (selectedTab.route == Screen.Image.route && screen.route != Screen.Image.route) {
                                    // Clear photo when navigating away from Image screen
                                    photoBitmapForImageScreen = null
                                }
                                selectedTab = screen
                                navExpanded = false
                            },
                            icon = {
                                val iconPainter = when (screen.route) {
                                    Screen.Home.route -> painterResource(id = R.drawable.home)
                                    Screen.Manual.route -> painterResource(id = R.drawable.manual_control_icon)
                                    Screen.Image.route -> painterResource(id = R.drawable.image_upload)
                                    Screen.Settings.route -> painterResource(id = R.drawable.settings_icon)
                                    Screen.ProfileManagement.route -> painterResource(id = R.drawable.profile_management)
                                    else -> painterResource(id = R.drawable.home)
                                }
                                Icon(
                                    painter = iconPainter,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(30.dp),
                                    tint = Color.Unspecified
                                )
                            },
                            label = { Text(text = screen.title, style = MaterialTheme.typography.labelMedium) },
                            alwaysShowLabel = true
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = { navExpanded = true },
                    modifier = Modifier
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
}