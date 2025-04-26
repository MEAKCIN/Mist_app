// Kotlin
package com.example.app2

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun ImageControlScreen(
    photoBitmap: Bitmap?,
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onTakePhoto) {
                Text("Take Photo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onUploadPhoto) {
                Text("Upload Photo")
            }
        }
        photoBitmap?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "User taken photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* send photo action */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Send Photo")
            }
        }
    }
}