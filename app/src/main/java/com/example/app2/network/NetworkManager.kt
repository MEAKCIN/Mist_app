package com.example.app2.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.app2.data.DeviceStatus
import com.example.app2.data.EmotionSetting // Import the new data class
import kotlinx.coroutines.Dispatchers
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class NetworkManager {
    companion object {
        fun updateDeviceRequest(
            deviceOn: Boolean,
            emotionSettings: List<EmotionSetting>, // Updated parameter
            onResponse: (Boolean, String?) -> Unit
        ) {
            val emotionsJsonArray = JSONArray()
            emotionSettings.forEach { setting ->
                val emotionJson = JSONObject().apply {
                    put("name", setting.name)
                    put("sprayPeriod", setting.sprayPeriod)
                    put("sprayDuration", setting.sprayDuration)
                    put("isActive", setting.isActive)
                }
                emotionsJsonArray.put(emotionJson)
            }

            val jsonPayload = JSONObject().apply {
                put("deviceOn", deviceOn)
                put("emotions", emotionsJsonArray)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/upload-manual") // Ensure your backend expects this new structure
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
                .url("http://10.0.2.2:5000/device") // Ensure your backend returns the new structure
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
                            val deviceOn = jsonObject.getBoolean("deviceOn")
                            val emotionsJsonArray = jsonObject.getJSONArray("emotions")
                            val emotionSettingsList = mutableListOf<EmotionSetting>()
                            for (i in 0 until emotionsJsonArray.length()) {
                                val emotionJson = emotionsJsonArray.getJSONObject(i)
                                emotionSettingsList.add(
                                    EmotionSetting(
                                        name = emotionJson.getString("name"),
                                        sprayPeriod = emotionJson.getDouble("sprayPeriod").toFloat(),
                                        sprayDuration = emotionJson.getDouble("sprayDuration").toFloat(),
                                        isActive = emotionJson.getBoolean("isActive")
                                    )
                                )
                            }
                            val status = DeviceStatus(
                                emotions = emotionSettingsList,
                                deviceOn = deviceOn
                            )
                            onResponse(true, null, status)
                        } catch (e: Exception) {
                            Log.e("NetworkManager", "Error parsing device status: ${e.message}")
                            onResponse(false, "Error parsing device status: ${e.message}", null)
                        }
                    } ?: onResponse(false, "Empty response", null)
                }
            })
        }
    }
}