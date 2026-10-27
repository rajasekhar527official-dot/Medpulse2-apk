package com.example.medpulse.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "medpulse_study_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.medpulse.ACTION_START"
        const val ACTION_PAUSE = "com.example.medpulse.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.medpulse.ACTION_RESUME"
        const val ACTION_RESET = "com.example.medpulse.ACTION_RESET"
        const val ACTION_SKIP = "com.example.medpulse.ACTION_SKIP"
        const val ACTION_STOP_SERVICE = "com.example.medpulse.ACTION_STOP_SERVICE"

        fun startService(context: Context, action: String = ACTION_START) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observeJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Observe timer state and refresh persistent notification
        observeJob = serviceScope.launch {
            TimerManager.getInstance(applicationContext).timerStateFlow.collectLatest { state ->
                if (state.status == TimerStatus.RUNNING || state.status == TimerStatus.PAUSED) {
                    val notification = buildNotification(state)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                } else if (state.status == TimerStatus.IDLE) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerManager = TimerManager.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_START -> {
                val state = timerManager.timerStateFlow.value
                val notification = buildNotification(state)
                startAsForeground(notification)
                if (state.status != TimerStatus.RUNNING) {
                    timerManager.startTimer()
                }
            }
            ACTION_PAUSE -> {
                timerManager.pauseTimer()
            }
            ACTION_RESUME -> {
                timerManager.resumeTimer()
            }
            ACTION_RESET -> {
                timerManager.resetTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SKIP -> {
                timerManager.skipSession()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val state = timerManager.timerStateFlow.value
                val notification = buildNotification(state)
                startAsForeground(notification)
            }
        }

        return START_STICKY
    }

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: TimerState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (state.mode) {
            TimerMode.FOCUS -> "MedPulse • Focus Session"
            TimerMode.BREAK -> "MedPulse • Break Time"
        }

        val content = buildString {
            append(state.formattedRemaining)
            append(if (state.status == TimerStatus.PAUSED) " (Paused)" else " remaining")
            if (!state.linkedTopicTitle.isNullOrBlank()) {
                append(" • ")
                append(state.linkedTopicTitle)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(state.status == TimerStatus.RUNNING || state.status == TimerStatus.PAUSED)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)

        // Actions
        if (state.status == TimerStatus.RUNNING) {
            val pauseIntent = Intent(this, TimerForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else if (state.status == TimerStatus.PAUSED) {
            val resumeIntent = Intent(this, TimerForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(
                this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        }

        val skipIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_SKIP
        }
        val skipPending = PendingIntent.getService(
            this, 3, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_media_next, "Skip", skipPending)

        val resetIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_RESET
        }
        val resetPending = PendingIntent.getService(
            this, 4, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", resetPending)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining study time and quick timer controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observeJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
