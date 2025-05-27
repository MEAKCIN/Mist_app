package com.example.app2.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R

@Composable
fun ImageControlScreen(
    photoBitmap: Bitmap?, // Receive the bitmap to display
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onSendPhoto: () -> Unit  // Callback to trigger sending
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(stringResource(R.string.take_photo))
            }
            Button(onClick = onUploadPhoto, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(stringResource(R.string.upload_photo))
            }
        }

        // Display the photo if available
        photoBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.user_taken_photo),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(300.dp),
                contentScale = ContentScale.Fit // Or ContentScale.Crop, depending on desired look
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSendPhoto, // Use the passed callback
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = true // Button is always enabled if a photo is displayed
            ) {
                Text(stringResource(R.string.send_photo))
            }
        }
    }
}