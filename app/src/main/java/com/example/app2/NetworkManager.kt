// File: app/src/main/java/com/example/app2/NetworkManager.kt
package com.example.app2

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

class NetworkManager {
    companion object {
        fun updateDeviceRequest(
            sprayPeriod: Float,
            sprayDuration: Float,
            deviceOn: Boolean,
            currentEmotion: String,
            onResponse: (Boolean, String?) -> Unit
        ) {
            val json = """
                {
                    "sprayPeriod": $sprayPeriod,
                    "sprayDuration": $sprayDuration,
                    "deviceOn": $deviceOn,
                    "currentEmotion": "$currentEmotion"
                }
            """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/upload_try")
                .post(requestBody)
                .build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("NetworkManager", "Request failed: ${e.message}")
                    onResponse(false, e.message)
                }
                override fun onResponse(call: Call, response: Response) {
                    onResponse(response.isSuccessful, response.body?.string())
                }
            })
        }

        fun sendPhotoRequest(
            bitmap: Bitmap,
            onResponse: (Boolean, String?) -> Unit
        ) {
            // Convert the bitmap to a JPEG byte array.
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()

            // Use Base64.NO_WRAP to avoid line breaks.
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            // Build JSON payload.
            val json = """
                {
                    "photo": "$base64String"
                }
            """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/upload")
                .post(requestBody)
                .build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onResponse(false, e.message)
                }
                override fun onResponse(call: Call, response: Response) {
                    onResponse(response.isSuccessful, response.body?.string())
                }
            })
        }
    }
}