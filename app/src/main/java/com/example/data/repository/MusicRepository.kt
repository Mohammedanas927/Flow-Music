package com.example.data.repository

import com.example.data.model.ArtistItem
import com.example.data.model.Song
import com.example.data.model.SongUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MusicRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val searchCache = ConcurrentHashMap<String, List<Song>>()

    suspend fun searchSongs(rawQuery: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val cacheKey = query.lowercase()
        searchCache[cacheKey]?.let { cached ->
            return@withContext Result.success(cached)
        }

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
            val url = "https://music-flow-x9th.onrender.com/api/search/songs?query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FlowMusic/1.0 (Android)")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
            }

            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            val jsonObject = JSONObject(bodyString)
            val success = jsonObject.optBoolean("success", false)
            if (!success && !jsonObject.has("data")) {
                return@withContext Result.failure(Exception("API returned unsuccessful status"))
            }

            val dataObj = jsonObject.optJSONObject("data")
            val resultsArr = dataObj?.optJSONArray("results") ?: JSONArray()
            val songList = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()

            for (i in 0 until resultsArr.length()) {
                val item = resultsArr.optJSONObject(i) ?: continue
                val song = normalizeSong(item)
                if (song != null && !seenIds.contains(song.id)) {
                    seenIds.add(song.id)
                    songList.add(song)
                }
            }

            searchCache[cacheKey] = songList
            Result.success(songList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeSong(item: JSONObject): Song? {
        val id = item.optString("id", "").ifEmpty { return null }
        val title = SongUtils.cleanHtml(item.optString("name", "Untitled Song"))
        val duration = item.optInt("duration", 0)
        val year = item.optString("year").takeIf { it.isNotBlank() && it != "null" }
        val language = item.optString("language").takeIf { it.isNotBlank() && it != "null" }
        val explicit = item.optBoolean("explicitContent", false)
        val playCount = if (item.has("playCount") && !item.isNull("playCount")) {
            item.optLong("playCount")
        } else null

        // Album
        val albumObj = item.optJSONObject("album")
        val albumName = SongUtils.cleanHtml(albumObj?.optString("name", "") ?: "")
        val albumId = albumObj?.optString("id")?.takeIf { it.isNotBlank() && it != "null" }

        // Artists
        val artistsObj = item.optJSONObject("artists")
        val primaryArr = artistsObj?.optJSONArray("primary")
        val artistsList = mutableListOf<ArtistItem>()
        if (primaryArr != null) {
            for (j in 0 until primaryArr.length()) {
                val aObj = primaryArr.optJSONObject(j) ?: continue
                val aId = aObj.optString("id", "")
                val aName = SongUtils.cleanHtml(aObj.optString("name", ""))
                val aUrl = aObj.optString("url", "")
                val aImageArr = aObj.optJSONArray("image")
                val aImg = extractBestImage(aImageArr)
                if (aName.isNotBlank()) {
                    artistsList.add(ArtistItem(id = aId, name = aName, image = aImg, url = aUrl))
                }
            }
        }

        val artistFormatted = when {
            artistsList.isNotEmpty() -> artistsList.joinToString(", ") { it.name }
            albumName.isNotBlank() -> albumName
            else -> "Various Artists"
        }

        // Artwork
        val imageArr = item.optJSONArray("image")
        val artwork = extractBestImage(imageArr)

        // Audio URLs (extract primary + fallback qualities, enforce https)
        val downloadArr = item.optJSONArray("downloadUrl")
        val (audioUrl, fallbackUrls) = extractAudioUrls(downloadArr)
        if (audioUrl.isBlank()) {
            return null // Ignore if no playable URL
        }

        return Song(
            id = id,
            title = title,
            artist = artistFormatted,
            artists = artistsList,
            album = albumName,
            albumId = albumId,
            year = year,
            duration = duration,
            language = language,
            artwork = artwork,
            audioUrl = audioUrl,
            fallbackUrls = fallbackUrls,
            playCount = playCount,
            explicit = explicit
        )
    }

    private fun extractBestImage(imageArr: JSONArray?): String {
        if (imageArr == null || imageArr.length() == 0) return ""
        var quality500 = ""
        var quality150 = ""
        var quality50 = ""
        var firstUrl = ""

        for (i in 0 until imageArr.length()) {
            val imgObj = imageArr.optJSONObject(i) ?: continue
            val quality = imgObj.optString("quality", "")
            val rawUrl = imgObj.optString("url", "")
            val url = if (rawUrl.startsWith("http://")) rawUrl.replace("http://", "https://") else rawUrl
            if (url.isBlank()) continue
            if (firstUrl.isEmpty()) firstUrl = url
            when (quality) {
                "500x500" -> quality500 = url
                "150x150" -> quality150 = url
                "50x50" -> quality50 = url
            }
        }

        return when {
            quality500.isNotEmpty() -> quality500
            quality150.isNotEmpty() -> quality150
            quality50.isNotEmpty() -> quality50
            else -> firstUrl
        }
    }

    private fun extractAudioUrls(downloadArr: JSONArray?): Pair<String, List<String>> {
        if (downloadArr == null || downloadArr.length() == 0) return "" to emptyList()
        val urlsByQuality = mutableMapOf<String, String>()
        val orderedUrls = mutableListOf<String>()

        for (i in 0 until downloadArr.length()) {
            val dObj = downloadArr.optJSONObject(i) ?: continue
            val quality = dObj.optString("quality", "").lowercase().trim()
            val rawUrl = dObj.optString("url", "").trim()
            if (rawUrl.isBlank()) continue
            val secureUrl = if (rawUrl.startsWith("http://")) rawUrl.replace("http://", "https://") else rawUrl
            urlsByQuality[quality] = secureUrl
            if (!orderedUrls.contains(secureUrl)) {
                orderedUrls.add(secureUrl)
            }
        }

        // Prefer 160kbps for fast buffering and rich audio, then 320kbps, then 96kbps, then others
        val primary = urlsByQuality["160kbps"]
            ?: urlsByQuality["320kbps"]
            ?: urlsByQuality["96kbps"]
            ?: urlsByQuality["48kbps"]
            ?: orderedUrls.firstOrNull()
            ?: ""

        val fallbacks = mutableListOf<String>()
        // Add remaining qualities in descending preference
        val prefList = listOf("320kbps", "160kbps", "96kbps", "48kbps", "12kbps")
        for (q in prefList) {
            val u = urlsByQuality[q]
            if (u != null && u != primary && !fallbacks.contains(u)) {
                fallbacks.add(u)
            }
        }
        for (u in orderedUrls) {
            if (u != primary && !fallbacks.contains(u)) {
                fallbacks.add(u)
            }
        }

        return primary to fallbacks
    }

    suspend fun fetchHomeCategories(): Map<String, List<Song>> = coroutineScope {
        val categories = listOf(
            "Tamil Hits" to "Tamil",
            "Malayalam Hits" to "Malayalam",
            "Trending Now" to "Trending",
            "Popular" to "Popular",
            "New Releases" to "New Songs",
            "Hindi Hits" to "Hindi",
            "English Hits" to "English"
        )

        val deferredResults = categories.map { (title, query) ->
            async {
                val res = searchSongs(query)
                title to (res.getOrNull() ?: emptyList())
            }
        }

        deferredResults.map { it.await() }.toMap()
    }
}
