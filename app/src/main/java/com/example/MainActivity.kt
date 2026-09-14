package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.data.repository.MusicRepository
import com.example.data.storage.LocalStorageManager
import com.example.player.MusicPlayerManager
import com.example.ui.MainApp
import com.example.ui.theme.FlowMusicTheme

class MainActivity : ComponentActivity() {

    private lateinit var storageManager: LocalStorageManager
    private lateinit var musicRepository: MusicRepository
    private lateinit var playerManager: MusicPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        storageManager = LocalStorageManager(applicationContext)
        musicRepository = MusicRepository()
        playerManager = MusicPlayerManager(applicationContext) { song ->
            storageManager.addRecentlyPlayed(song)
        }

        setContent {
            FlowMusicTheme {
                MainApp(
                    repository = musicRepository,
                    storage = storageManager,
                    player = playerManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.releasePlayer()
    }
}
