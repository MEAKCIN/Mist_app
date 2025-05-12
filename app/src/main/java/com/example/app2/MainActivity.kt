// File: src/main/java/com/example/app2/MainActivity.kt
package com.example.app2

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import com.example.app2.ui.theme.App2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen(val title: String) {
    object Home : Screen("Home")
    object Manual : Screen("Manual Control")
    object Image : Screen("Image Control")
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App2Theme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var deviceOn by remember { mutableStateOf(true) }
    var currentEmotion by remember { mutableStateOf("Neutral") }
    var sprayPeriod by remember { mutableStateOf(1f) }
    var sprayDuration by remember { mutableStateOf(1f) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var navExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Sync device status on startup
    LaunchedEffect(Unit) {
        NetworkManager.getDeviceStatus { success, response, status ->
            if (success && status != null) {
                sprayPeriod = status.sprayPeriod
                sprayDuration = status.sprayDuration
                deviceOn = status.deviceOn
                currentEmotion = status.currentEmotion
                scope.launch { snackbarHostState.showSnackbar("Synced successfully") }
            } else {
                scope.launch { snackbarHostState.showSnackbar(response ?: "Sync failed") }
            }
        }
    }

    // Function for manual sync
    fun syncDevice() {
        scope.launch(Dispatchers.IO) {
            NetworkManager.getDeviceStatus { success, response, status ->
                scope.launch {
                    if (success && status != null) {
                        sprayPeriod = status.sprayPeriod
                        sprayDuration = status.sprayDuration
                        deviceOn = status.deviceOn
                        currentEmotion = status.currentEmotion
                        snackbarHostState.showSnackbar("Synced successfully")
                    } else {
                        snackbarHostState.showSnackbar(response ?: "Sync failed")
                    }
                }
            }
        }
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
                println("Error uploading image: ${e.message}")
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
                    Screen.Home -> HomeScreen(
                        deviceOn = deviceOn,
                        currentEmotion = if (deviceOn) currentEmotion else "Device Off",
                        sprayPeriod = sprayPeriod,
                        sprayDuration = sprayDuration,
                        onSwitchChange = { deviceOn = it },
                        onPeriodChange = { sprayPeriod = it },
                        onDurationChange = { sprayDuration = it },
                        onUpdateDevice = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    NetworkManager.updateDeviceRequest(
                                        sprayPeriod,
                                        sprayDuration,
                                        deviceOn,
                                        currentEmotion
                                    ) { success, response ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                response ?: if (success) "Device updated" else "Update failed"
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Network error: ${e.localizedMessage}")
                                    }
                                    println("Error updating device: ${e.message}")
                                }
                            }
                        },
                        onSyncDevice = { syncDevice() }
                    )
                    Screen.Manual -> ManualControlScreen(
                        deviceOn = deviceOn,
                        currentEmotion = currentEmotion,
                        onEmotionChange = { currentEmotion = it },
                        showDisabledMessage = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Turn on the device first")
                            }
                        },
                        onUpdateDevice = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    NetworkManager.updateDeviceRequest(
                                        sprayPeriod,
                                        sprayDuration,
                                        deviceOn,
                                        currentEmotion
                                    ) { success, response ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                response ?: if (success) "Device updated" else "Update failed"
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Network error: ${e.localizedMessage}")
                                    }
                                    println("Error updating manual control: ${e.message}")
                                }
                            }
                        }
                    )
                    Screen.Image -> ImageControlScreen(
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
                                            if (success) {
                                                syncDevice()
                                            }
                                        }
                                    }
                                }
                            } ?: run {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No photo to send")
                                }
                            }
                        }
                    )
                }
            }
        }

        if (navExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            navExpanded = false
                        })
                    }
            )

            NavigationRail(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                header = {
                    IconButton(onClick = {
                        navExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Collapse Navigation",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            ) {
                listOf(Screen.Home, Screen.Manual, Screen.Image).forEach { screen ->
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
                            }
                            Icon(
                                painter = iconPainter,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp),
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
                onClick = {
                    navExpanded = true
                },
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