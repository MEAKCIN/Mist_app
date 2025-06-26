package com.example.app2.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.app2.MainActivity
import com.example.app2.R
import com.example.app2.data.EmotionSetting
import com.example.app2.data.Profile
import com.example.app2.network.NetworkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.app2.ACTION_ALARM_TRIGGER"
        const val EXTRA_DEVICE_ON = "EXTRA_DEVICE_ON"
        const val EXTRA_PROFILE_ID_TO_ACTIVATE = "EXTRA_PROFILE_ID_TO_ACTIVATE"
        private const val ALARM_NOTIFICATION_CHANNEL_ID = "mist_app_alarms"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ALARM_TRIGGER) {
            val pendingResult = goAsync()
            val deviceOn = intent.getBooleanExtra(EXTRA_DEVICE_ON, false)
            val profileId = intent.getStringExtra(EXTRA_PROFILE_ID_TO_ACTIVATE)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val profile = getProfile(context, profileId)
                    val settings = profile?.emotionSettings ?: defaultDeviceEmotionSettings()
                    val profileName = profile?.name ?: "Default"
                    val actionText = if (deviceOn) "etkinleştirildi" else "devre dışı bırakıldı"
                    val title = "Alarm: '${profileName}' profili $actionText."

                    // 1. Cihaza AÇMA/KAPAMA komutunu gönder
                    NetworkManager.updateDeviceRequest(
                        deviceOn = deviceOn,
                        emotionSettings = settings,
                        onResponse = { _, _ ->
                            // 2. Komut gönderildikten sonra, cihazdan son durumu senkronize et
                            NetworkManager.getDeviceStatus { syncSuccess, _, deviceStatus ->
                                val message = if (syncSuccess && deviceStatus != null) {
                                    // Senkronizasyon başarılıysa durumu bildir
                                    "Cihaz durumu senkronize edildi. Cihaz şimdi ${if (deviceStatus.deviceOn) "AÇIK" else "KAPALI"}."
                                } else {
                                    // Başarısızsa genel bir mesaj göster
                                    "Komut cihaza gönderildi ancak durum senkronize edilemedi."
                                }
                                // 3. Kullanıcıya pop-up bildirim göster
                                showNotification(context, title, message)
                            }
                        }
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    /**
     * Kullanıcıya durumu bildiren bir pop-up (Android Notification) gösterir.
     */
    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (Oreo) ve sonrası için bildirim kanalı oluşturma zorunluluğu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID,
                "Mist App Alarms",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mist App alarmları için bildirimler"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Bildirime tıklandığında uygulamayı açacak olan Intent
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Bildirimi oluştur
        val builder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Bildirimi göster
        // Her bildirim için benzersiz bir ID kullanmak çakışmaları önler
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * ID'ye göre profili SharedPreferences'tan yükler.
     */
    private fun getProfile(context: Context, profileId: String?): Profile? {
        if (profileId == null) return null
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("user_profiles", null)
        val profiles: List<Profile> = if (json != null) {
            try {
                val type = object : TypeToken<List<Profile>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        return profiles.find { it.id == profileId }
    }

    private fun defaultDeviceEmotionSettings(): List<EmotionSetting> {
        return listOf(
            EmotionSetting("Neutral", 1f, 1f, false),
            EmotionSetting("Happy", 10f, 5f, true),
            EmotionSetting("Angry", 5f, 2f, false),
            EmotionSetting("Sad", 20f, 10f, false)
        )
    }
}