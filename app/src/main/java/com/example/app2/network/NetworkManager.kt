package com.example.app2.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.app2.data.DeviceStatus
import kotlinx.coroutines.Dispatchers
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
                .url("http://10.0.2.2:5000/upload-manual")
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
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val json = """
                {
                    "photo": "$base64String"
                }
            """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/upload-photo")
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

        fun getDeviceStatus(
            onResponse: (Boolean, String?, DeviceStatus?) -> Unit
        ) {
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/device")
                .build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("NetworkManager", "GET failed: ${e.message}")
                    onResponse(false, e.message, null)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.body?.string()?.let { bodyString ->
                        try {
                            val jsonObject = JSONObject(bodyString)
                            val status = DeviceStatus(
                                sprayPeriod = jsonObject.getDouble("sprayPeriod").toFloat(),
                                sprayDuration = jsonObject.getDouble("sprayDuration").toFloat(),
                                deviceOn = jsonObject.getBoolean("deviceOn"),
                                currentEmotion = jsonObject.getString("currentEmotion")
                            )
                            onResponse(true, null, status)
                        } catch (e: Exception) {
                            onResponse(false, e.message, null)
                        }
                    } ?: onResponse(false, "Empty response", null)
                }
            })
        }
    }
}