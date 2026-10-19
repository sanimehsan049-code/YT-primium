package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.MainActivity
import com.example.R

class BackgroundMediaService : Service() {

    private val binder = LocalBinder()
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPlaying: Boolean = false
    private var currentTitle: String = "YouTube Video"
    private var currentArtist: String = "YouTube"

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundMediaService = this@BackgroundMediaService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BraveTube:BackgroundPlaybackWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L) // 12 hours max
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background video and audio playback"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "BraveTubeMediaSession").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    mediaListener?.onMediaPlay()
                }

                override fun onPause() {
                    mediaListener?.onMediaPause()
                }

                override fun onFastForward() {
                    mediaListener?.onMediaSeekRelative(10)
                }

                override fun onRewind() {
                    mediaListener?.onMediaSeekRelative(-10)
                }

                override fun onStop() {
                    mediaListener?.onMediaStop()
                    stopForegroundPlayback()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> mediaListener?.onMediaPlay()
            ACTION_PAUSE -> mediaListener?.onMediaPause()
            ACTION_FORWARD -> mediaListener?.onMediaSeekRelative(10)
            ACTION_REWIND -> mediaListener?.onMediaSeekRelative(-10)
            ACTION_STOP -> {
                mediaListener?.onMediaStop()
                stopForegroundPlayback()
            }
        }
        return START_NOT_STICKY
    }

    fun updatePlaybackState(playing: Boolean, title: String, artist: String) {
        this.isPlaying = playing
        this.currentTitle = title.ifBlank { "YouTube Video" }
        this.currentArtist = artist.ifBlank { "YouTube" }

        // Update MediaSession
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "BraveTube")
            .build()
        mediaSession?.setMetadata(metadata)

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun stopForegroundPlayback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewindIntent = Intent(this, BackgroundMediaService::class.java).apply { action = ACTION_REWIND }
        val rewindPendingIntent = PendingIntent.getService(this, 1, rewindIntent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIntent = Intent(this, BackgroundMediaService::class.java).apply { action = playPauseAction }
        val playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val forwardIntent = Intent(this, BackgroundMediaService::class.java).apply { action = ACTION_FORWARD }
        val forwardPendingIntent = PendingIntent.getService(this, 3, forwardIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, BackgroundMediaService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder.setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText("Brave Shields Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_rew, "Rewind 10s", rewindPendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_ff, "Forward 10s", forwardPendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent
                ).build()
            )

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }
        builder.style = mediaStyle

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    interface MediaControlListener {
        fun onMediaPlay()
        fun onMediaPause()
        fun onMediaSeekRelative(seconds: Int)
        fun onMediaStop()
    }

    companion object {
        const val CHANNEL_ID = "brave_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.service.PLAY"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_FORWARD = "com.example.service.FORWARD"
        const val ACTION_REWIND = "com.example.service.REWIND"
        const val ACTION_STOP = "com.example.service.STOP"

        var mediaListener: MediaControlListener? = null
    }
}
