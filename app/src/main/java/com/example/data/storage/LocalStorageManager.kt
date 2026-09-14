package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ArtistItem
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalStorageManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flow_music_local_data", Context.MODE_PRIVATE)

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _likedSongs.value = loadSongsFromKey(KEY_LIKED_SONGS)
        _recentlyPlayed.value = loadSongsFromKey(KEY_RECENTLY_PLAYED)
        _playlists.value = loadPlaylists()
    }

    // Liked Songs
    fun isSongLiked(songId: String): Boolean {
        return _likedSongs.value.any { it.id == songId }
    }

    fun toggleLikeSong(song: Song): Boolean {
        val current = _likedSongs.value.toMutableList()
        val index = current.indexOfFirst { it.id == song.id }
        val nowLiked: Boolean
        if (index >= 0) {
            current.removeAt(index)
            nowLiked = false
        } else {
            current.add(0, song)
            nowLiked = true
        }
        _likedSongs.value = current
        saveSongsToKey(KEY_LIKED_SONGS, current)
        return nowLiked
    }

    // Recently Played (max 50, deduplicated, newest first)
    fun addRecentlyPlayed(song: Song) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        val limited = if (current.size > 50) current.take(50) else current
        _recentlyPlayed.value = limited
        saveSongsToKey(KEY_RECENTLY_PLAYED, limited)
    }

    // Playlists
    fun createPlaylist(name: String, description: String = ""): Playlist {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "My Playlist" },
            description = description,
            songs = emptyList()
        )
        val current = _playlists.value.toMutableList()
        current.add(0, newPlaylist)
        _playlists.value = current
        savePlaylists(current)
        return newPlaylist
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(name = newName.trim().ifEmpty { old.name })
            _playlists.value = current
            savePlaylists(current)
        }
    }

    fun deletePlaylist(playlistId: String) {
        val current = _playlists.value.toMutableList()
        current.removeAll { it.id == playlistId }
        _playlists.value = current
        savePlaylists(current)
    }

    fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val playlist = current[index]
            if (playlist.songs.none { it.id == song.id }) {
                val updatedSongs = playlist.songs.toMutableList().apply { add(song) }
                current[index] = playlist.copy(songs = updatedSongs)
                _playlists.value = current
                savePlaylists(current)
                return true
            }
        }
        return false
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val playlist = current[index]
            val updatedSongs = playlist.songs.filter { it.id != songId }
            current[index] = playlist.copy(songs = updatedSongs)
            _playlists.value = current
            savePlaylists(current)
        }
    }

    // JSON Serialization Helpers
    private fun saveSongsToKey(key: String, songs: List<Song>) {
        val jsonArray = JSONArray()
        for (song in songs) {
            jsonArray.put(songToJson(song))
        }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun loadSongsFromKey(key: String): List<Song> {
        val str = prefs.getString(key, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(str)
            val list = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                songFromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        val jsonArray = JSONArray()
        for (p in playlists) {
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("createdAt", p.createdAt)
                val sArr = JSONArray()
                for (s in p.songs) {
                    sArr.put(songToJson(s))
                }
                put("songs", sArr)
            }
            jsonArray.put(pObj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply()
    }

    private fun loadPlaylists(): List<Playlist> {
        val str = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(str)
            val list = mutableListOf<Playlist>()
            for (i in 0 until jsonArray.length()) {
                val pObj = jsonArray.optJSONObject(i) ?: continue
                val id = pObj.optString("id")
                val name = pObj.optString("name")
                val desc = pObj.optString("description")
                val createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
                val sArr = pObj.optJSONArray("songs") ?: JSONArray()
                val songs = mutableListOf<Song>()
                for (j in 0 until sArr.length()) {
                    val sObj = sArr.optJSONObject(j) ?: continue
                    songFromJson(sObj)?.let { songs.add(it) }
                }
                list.add(Playlist(id = id, name = name, description = desc, songs = songs, createdAt = createdAt))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun songToJson(song: Song): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("albumId", song.albumId ?: "")
            put("year", song.year ?: "")
            put("duration", song.duration)
            put("language", song.language ?: "")
            put("artwork", song.artwork)
            put("audioUrl", song.audioUrl)
            if (song.playCount != null) put("playCount", song.playCount)
            put("explicit", song.explicit)

            val aArr = JSONArray()
            for (a in song.artists) {
                aArr.put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("image", a.image)
                    put("url", a.url)
                })
            }
            put("artists", aArr)
        }
    }

    private fun songFromJson(obj: JSONObject): Song? {
        val id = obj.optString("id", "")
        if (id.isEmpty()) return null
        val title = obj.optString("title", "Untitled")
        val artist = obj.optString("artist", "")
        val album = obj.optString("album", "")
        val albumId = obj.optString("albumId").takeIf { it.isNotBlank() }
        val year = obj.optString("year").takeIf { it.isNotBlank() }
        val duration = obj.optInt("duration", 0)
        val language = obj.optString("language").takeIf { it.isNotBlank() }
        val artwork = obj.optString("artwork", "")
        val audioUrl = obj.optString("audioUrl", "")
        val playCount = if (obj.has("playCount")) obj.optLong("playCount") else null
        val explicit = obj.optBoolean("explicit", false)

        val artists = mutableListOf<ArtistItem>()
        val aArr = obj.optJSONArray("artists")
        if (aArr != null) {
            for (i in 0 until aArr.length()) {
                val aObj = aArr.optJSONObject(i) ?: continue
                artists.add(
                    ArtistItem(
                        id = aObj.optString("id"),
                        name = aObj.optString("name"),
                        image = aObj.optString("image"),
                        url = aObj.optString("url")
                    )
                )
            }
        }

        return Song(
            id = id,
            title = title,
            artist = artist,
            artists = artists,
            album = album,
            albumId = albumId,
            year = year,
            duration = duration,
            language = language,
            artwork = artwork,
            audioUrl = audioUrl,
            playCount = playCount,
            explicit = explicit
        )
    }

    companion object {
        private const val KEY_LIKED_SONGS = "key_liked_songs"
        private const val KEY_RECENTLY_PLAYED = "key_recently_played"
        private const val KEY_PLAYLISTS = "key_playlists"
    }
}
