package com.example.app2.ui

import android.content.Context
import android.content.SharedPreferences
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
// Import a suitable icon for profile management, e.g., AccountCircle or SettingsSuggest

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
private const val CUSTOM_PROFILE_ID_PLACEHOLDER = "custom_profile_id_placeholder" // Placeholder for custom

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
                // Log error or handle corrupt data
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
    val customProfileNameText = stringResource(id = R.string.custom_profile_name) // Add this to strings.xml

    // Initialize default profile if none exist
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
            // If activeProfileId is null but profiles exist, set the first one as active
            activeProfileId = userProfiles.first().id
            sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
            isCustomSettingsActive = false
        }
    }


    fun getActiveProfile(): Profile? {
        if (isCustomSettingsActive) return null // No specific profile when custom is active
        return userProfiles.find { it.id == activeProfileId }
    }

    var currentDeviceEmotionSettings by remember {
        mutableStateOf(getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings())
    }
    var deviceOn by remember { mutableStateOf(true) }

    LaunchedEffect(activeProfileId, userProfiles, isCustomSettingsActive) {
        if (!isCustomSettingsActive) {
            val newActiveProfile = getActiveProfile()
            currentDeviceEmotionSettings = newActiveProfile?.emotionSettings ?: defaultDeviceEmotionSettings()
        }
        // If isCustomSettingsActive is true, currentDeviceEmotionSettings is managed by ManualControlScreen's changes
    }

    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var navExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditProfileScreenFor by remember { mutableStateOf<Profile?>(null) }
    var isEditingNewProfile by remember { mutableStateOf(false) }


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
    val profileSavedMessage = stringResource(R.string.profile_saved_successfully)
    val profileDeletedMessage = stringResource(R.string.profile_deleted_successfully)
    val manualSettingsAppliedMessage = stringResource(R.string.manual_settings_applied) // Add to strings.xml
    val noProfilesToUpdateDeviceMessage = stringResource(R.string.no_profiles_to_update_device)


    fun syncDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.getDeviceStatus { success, response, status ->
                scope.launch {
                    if (success && status != null) {
                        deviceOn = status.deviceOn
                        // If not in custom mode, and a profile is active,
                        // you might want to compare or decide if device status should override profile.
                        // For now, let's assume local profile/custom settings are the source of truth for sending.
                        // Syncing primarily updates the 'deviceOn' status and confirms connectivity.
                        // If you want synced emotions to update currentDeviceEmotionSettings:
                        // if (!isCustomSettingsActive) {
                        //    currentDeviceEmotionSettings = if (status.emotions.isNotEmpty()) status.emotions
                        //                                else getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings()
                        // }
                        snackbarHostState.showSnackbar(syncedSuccessfullyMessage)
                    } else {
                        snackbarHostState.showSnackbar(response ?: syncFailedDefaultMessage)
                    }
                }
            }
        }
    }

    fun updateDeviceSettingsWithProfile() { // Renamed to be specific
        if (isCustomSettingsActive) {
            // This function should ideally not be called if custom settings are active from HomeScreen's update button.
            // The "Update Device Settings" button in HomeScreen should be disabled or act differently
            // if custom settings are active. For now, we proceed, but this indicates a UI/UX refinement.
        }

        val profileToUse = getActiveProfile()
        if (profileToUse == null && !isCustomSettingsActive) {
            scope.launch { snackbarHostState.showSnackbar(noProfilesToUpdateDeviceMessage) }
            return
        }

        val settingsToSend = profileToUse?.emotionSettings ?: currentDeviceEmotionSettings // Fallback to current if profile somehow null

        scope.launch(Dispatchers.IO) {
            NetworkManager.updateDeviceRequest(
                deviceOn,
                settingsToSend
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
                currentDeviceEmotionSettings // These are the settings from ManualControlScreen
            ) { success, response ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        response ?: if (success) manualSettingsAppliedMessage else deviceUpdateFailedMessage
                    )
                }
            }
        }
    }


    LaunchedEffect(Unit) { // Initial sync
        syncDevice()
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        // photoBitmap state would be needed if ImageControlScreen uses it directly
        // For now, assuming ImageControlScreen handles its own bitmap state if needed for display before send
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                // Pass this bitmap to ImageControlScreen or handle upload directly
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
    val screenProfileManagementTitle = stringResource(R.string.profile_management)

    val collapseNavDesc = stringResource(R.string.collapse_navigation)
    val expandNavDesc = stringResource(R.string.expand_navigation)


    if (isEditingNewProfile || showEditProfileScreenFor != null) {
        EditProfileScreen(
            initialProfile = if (isEditingNewProfile) null else showEditProfileScreenFor,
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

                // If the saved profile is becoming the active one, or was already active
                if (activeProfileId == profileToSave.id || (activeProfileId == null && userProfiles.size == 1) || isCustomSettingsActive || (isEditingNewProfile && userProfiles.size == 1)) {
                    activeProfileId = profileToSave.id
                    sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                    currentDeviceEmotionSettings = profileToSave.emotionSettings
                    isCustomSettingsActive = false // Saving a profile makes it active and not custom
                }


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
                                // If turning device on/off with custom settings, still apply them
                                if (isCustomSettingsActive) {
                                    applyManualSettingsToDevice()
                                } else if (activeProfileId != null) { // Or update with profile if not custom
                                    updateDeviceSettingsWithProfile()
                                }
                            },
                            onUpdateDevice = { // This button on home screen always uses the selected profile
                                if (!isCustomSettingsActive && activeProfileId != null) {
                                    updateDeviceSettingsWithProfile()
                                } else if (isCustomSettingsActive) {
                                    // Optionally, prompt user that this will apply the selected profile, not the custom one,
                                    // or disable this button if custom is active.
                                    // For now, let's assume it applies the *displayed* profile (which would be "Custom" text but underlying actual last profile)
                                    // This needs UX refinement. A safer bet is to make it apply the last *saved* active profile.
                                    val lastActiveSavedProfile = userProfiles.find { it.id == sharedPreferences.getString(ACTIVE_PROFILE_ID_KEY, null) }
                                    if(lastActiveSavedProfile != null) {
                                        currentDeviceEmotionSettings = lastActiveSavedProfile.emotionSettings
                                        activeProfileId = lastActiveSavedProfile.id //
                                        // isCustomSettingsActive should be false here if we are applying a saved profile.
                                        // This interaction is tricky. Let's enforce that "Update Device Settings" on Home applies the *selected saved profile*.
                                        updateDeviceSettingsWithProfile()

                                    } else {
                                        scope.launch{snackbarHostState.showSnackbar(noProfilesToUpdateDeviceMessage)}
                                    }
                                } else {
                                    scope.launch{snackbarHostState.showSnackbar(noProfilesToUpdateDeviceMessage)}
                                }
                            },
                            onSyncDevice = { syncDevice() },
                            profiles = userProfiles,
                            activeProfileId = if (isCustomSettingsActive) CUSTOM_PROFILE_ID_PLACEHOLDER else activeProfileId,
                            onProfileSelected = { profileId ->
                                if (profileId == CUSTOM_PROFILE_ID_PLACEHOLDER) {
                                    isCustomSettingsActive = true // Should not happen if "Custom" is not selectable
                                } else {
                                    activeProfileId = profileId
                                    sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, profileId).apply()
                                    isCustomSettingsActive = false // Selecting a profile makes it not custom
                                    // currentDeviceEmotionSettings updated by LaunchedEffect
                                    updateDeviceSettingsWithProfile() // Auto-update device on profile selection
                                }
                            },
                            isCustomActive = isCustomSettingsActive,
                            customProfileName = customProfileNameText
                        )
                    Screen.Manual ->
                        ManualControlScreen(
                            deviceOn = deviceOn,
                            emotionSettings = currentDeviceEmotionSettings, // Always show the current in-memory settings
                            onEmotionSettingChange = { updatedSettings ->
                                currentDeviceEmotionSettings = updatedSettings
                                isCustomSettingsActive = true // Any change here makes it custom
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
                        onAddProfile = { /* Navigation handled by setting isEditingNewProfile */ },
                        onUpdateProfile = { /* Navigation handled by setting showEditProfileScreenFor */ },
                        onDeleteProfile = { profileToDelete ->
                            val wasDeletedProfileActive = activeProfileId == profileToDelete.id
                            userProfiles = userProfiles.filterNot { it.id == profileToDelete.id }.toMutableList()
                            saveProfilesToPrefs(userProfiles)

                            if (wasDeletedProfileActive || isCustomSettingsActive) { // If custom was active, or deleted was active
                                activeProfileId = userProfiles.firstOrNull()?.id
                                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                                isCustomSettingsActive = false // Deleting a profile leads to a saved profile or no profile
                                // currentDeviceEmotionSettings updated by LaunchedEffect
                                if (activeProfileId != null) {
                                    updateDeviceSettingsWithProfile()
                                } else {
                                    // Handle case where no profiles are left - perhaps set to default and update device
                                    currentDeviceEmotionSettings = defaultDeviceEmotionSettings()
                                    // Potentially update device with defaults if needed
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
                            // When navigating to edit, if custom was active, the settings being edited
                            // are based on 'currentDeviceEmotionSettings'. If a profile is passed,
                            // 'initialProfile.emotionSettings' will be used by EditProfileScreen.
                            // If a new profile is created after being in custom mode,
                            // it could start with 'currentDeviceEmotionSettings'.
                            if (profile == null && isCustomSettingsActive) {
                                // Consider pre-filling new profile with current custom settings
                                // This logic is in EditProfileScreen's remember for currentEmotionSettings.
                                // For safety, ensure EditProfileScreen's initial settings are correct.
                            }
                        }
                    )
                    Screen.Image ->
                        ImageControlScreen(
                            photoBitmap = null,
                            onTakePhoto = { photoLauncher.launch(null) },
                            onUploadPhoto = { uploadLauncher.launch("image/*") },
                            onSendPhoto = { /* TODO: Implement actual photo sending */
                                scope.launch { snackbarHostState.showSnackbar(noPhotoToSendMesssage) }
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