package com.faust.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.faust.HomeActivity
import com.faust.MyApplication
import com.faust.R
import com.faust.util.Constants
import com.faust.util.toDurationString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

/**
 * Foreground service that shows the live workout notification.
 * Keeps the workout alive when the app is backgrounded.
 */
@AndroidEntryPoint
class LiveWorkoutNotificationService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val EXTRA_WORKOUT_NAME = "workout_name"
        const val EXTRA_ELAPSED_SECONDS = "elapsed_seconds"
        const val EXTRA_TOTAL_SETS = "total_sets"
        const val EXTRA_TOTAL_VOLUME = "total_volume"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var elapsedSeconds = 0L

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val workoutName = intent.getStringExtra(EXTRA_WORKOUT_NAME) ?: "Workout"
                startForeground(
                    Constants.NotificationIds.LIVE_WORKOUT,
                    buildNotification(workoutName, "Starting...")
                )
                startTimer(workoutName)
            }
            ACTION_STOP -> stopSelf()
            ACTION_UPDATE -> {
                val name = intent.getStringExtra(EXTRA_WORKOUT_NAME) ?: "Workout"
                val sets = intent.getIntExtra(EXTRA_TOTAL_SETS, 0)
                val volume = intent.getFloatExtra(EXTRA_TOTAL_VOLUME, 0f)
                updateNotification(name, sets, volume)
            }
        }
        return START_STICKY
    }

    private fun startTimer(workoutName: String) {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
                val notification = buildNotification(
                    workoutName,
                    elapsedSeconds.toDurationString()
                )
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(Constants.NotificationIds.LIVE_WORKOUT, notification)
            }
        }
    }

    private fun buildNotification(workoutName: String, subtitle: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MyApplication.WORKOUT_NOTIFICATION_CHANNEL)
            .setContentTitle(workoutName)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(workoutName: String, sets: Int, volume: Float) {
        val notification = buildNotification(
            workoutName,
            "$sets sets · ${volume.toInt()} kg"
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(Constants.NotificationIds.LIVE_WORKOUT, notification)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
