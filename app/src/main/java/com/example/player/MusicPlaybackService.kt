package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            mediaSession = MediaSessionCompat(this, "FlowMusicSession").apply {
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        MusicPlayerManager.instance?.togglePlayPause()
                    }
                    override fun onPause() {
                        MusicPlayerManager.instance?.togglePlayPause()
                    }
                    override fun onSkipToNext() {
                        MusicPlayerManager.instance?.playNext()
                    }
                    override fun onSkipToPrevious() {
                        MusicPlayerManager.instance?.playPrevious()
                    }
                    override fun onSeekTo(pos: Long) {
                        MusicPlayerManager.instance?.seekTo(pos.toInt())
                    }
                })
                isActive = true
            }
        } catch (e: Exception) {
            Log.e("MusicPlaybackService", "Error initializing MediaSession: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                MusicPlayerManager.instance?.togglePlayPause()
            }
            ACTION_NEXT -> {
                MusicPlayerManager.instance?.playNext()
            }
            ACTION_PREVIOUS -> {
                MusicPlayerManager.instance?.playPrevious()
            }
            ACTION_STOP -> {
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } catch (e: Exception) {
                    Log.e("MusicPlaybackService", "Error stopping foreground: ${e.message}", e)
                }
            }
            ACTION_UPDATE -> {
                val song = MusicPlayerManager.instance?.currentSong?.value
                val isPlaying = MusicPlayerManager.instance?.isPlaying?.value ?: false
                if (song != null) {
                    updateNotification(song, isPlaying)
                } else {
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } catch (e: Exception) {
                        Log.e("MusicPlaybackService", "Error stopping foreground: ${e.message}", e)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(song: Song, isPlaying: Boolean) {
        try {
            val notification = buildNotification(song, isPlaying, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Load artwork asynchronously for the notification large icon
            if (song.artwork.isNotBlank()) {
                scope.launch {
                    val bitmap = loadArtworkBitmap(song.artwork)
                    if (bitmap != null) {
                        try {
                            val updatedNotification = buildNotification(song, isPlaying, bitmap)
                            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.notify(NOTIFICATION_ID, updatedNotification)
                        } catch (e: Exception) {
                            Log.e("MusicPlaybackService", "Error notifying updated artwork: ${e.message}", e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MusicPlaybackService", "Error in updateNotification: ${e.message}", e)
        }
    }

    private fun buildNotification(song: Song, isPlaying: Boolean, artwork: Bitmap?): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(
            this, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            this, 2, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(if (song.album.isNotBlank()) song.album else "Flow Music")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession?.sessionToken)
            )

        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }

        return builder.build()
    }

    private suspend fun loadArtworkBitmap(url: String): Bitmap? {
        return try {
            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flow Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media controls for playing music"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "flow_music_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.ACTION_UPDATE"

        fun startOrUpdate(context: Context) {
            try {
                val intent = Intent(context, MusicPlaybackService::class.java).apply {
                    action = ACTION_UPDATE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e("MusicPlaybackService", "Failed to startOrUpdate foreground service: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MusicPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Throwable) {
                Log.e("MusicPlaybackService", "Failed to stop service: ${e.message}", e)
            }
        }
    }
}
