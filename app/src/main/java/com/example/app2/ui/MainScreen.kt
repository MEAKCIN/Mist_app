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

    val defaultProfileNameText = stringResource(id = R.string.default_profile_name)

    // Initialize default profile if none exist
    LaunchedEffect(Unit) {
        if (userProfiles.isEmpty()) {
            val defaultProfile = Profile(name = defaultProfileNameText, emotionSettings = defaultDeviceEmotionSettings())
            userProfiles = mutableListOf(defaultProfile)
            saveProfilesToPrefs(userProfiles)
            if (activeProfileId == null) {
                activeProfileId = defaultProfile.id
                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
            }
        } else if (activeProfileId == null && userProfiles.isNotEmpty()) {
            // If activeProfileId is null but profiles exist, set the first one as active
            activeProfileId = userProfiles.first().id
            sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
        }
    }


    fun getActiveProfile(): Profile? {
        return userProfiles.find { it.id == activeProfileId }
    }

    var currentDeviceEmotionSettings by remember {
        mutableStateOf(getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings())
    }
    var deviceOn by remember { mutableStateOf(true) }

    LaunchedEffect(activeProfileId, userProfiles) {
        val newActiveProfile = getActiveProfile()
        currentDeviceEmotionSettings = newActiveProfile?.emotionSettings ?: defaultDeviceEmotionSettings()
        // Optionally, you might want to automatically update the device when the profile changes.
        // For now, this is handled by the "Update Device Settings" button in HomeScreen.
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


    fun syncDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.getDeviceStatus { success, response, status ->
                scope.launch {
                    if (success && status != null) {
                        deviceOn = status.deviceOn
                        // Decide how to handle incoming emotion settings vs local profiles
                        // For now, we primarily manage emotion settings via local profiles
                        // currentDeviceEmotionSettings = if (status.emotions.isNotEmpty()) status.emotions else getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings()
                        snackbarHostState.showSnackbar(syncedSuccessfullyMessage)
                    } else {
                        snackbarHostState.showSnackbar(response ?: syncFailedDefaultMessage)
                    }
                }
            }
        }
    }

    fun updateDeviceSettings() {
        if (activeProfileId == null && userProfiles.isNotEmpty()) {
            // If no active profile somehow, try to set one
            activeProfileId = userProfiles.first().id
            sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
            currentDeviceEmotionSettings = getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings()
        } else if (userProfiles.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("No profiles available to update device.")} // Or a localized string
            return
        }


        scope.launch(Dispatchers.IO) {
            NetworkManager.updateDeviceRequest(
                deviceOn,
                currentDeviceEmotionSettings
            ) { success, response ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        response ?: if (success) deviceSettingsUpdatedMessage else deviceUpdateFailedMessage
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

                if (activeProfileId == profileToSave.id || (activeProfileId == null && userProfiles.size == 1)) {
                    activeProfileId = profileToSave.id // Ensure active ID is set if it was the one edited or the only one
                    sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                    currentDeviceEmotionSettings = profileToSave.emotionSettings
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
                            onDeviceOnSwitchChange = { deviceOn = it },
                            onUpdateDevice = { updateDeviceSettings() },
                            onSyncDevice = { syncDevice() },
                            profiles = userProfiles,
                            activeProfileId = activeProfileId,
                            onProfileSelected = { profileId ->
                                activeProfileId = profileId
                                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, profileId).apply()
                                // Update currentDeviceEmotionSettings immediately
                                currentDeviceEmotionSettings = getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings()
                                // Optionally, auto-update device on profile selection:
                                updateDeviceSettings()
                            }
                        )
                    Screen.Manual -> // This screen's role might need re-evaluation.
                        // It could be a "live test" for currentDeviceEmotionSettings.
                        ManualControlScreen(
                            deviceOn = deviceOn,
                            emotionSettings = currentDeviceEmotionSettings,
                            onEmotionSettingChange = { updatedSettings ->
                                // These changes are temporary and not saved to the profile here.
                                currentDeviceEmotionSettings = updatedSettings
                            },
                            showDisabledMessage = {
                                scope.launch { snackbarHostState.showSnackbar(deviceOffActionDisabledMessage) }
                            }
                        )
                    Screen.ProfileManagement -> ProfileManagementScreen(
                        profiles = userProfiles,
                        onAddProfile = { /* Navigation handled by setting isEditingNewProfile */ },
                        onUpdateProfile = { /* Navigation handled by setting showEditProfileScreenFor */ },
                        onDeleteProfile = { profileToDelete ->
                            userProfiles = userProfiles.filterNot { it.id == profileToDelete.id }.toMutableList()
                            saveProfilesToPrefs(userProfiles)
                            if (activeProfileId == profileToDelete.id) {
                                activeProfileId = userProfiles.firstOrNull()?.id
                                sharedPreferences.edit().putString(ACTIVE_PROFILE_ID_KEY, activeProfileId).apply()
                                currentDeviceEmotionSettings = getActiveProfile()?.emotionSettings ?: defaultDeviceEmotionSettings()
                                if (activeProfileId != null) { // If a new profile became active, update device
                                    updateDeviceSettings()
                                }
                            }
                            scope.launch { snackbarHostState.showSnackbar(profileDeletedMessage) }
                        },
                        onNavigateToEditProfile = { profile ->
                            if (profile == null) {
                                isEditingNewProfile = true
                                showEditProfileScreenFor = null // Ensure this is null for new profile
                            } else {
                                isEditingNewProfile = false
                                showEditProfileScreenFor = profile
                            }
                        }
                    )
                    Screen.Image ->
                        ImageControlScreen(
                            // Pass necessary states and callbacks for ImageControlScreen
                            // For example, if it needs to display the photo:
                            photoBitmap = null, // This needs to be managed if ImageControlScreen displays it
                            onTakePhoto = { photoLauncher.launch(null) },
                            onUploadPhoto = { uploadLauncher.launch("image/*") },
                            onSendPhoto = { /* bitmap -> ... NetworkManager.sendPhotoRequest(bitmap) ... */ }
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
                        .width(220.dp) // Slightly wider for profile names
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
                                    Screen.Image.route -> painterResource(id = R.drawable.image_upload) // Check this
                                    Screen.Settings.route -> painterResource(id = R.drawable.settings_icon) // Check this
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

