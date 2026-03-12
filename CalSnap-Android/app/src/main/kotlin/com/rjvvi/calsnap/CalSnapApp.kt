package com.rjvvi.calsnap

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.rjvvi.calsnap.data.api.GeminiRepository
import com.rjvvi.calsnap.data.db.AppDatabase
import com.rjvvi.calsnap.data.prefs.UserPrefs

class CalSnapApp : Application() {

    val db by lazy { AppDatabase.get(this) }
    val prefs by lazy { UserPrefs(this) }
    val gemini by lazy { GeminiRepository() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    "meal_reminders",
                    "Напоминания о еде",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Напоминания добавить приём пищи" }
            )
        }
    }
}
