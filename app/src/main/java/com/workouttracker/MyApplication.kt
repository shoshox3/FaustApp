package com.faust

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.faust.persistence.database.WorkoutDatabase
import com.faust.util.SessionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
            private set
        const val WORKOUT_NOTIFICATION_CHANNEL = "workout_live_channel"
        const val REMINDER_NOTIFICATION_CHANNEL = "workout_reminder_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        initDeferredLibraries()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Live workout channel
            val liveWorkoutChannel = NotificationChannel(
                WORKOUT_NOTIFICATION_CHANNEL,
                "Live Workout",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing workout progress"
            }

            // Reminder channel
            val reminderChannel = NotificationChannel(
                REMINDER_NOTIFICATION_CHANNEL,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sends workout reminder notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(liveWorkoutChannel, reminderChannel)
            )
        }
    }

    private fun initDeferredLibraries() {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize any heavy SDKs here asynchronously
        }
    }
}
