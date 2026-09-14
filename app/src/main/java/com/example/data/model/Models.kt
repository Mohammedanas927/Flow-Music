package com.example.data.model

import java.util.Locale

data class ArtistItem(
    val id: String = "",
    val name: String = "",
    val image: String = "",
    val url: String = ""
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artists: List<ArtistItem> = emptyList(),
    val album: String = "",
    val albumId: String? = null,
    val year: String? = null,
    val duration: Int = 0,
    val language: String? = null,
    val artwork: String = "",
    val audioUrl: String = "",
    val fallbackUrls: List<String> = emptyList(),
    val playCount: Long? = null,
    val explicit: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

object SongUtils {
    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    fun formatPlayCount(count: Long?): String {
        if (count == null || count <= 0) return ""
        return when {
            count >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.1fB plays", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM plays", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.getDefault(), "%.1fK plays", count / 1_000.0)
            else -> "$count plays"
        }
    }

    fun cleanHtml(input: String): String {
        return input
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }
}
