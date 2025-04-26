
package com.example.app2
import com.example.app2.HomeScreen
import com.example.app2.ImageControlScreen
import com.example.app2.ManualControlScreen

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.app2.ui.theme.App2Theme
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen() {
    var selectedTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var deviceOn by remember { mutableStateOf(true) }
    var currentEmotion by remember { mutableStateOf("Neutral") }
    var sprayPeriod by remember { mutableStateOf(1f) }
    var sprayDuration by remember { mutableStateOf(1f) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        photoBitmap = bitmap
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Home, Screen.Manual, Screen.Image).forEach { screen ->
                    NavigationBarItem(
                        selected = selectedTab == screen,
                        onClick = { selectedTab = screen },
                        icon = {
                            when (screen) {
                                Screen.Home -> {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = screen.title
                                    )
                                }
                                Screen.Manual -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.manual_control_icon),
                                        contentDescription = screen.title
                                    )
                                }
                                Screen.Image -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.image_upload),
                                        contentDescription = screen.title
                                    )
                                }
                            }
                        },
                        label = {}
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Screen.Home -> {
                    HomeScreen(
                        deviceOn = deviceOn,
                        currentEmotion = if (deviceOn) currentEmotion else "Device Turned Off",
                        sprayPeriod = sprayPeriod,
                        sprayDuration = sprayDuration,
                        onSwitchChange = { deviceOn = it },
                        onPeriodChange = { sprayPeriod = it },
                        onDurationChange = { sprayDuration = it }
                    )
                }
                Screen.Manual -> {
                    ManualControlScreen(
                        deviceOn = deviceOn,
                        currentEmotion = currentEmotion,
                        onEmotionChange = { newEmotion -> currentEmotion = newEmotion },
                        showDisabledMessage = {
                            scope.launch { snackbarHostState.showSnackbar("Turn on the device first") }
                        }
                    )
                }
                Screen.Image -> {
                    ImageControlScreen(
                        photoBitmap = photoBitmap,
                        onTakePhoto = { photoLauncher.launch(null) },
                        onUploadPhoto = { uploadLauncher.launch("image/*") }
                    )
                }
            }
        }
    }
}
