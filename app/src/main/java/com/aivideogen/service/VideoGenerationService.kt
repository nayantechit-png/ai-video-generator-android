package com.aivideogen.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aivideogen.R
import com.aivideogen.ui.MainActivity
import timber.log.Timber

/**
 * Foreground service that keeps video generation alive
 * even when the app is backgrounded.
 */
class VideoGenerationService : Service() {

    companion object {
        const val CHANNEL_ID       = "video_generation_channel"
        const val NOTIFICATION_ID  = 1001
        const val ACTION_START     = "ACTION_START"
        const val ACTION_STOP      = "ACTION_STOP"
        const val EXTRA_TITLE      = "extra_title"

        fun start(context: Context, title: String = "Generating AI Video…") {
            val intent = Intent(context, VideoGenerationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, VideoGenerationService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Generating AI Video…"
                startForeground(NOTIFICATION_ID, buildNotification(title))
                Timber.d("VideoGenerationService started")
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Timber.d("VideoGenerationService stopped")
            }
        }
        return START_NOT_STICKY
    }

    fun updateProgress(percent: Int, message: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(message, percent))
    }

    private fun buildNotification(
        contentText: String,
        progress: Int = 0
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AI Video Generator")
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_video_placeholder)
        .setOngoing(true)
        .setProgress(100, progress, progress == 0)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while generating AI videos"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
