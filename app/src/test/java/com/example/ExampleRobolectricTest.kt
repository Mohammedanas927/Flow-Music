package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Song
import com.example.data.model.SongUtils
import com.example.data.storage.LocalStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flow Music", appName)
  }

  @Test
  fun `formatDuration formats properly`() {
    assertEquals("0:00", SongUtils.formatDuration(0))
    assertEquals("3:08", SongUtils.formatDuration(188))
    assertEquals("4:29", SongUtils.formatDuration(269))
  }

  @Test
  fun `local storage manages liked songs and playlists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = LocalStorageManager(context)

    val song = Song(
      id = "test_song_1",
      title = "Hukum",
      artist = "Anirudh Ravichander",
      duration = 200,
      audioUrl = "https://test.com/audio.mp4"
    )

    val liked = storage.toggleLikeSong(song)
    assertTrue(liked)
    assertTrue(storage.isSongLiked("test_song_1"))

    val playlist = storage.createPlaylist("Favorites")
    assertEquals("Favorites", playlist.name)

    val added = storage.addSongToPlaylist(playlist.id, song)
    assertTrue(added)
  }
}
