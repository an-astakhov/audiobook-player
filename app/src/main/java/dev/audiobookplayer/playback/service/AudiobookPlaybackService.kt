package dev.audiobookplayer.playback.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.audiobookplayer.AudiobookPlayerApplication
import dev.audiobookplayer.R

class AudiobookPlaybackService : MediaSessionService() {
    private var sessionRegistered = false

    private val appContainer by lazy {
        (application as AudiobookPlayerApplication).appContainer
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build(),
        )
        addSession(appContainer.playbackRuntime.mediaSession)
        sessionRegistered = true
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return appContainer.playbackRuntime.mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!appContainer.playbackRuntime.isPlaybackOngoing()) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (sessionRegistered) {
            removeSession(appContainer.playbackRuntime.mediaSession)
            sessionRegistered = false
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.playback_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.playback_notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.playback_notification_title))
        .setContentText(getString(R.string.playback_notification_preparing))
        .setSmallIcon(R.mipmap.ic_launcher)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .build()

    private companion object {
        const val CHANNEL_ID = "playback"
        const val NOTIFICATION_ID = 1001
    }
}
