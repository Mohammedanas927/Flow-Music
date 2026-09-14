package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

class MusicPlayerManager(
    private val context: Context,
    private val onSongStarted: (Song) -> Unit = {}
) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // State flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _originalPlaylist = mutableListOf<Song>()
    private var currentPlaylistIndex = -1

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Fallback tracking
    private var currentFallbackIndex = 0
    private var currentAttemptSong: Song? = null

    init {
        instance = this
    }

    private fun resetPlayer() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
        } catch (e: Throwable) {
            // Ignore
        }
        try {
            mediaPlayer?.reset()
        } catch (e: Throwable) {
            // Ignore
        }
        try {
            mediaPlayer?.release()
        } catch (e: Throwable) {
            // Ignore
        }
        mediaPlayer = null
    }

    private fun createMediaPlayer(song: Song): MediaPlayer {
        resetPlayer()
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            } catch (e: Throwable) {
                // Ignore wake lock issues if any
            }
            setOnPreparedListener { preparedMp ->
                try {
                    _isBuffering.value = false
                    val dur = preparedMp.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                    val vol = if (_isMuted.value) 0f else _volume.value
                    preparedMp.setVolume(vol, vol)
                    preparedMp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                    MusicPlaybackService.startOrUpdate(context)
                    Log.d("MusicPlayerManager", "Playback started successfully for: ${song.title}")
                } catch (e: Throwable) {
                    Log.e("MusicPlayerManager", "Error in onPrepared: ${e.message}", e)
                    tryNextFallbackOrError(song)
                }
            }
            setOnCompletionListener {
                handleSongCompletion()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MusicPlayerManager", "MediaPlayer error: what=$what, extra=$extra for song: ${song.title}")
                tryNextFallbackOrError(song)
                true
            }
            setOnBufferingUpdateListener { _, _ ->
                // Buffering updates
            }
        }
        mediaPlayer = mp
        return mp
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition
                        val dur = mp.duration
                        if (dur > 0 && _durationMs.value != dur) {
                            _durationMs.value = dur
                        }
                    }
                } catch (e: Throwable) {
                    // Safe catch to avoid any IllegalStateException crashing the coroutine
                }
                delay(250)
            }
        }
    }

    fun playSong(song: Song, playlistContext: List<Song> = emptyList()) {
        if (playlistContext.isNotEmpty()) {
            _originalPlaylist.clear()
            _originalPlaylist.addAll(playlistContext)
            val idx = _originalPlaylist.indexOfFirst { it.id == song.id }
            currentPlaylistIndex = if (idx >= 0) idx else 0
        } else if (_originalPlaylist.isEmpty()) {
            _originalPlaylist.clear()
            _originalPlaylist.add(song)
            currentPlaylistIndex = 0
        }

        _currentSong.value = song
        _currentPositionMs.value = 0
        _durationMs.value = if (song.duration > 0) song.duration * 1000 else 0
        _isBuffering.value = true
        _isPlaying.value = false
        currentAttemptSong = song
        currentFallbackIndex = 0

        try {
            onSongStarted(song)
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "Error in onSongStarted: ${e.message}", e)
        }

        startAudioPlayback(song, song.audioUrl)
    }

    private fun startAudioPlayback(song: Song, url: String) {
        if (url.isBlank()) {
            Log.w("MusicPlayerManager", "Audio URL is blank, trying fallback")
            tryNextFallbackOrError(song)
            return
        }

        Log.d("MusicPlayerManager", "Preparing audio playback for '${song.title}' with URL: $url")
        _isBuffering.value = true

        try {
            val mp = createMediaPlayer(song)
            val uri = Uri.parse(url)
            var dataSourceSet = false

            // Try setting headers first
            try {
                val headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    "Accept" to "*/*"
                )
                mp.setDataSource(context, uri, headers)
                dataSourceSet = true
            } catch (e: Throwable) {
                Log.w("MusicPlayerManager", "setDataSource with headers failed: ${e.message}, retrying direct string")
            }

            if (!dataSourceSet) {
                try {
                    mp.setDataSource(url)
                    dataSourceSet = true
                } catch (e: Throwable) {
                    Log.e("MusicPlayerManager", "Direct setDataSource failed: ${e.message}", e)
                }
            }

            if (dataSourceSet) {
                mp.prepareAsync()
            } else {
                tryNextFallbackOrError(song)
            }
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "Failed to start audio playback: ${e.message}", e)
            tryNextFallbackOrError(song)
        }
    }

    private fun tryNextFallbackOrError(song: Song) {
        if (currentFallbackIndex < song.fallbackUrls.size) {
            val nextUrl = song.fallbackUrls[currentFallbackIndex]
            currentFallbackIndex++
            Log.d("MusicPlayerManager", "Retrying song '${song.title}' with fallback url #$currentFallbackIndex: $nextUrl")
            startAudioPlayback(song, nextUrl)
        } else {
            Log.e("MusicPlayerManager", "All available audio URLs failed for '${song.title}'")
            _isBuffering.value = false
            _isPlaying.value = false
            try {
                MusicPlaybackService.startOrUpdate(context)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp == null) {
            _currentSong.value?.let { playSong(it) }
            return
        }

        if (_isPlaying.value) {
            try {
                mp.pause()
                _isPlaying.value = false
                MusicPlaybackService.startOrUpdate(context)
            } catch (e: Throwable) {
                Log.e("MusicPlayerManager", "Error pausing playback: ${e.message}", e)
            }
        } else {
            try {
                mp.start()
                _isPlaying.value = true
                startProgressTracker()
                MusicPlaybackService.startOrUpdate(context)
            } catch (e: Throwable) {
                Log.e("MusicPlayerManager", "Error resuming playback: ${e.message}", e)
                _currentSong.value?.let { playSong(it) }
            }
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "Error seeking: ${e.message}", e)
        }
    }

    fun playNext() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val nextSong = q.first()
            _queue.value = q.drop(1)
            playSong(nextSong)
            return
        }

        if (_originalPlaylist.isNotEmpty()) {
            if (_isShuffle.value) {
                val nextIndex = (0 until _originalPlaylist.size).random()
                currentPlaylistIndex = nextIndex
                playSong(_originalPlaylist[nextIndex], _originalPlaylist)
            } else {
                val nextIndex = currentPlaylistIndex + 1
                if (nextIndex < _originalPlaylist.size) {
                    currentPlaylistIndex = nextIndex
                    playSong(_originalPlaylist[nextIndex], _originalPlaylist)
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    currentPlaylistIndex = 0
                    playSong(_originalPlaylist[0], _originalPlaylist)
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    MusicPlaybackService.startOrUpdate(context)
                }
            }
        }
    }

    fun playPrevious() {
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        if (_originalPlaylist.isNotEmpty()) {
            val prevIndex = currentPlaylistIndex - 1
            if (prevIndex >= 0) {
                currentPlaylistIndex = prevIndex
                playSong(_originalPlaylist[prevIndex], _originalPlaylist)
            } else if (_repeatMode.value == RepeatMode.ALL) {
                currentPlaylistIndex = _originalPlaylist.size - 1
                playSong(_originalPlaylist[currentPlaylistIndex], _originalPlaylist)
            } else {
                seekTo(0)
            }
        }
    }

    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                try {
                    mediaPlayer?.start()
                    _isPlaying.value = true
                    MusicPlaybackService.startOrUpdate(context)
                } catch (e: Throwable) {
                    _currentSong.value?.let { playSong(it) }
                }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                if (q.isNotEmpty() || (_originalPlaylist.isNotEmpty() && currentPlaylistIndex + 1 < _originalPlaylist.size)) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    MusicPlaybackService.startOrUpdate(context)
                }
            }
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun addToQueue(song: Song) {
        val current = _queue.value.toMutableList()
        current.add(song)
        _queue.value = current
    }

    fun playNextInQueue(song: Song) {
        val current = _queue.value.toMutableList()
        current.add(0, song)
        _queue.value = current
    }

    fun removeFromQueue(index: Int) {
        val current = _queue.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _queue.value = current
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        _isMuted.value = clamped == 0f
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (e: Throwable) {
            // Ignore
        }
    }

    fun toggleMute() {
        if (_isMuted.value) {
            _isMuted.value = false
            val vol = if (_volume.value > 0f) _volume.value else 0.8f
            try {
                mediaPlayer?.setVolume(vol, vol)
            } catch (e: Throwable) {
                // Ignore
            }
        } else {
            _isMuted.value = true
            try {
                mediaPlayer?.setVolume(0f, 0f)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    fun releasePlayer() {
        resetPlayer()
        MusicPlaybackService.stop(context)
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        var instance: MusicPlayerManager? = null
    }
}
