package com.example.app2.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding // Added padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app2.R

@Composable
fun ImageControlScreen(
    photoBitmap: Bitmap?,
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onSendPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // Added padding for consistency
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // Center align content
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between buttons
        ) {
            Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth(0.8f)) { // Make buttons a bit wider
                Text(stringResource(R.string.take_photo))
            }
            Button(onClick = onUploadPhoto, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(stringResource(R.string.upload_photo))
            }
        }
        photoBitmap?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.user_taken_photo), // Localized content description
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Adjust image width
                    .height(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSendPhoto,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.send_photo))
            }
        }
    }
}