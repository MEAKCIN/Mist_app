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
    object Home : Screen("Ana Sayfa")
    object Manual : Screen("Manuel Kontrol")
    object Image : Screen("Görüntü Kontrolü")
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
    var currentEmotion by remember { mutableStateOf("Nötr") }
    var sprayPeriod by remember { mutableStateOf(1f) }
    var sprayDuration by remember { mutableStateOf(1f) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var navExpanded by remember { mutableStateOf(false) }
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
            try {
                photoBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Resim yüklenirken hata: ${e.localizedMessage}")
                }
                println("Resim yüklenirken hata: ${e.message}")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize() // Scaffold content alanı kadar yer kapla
                    .padding(innerPadding) // Scaffold'un kendi iç boşluklarını (FAB, BottomBar vb. için) uygula
                    .padding(top = if (!navExpanded) 72.dp else 0.dp) // Navigasyon kapalıyken menü butonu için ek üst boşluk
            ) {
                when (selectedTab) {
                    Screen.Home -> HomeScreen(
                        deviceOn = deviceOn,
                        currentEmotion = if (deviceOn) currentEmotion else "Cihaz Kapalı",
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
                                            snackbarHostState.showSnackbar(response ?: if (success) "Cihaz güncellendi" else "Güncelleme başarısız")
                                        }
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Ağ hatası: ${e.localizedMessage}")
                                    }
                                    println("AnaEkran güncellemesinde ağ hatası: ${e.message}")
                                }
                            }
                        }
                    )
                    Screen.Manual -> ManualControlScreen(
                        deviceOn = deviceOn,
                        currentEmotion = currentEmotion,
                        onEmotionChange = { currentEmotion = it },
                        showDisabledMessage = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Önce cihazı açın")
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
                                            snackbarHostState.showSnackbar(response ?: if (success) "Cihaz güncellendi" else "Güncelleme başarısız")
                                        }
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Ağ hatası: ${e.localizedMessage}")
                                    }
                                    println("ManuelKontrol güncellemesinde ağ hatası: ${e.message}")
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
                                            snackbarHostState.showSnackbar(response ?: (if (success) "Fotoğraf başarıyla gönderildi" else "Fotoğraf gönderme başarısız"))
                                        }
                                    }
                                }
                            } ?: run {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Gönderilecek fotoğraf yok")
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
                            println("Scrim tıklandı. navExpanded false olarak ayarlanıyor.")
                            navExpanded = false
                        })
                    }
            )

            NavigationRail(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                header = {
                    IconButton(onClick = {
                        println("NavigationRail header içindeki menü düğmesi tıklandı. navExpanded false olarak ayarlanıyor.")
                        navExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Navigasyonu Daralt",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            ) {
                listOf(Screen.Home, Screen.Manual, Screen.Image).forEach { screen ->
                    NavigationRailItem(
                        selected = selectedTab == screen,
                        onClick = {
                            println("${screen.title} tıklandı. navExpanded false olarak ayarlanıyor.")
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
                    println("Navigasyonu Aç düğmesi tıklandı. Önceki navExpanded: $navExpanded")
                    navExpanded = true
                    println("Navigasyonu Aç düğmesi tıklandı. Yeni navExpanded: $navExpanded")
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, start = 16.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Navigasyonu Aç",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
