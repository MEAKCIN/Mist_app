package com.example.app2.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R
import com.example.app2.data.EmotionSetting
import com.example.app2.data.Profile

// Bu ekranda profilleri listelemeli, yeni profil ekleme ve düzenleme için
// dialoglar veya ayrı ekranlar açılmalı.
// Düzenleme kısmı ManualControlScreen'e çok benzeyecektir.

@Composable
fun ProfileManagementScreen(
    profiles: List<Profile>,
    onAddProfile: (Profile) -> Unit,
    onUpdateProfile: (Profile) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onNavigateToEditProfile: (Profile?) -> Unit // Profile null ise yeni profil
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) } // Düzenlenecek veya yeni oluşturulacak profil
    // Gerçek uygulamada yeni profil oluşturma/düzenleme için ayrı bir composable/ekran daha iyi olur.

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEditProfile(null) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_profile))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(profiles) { profile ->
                ProfileRow(
                    profile = profile,
                    onEdit = { onNavigateToEditProfile(profile) },
                    onDelete = { onDeleteProfile(profile) }
                )
                Divider()
            }
        }
    }

    // Yeni profil / Düzenleme için bir Dialog veya ayrı bir ekran açılabilir.
    // Bu kısım için daha detaylı bir yapıya ihtiyaç var.
    // Şimdilik sadece yönlendirme fonksiyonunu (onNavigateToEditProfile) çağırıyoruz.
}

@Composable
fun ProfileRow(profile: Profile, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(profile.name, style = MaterialTheme.typography.bodyLarge)
        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_profile))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_profile))
            }
        }
    }
}

