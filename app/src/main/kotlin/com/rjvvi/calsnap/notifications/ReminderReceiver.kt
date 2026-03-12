package com.rjvvi.calsnap.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, "meal_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CalSnap 🍎")
            .setContentText(intent.getStringExtra("message") ?: "Не забудь записать приём пищи!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
