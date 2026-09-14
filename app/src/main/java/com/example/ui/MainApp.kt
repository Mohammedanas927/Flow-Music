package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.data.storage.LocalStorageManager
import com.example.player.MusicPlayerManager
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.RenamePlaylistDialog
import com.example.ui.components.SongOptionsMenu
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LikedSongsScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.FlowAccent
import com.example.ui.theme.FlowCardBorder
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object Library : Screen()
    object Liked : Screen()
    data class PlaylistDetail(val playlistId: String) : Screen()
    data class ArtistDetail(val artistName: String) : Screen()
    data class AlbumDetail(val albumName: String) : Screen()
    object Queue : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    repository: MusicRepository,
    storage: LocalStorageManager,
    player: MusicPlayerManager
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation Stack
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var screenStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }

    fun navigateTo(screen: Screen) {
        screenStack = screenStack + screen
        currentScreen = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            val newStack = screenStack.dropLast(1)
            screenStack = newStack
            currentScreen = newStack.last()
            return true
        }
        return false
    }

    // Full Player Modal
    var isFullPlayerExpanded by remember { mutableStateOf(false) }

    // Handle Back Press
    BackHandler(enabled = isFullPlayerExpanded || screenStack.size > 1) {
        if (isFullPlayerExpanded) {
            isFullPlayerExpanded = false
        } else {
            navigateBack()
        }
    }

    // Player State
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isBuffering by player.isBuffering.collectAsState()
    val currentPosMs by player.currentPositionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val queue by player.queue.collectAsState()
    val volume by player.volume.collectAsState()
    val isMuted by player.isMuted.collectAsState()

    // Storage State
    val likedSongs by storage.likedSongs.collectAsState()
    val playlists by storage.playlists.collectAsState()
    val recentlyPlayed by storage.recentlyPlayed.collectAsState()

    // Home Categories State
    var categories by remember { mutableStateOf<Map<String, List<Song>>>(emptyMap()) }
    var isHomeLoading by remember { mutableStateOf(true) }

    fun loadHomeData() {
        scope.launch {
            isHomeLoading = true
            categories = repository.fetchHomeCategories()
            isHomeLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHomeData()
    }

    // Search Screen State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            val res = repository.searchSongs(query)
            res.onSuccess { songs ->
                searchResults = songs
            }.onFailure { err ->
                searchError = err.message ?: "Network error"
            }
            isSearching = false
        }
    }

    // Detail Screen Data Fetchers
    var artistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isArtistLoading by remember { mutableStateOf(false) }
    var albumSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isAlbumLoading by remember { mutableStateOf(false) }

    fun openArtist(name: String) {
        navigateTo(Screen.ArtistDetail(name))
        scope.launch {
            isArtistLoading = true
            val res = repository.searchSongs(name)
            artistSongs = res.getOrNull() ?: emptyList()
            isArtistLoading = false
        }
    }

    fun openAlbum(name: String) {
        navigateTo(Screen.AlbumDetail(name))
        scope.launch {
            isAlbumLoading = true
            val res = repository.searchSongs(name)
            albumSongs = res.getOrNull() ?: emptyList()
            isAlbumLoading = false
        }
    }

    // Options Menu Sheet
    val optionsMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var songForMenu by remember { mutableStateOf<Song?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Dialogs State
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songForAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    fun playSongWithContext(song: Song, contextSongs: List<Song>) {
        storage.addRecentlyPlayed(song)
        player.playSong(song, contextSongs)
        isFullPlayerExpanded = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Mini Player (docked right above navigation bar)
                if (currentSong != null && !isFullPlayerExpanded) {
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPosMs = currentPosMs,
                        durationMs = durationMs,
                        isLiked = storage.isSongLiked(currentSong!!.id),
                        onPlayPause = { player.togglePlayPause() },
                        onNext = { player.playNext() },
                        onLikeToggle = {
                            val liked = storage.toggleLikeSong(currentSong!!)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                                )
                            }
                        },
                        onExpand = { isFullPlayerExpanded = true }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = FlowTextPrimary,
                    tonalElevation = 6.dp
                ) {
                    val navItems = listOf(
                        Triple(Screen.Home, "Home", Icons.Default.Home to Icons.Outlined.Home),
                        Triple(Screen.Search, "Search", Icons.Default.Search to Icons.Outlined.Search),
                        Triple(Screen.Library, "Library", Icons.AutoMirrored.Filled.QueueMusic to Icons.AutoMirrored.Outlined.QueueMusic),
                        Triple(Screen.Liked, "Liked", Icons.Default.Favorite to Icons.Default.FavoriteBorder)
                    )

                    navItems.forEach { (screen, label, icons) ->
                        val isSelected = when (screen) {
                            Screen.Home -> currentScreen is Screen.Home
                            Screen.Search -> currentScreen is Screen.Search
                            Screen.Library -> currentScreen is Screen.Library || currentScreen is Screen.PlaylistDetail
                            Screen.Liked -> currentScreen is Screen.Liked
                            else -> false
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                isFullPlayerExpanded = false
                                if (currentScreen != screen) {
                                    screenStack = listOf(screen)
                                    currentScreen = screen
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) icons.first else icons.second,
                                    contentDescription = label,
                                    tint = if (isSelected) FlowPrimary else FlowTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (isSelected) FlowPrimary else FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        categories = categories,
                        recentlyPlayed = recentlyPlayed,
                        isLoading = isHomeLoading,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        isLiked = { storage.isSongLiked(it) },
                        onSongClick = { s, list -> playSongWithContext(s, list) },
                        onArtistClick = { openArtist(it) },
                        onAlbumClick = { openAlbum(it) },
                        onLikeToggle = { s ->
                            val liked = storage.toggleLikeSong(s)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                                )
                            }
                        },
                        onMoreClick = { s ->
                            songForMenu = s
                            showOptionsMenu = true
                        },
                        onSearchClick = {
                            screenStack = listOf(Screen.Search)
                            currentScreen = Screen.Search
                        },
                        onRetry = { loadHomeData() }
                    )
                }

                is Screen.Search -> {
                    SearchScreen(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            if (it.isBlank()) {
                                searchResults = emptyList()
                                searchError = null
                            } else {
                                performSearch(it)
                            }
                        },
                        onSearch = { performSearch(it) },
                        searchResults = searchResults,
                        isSearching = isSearching,
                        searchError = searchError,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        isLiked = { storage.isSongLiked(it) },
                        onSongClick = { s, list -> playSongWithContext(s, list) },
                        onArtistClick = { openArtist(it) },
                        onAlbumClick = { openAlbum(it) },
                        onLikeToggle = { s ->
                            val liked = storage.toggleLikeSong(s)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                                )
                            }
                        },
                        onMoreClick = { s ->
                            songForMenu = s
                            showOptionsMenu = true
                        }
                    )
                }

                is Screen.Library -> {
                    LibraryScreen(
                        likedSongs = likedSongs,
                        playlists = playlists,
                        recentlyPlayed = recentlyPlayed,
                        onOpenLikedSongs = { navigateTo(Screen.Liked) },
                        onOpenPlaylist = { p -> navigateTo(Screen.PlaylistDetail(p.id)) },
                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                        onRenamePlaylist = { p -> playlistToRename = p },
                        onDeletePlaylist = { p -> playlistToDelete = p },
                        onPlayLikedSongs = {
                            if (likedSongs.isNotEmpty()) {
                                playSongWithContext(likedSongs.first(), likedSongs)
                            }
                        }
                    )
                }

                is Screen.Liked -> {
                    LikedSongsScreen(
                        likedSongs = likedSongs,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        onBack = { navigateBack() },
                        onSongClick = { s, list -> playSongWithContext(s, list) },
                        onPlayAll = {
                            if (likedSongs.isNotEmpty()) {
                                playSongWithContext(likedSongs.first(), likedSongs)
                            }
                        },
                        onShuffleAll = {
                            if (likedSongs.isNotEmpty()) {
                                val shuffled = likedSongs.shuffled()
                                playSongWithContext(shuffled.first(), shuffled)
                            }
                        },
                        onArtistClick = { openArtist(it) },
                        onAlbumClick = { openAlbum(it) },
                        onLikeToggle = { s ->
                            storage.toggleLikeSong(s)
                        },
                        onMoreClick = { s ->
                            songForMenu = s
                            showOptionsMenu = true
                        }
                    )
                }

                is Screen.PlaylistDetail -> {
                    val playlist = playlists.find { it.id == screen.playlistId }
                    if (playlist != null) {
                        PlaylistDetailScreen(
                            playlist = playlist,
                            currentSongId = currentSong?.id,
                            isPlaying = isPlaying,
                            isLiked = { storage.isSongLiked(it) },
                            onBack = { navigateBack() },
                            onSongClick = { s, list -> playSongWithContext(s, list) },
                            onPlayAll = {
                                if (playlist.songs.isNotEmpty()) {
                                    playSongWithContext(playlist.songs.first(), playlist.songs)
                                }
                            },
                            onShuffleAll = {
                                if (playlist.songs.isNotEmpty()) {
                                    val shuffled = playlist.songs.shuffled()
                                    playSongWithContext(shuffled.first(), shuffled)
                                }
                            },
                            onArtistClick = { openArtist(it) },
                            onAlbumClick = { openAlbum(it) },
                            onLikeToggle = { s ->
                                storage.toggleLikeSong(s)
                            },
                            onMoreClick = { s ->
                                songForMenu = s
                                showOptionsMenu = true
                            },
                            onRename = { playlistToRename = playlist },
                            onDelete = { playlistToDelete = playlist }
                        )
                    } else {
                        navigateBack()
                    }
                }

                is Screen.ArtistDetail -> {
                    ArtistDetailScreen(
                        artistName = screen.artistName,
                        artistSongs = artistSongs,
                        isLoading = isArtistLoading,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        isLiked = { storage.isSongLiked(it) },
                        onBack = { navigateBack() },
                        onSongClick = { s, list -> playSongWithContext(s, list) },
                        onPlayAll = {
                            if (artistSongs.isNotEmpty()) {
                                playSongWithContext(artistSongs.first(), artistSongs)
                            }
                        },
                        onShuffleAll = {
                            if (artistSongs.isNotEmpty()) {
                                val shuffled = artistSongs.shuffled()
                                playSongWithContext(shuffled.first(), shuffled)
                            }
                        },
                        onAlbumClick = { openAlbum(it) },
                        onLikeToggle = { s ->
                            val liked = storage.toggleLikeSong(s)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                                )
                            }
                        },
                        onMoreClick = { s ->
                            songForMenu = s
                            showOptionsMenu = true
                        }
                    )
                }

                is Screen.AlbumDetail -> {
                    AlbumDetailScreen(
                        albumName = screen.albumName,
                        albumSongs = albumSongs,
                        isLoading = isAlbumLoading,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        isLiked = { storage.isSongLiked(it) },
                        onBack = { navigateBack() },
                        onSongClick = { s, list -> playSongWithContext(s, list) },
                        onPlayAll = {
                            if (albumSongs.isNotEmpty()) {
                                playSongWithContext(albumSongs.first(), albumSongs)
                            }
                        },
                        onShuffleAll = {
                            if (albumSongs.isNotEmpty()) {
                                val shuffled = albumSongs.shuffled()
                                playSongWithContext(shuffled.first(), shuffled)
                            }
                        },
                        onArtistClick = { openArtist(it) },
                        onLikeToggle = { s ->
                            val liked = storage.toggleLikeSong(s)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                                )
                            }
                        },
                        onMoreClick = { s ->
                            songForMenu = s
                            showOptionsMenu = true
                        }
                    )
                }

                is Screen.Queue -> {
                    QueueScreen(
                        currentSong = currentSong,
                        queue = queue,
                        isPlaying = isPlaying,
                        onBack = { navigateBack() },
                        onSongClick = { s -> playSongWithContext(s, queue) },
                        onRemoveFromQueue = { player.removeFromQueue(it) },
                        onClearQueue = { player.clearQueue() }
                    )
                }
            }

            // Full Player Modal Overlay
            AnimatedVisibility(
                visible = isFullPlayerExpanded && currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FullPlayerSheet(
                    song = currentSong,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    currentPosMs = currentPosMs,
                    durationMs = durationMs,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    isLiked = if (currentSong != null) storage.isSongLiked(currentSong!!.id) else false,
                    volume = volume,
                    isMuted = isMuted,
                    onPlayPause = { player.togglePlayPause() },
                    onNext = { player.playNext() },
                    onPrevious = { player.playPrevious() },
                    onSeekTo = { player.seekTo(it) },
                    onToggleShuffle = { player.toggleShuffle() },
                    onCycleRepeat = { player.cycleRepeatMode() },
                    onToggleLike = {
                        currentSong?.let {
                            val liked = storage.toggleLikeSong(it)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                                )
                            }
                        }
                    },
                    onVolumeChange = { player.setVolume(it) },
                    onToggleMute = { player.toggleMute() },
                    onDismiss = { isFullPlayerExpanded = false },
                    onOpenQueue = {
                        isFullPlayerExpanded = false
                        navigateTo(Screen.Queue)
                    },
                    onAddToPlaylist = {
                        songForAddToPlaylist = currentSong
                        showAddToPlaylistDialog = true
                    },
                    onArtistClick = {
                        isFullPlayerExpanded = false
                        openArtist(it)
                    },
                    onMoreClick = {
                        songForMenu = currentSong
                        showOptionsMenu = true
                    }
                )
            }
        }
    }

    // More Options Bottom Sheet
    if (showOptionsMenu && songForMenu != null) {
        val s = songForMenu!!
        SongOptionsMenu(
            song = s,
            isLiked = storage.isSongLiked(s.id),
            sheetState = optionsMenuSheetState,
            onDismiss = { showOptionsMenu = false },
            onPlay = {
                playSongWithContext(s, listOf(s))
            },
            onPlayNext = {
                player.playNextInQueue(s)
                scope.launch { snackbarHostState.showSnackbar("Playing \"${s.title}\" next") }
            },
            onAddToQueue = {
                player.addToQueue(s)
                scope.launch { snackbarHostState.showSnackbar("Added \"${s.title}\" to queue") }
            },
            onAddToPlaylist = {
                songForAddToPlaylist = s
                showAddToPlaylistDialog = true
            },
            onToggleLike = {
                val liked = storage.toggleLikeSong(s)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (liked) "Saved to Liked Songs" else "Removed from Liked Songs"
                    )
                }
            },
            onGoToArtist = { openArtist(it) },
            onGoToAlbum = { openAlbum(it) }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                val newPl = storage.createPlaylist(name)
                showCreatePlaylistDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Created playlist \"${newPl.name}\"")
                }
            }
        )
    }

    // Add To Playlist Dialog
    if (showAddToPlaylistDialog && songForAddToPlaylist != null) {
        val s = songForAddToPlaylist!!
        AddToPlaylistDialog(
            song = s,
            playlists = playlists,
            onDismiss = {
                showAddToPlaylistDialog = false
                songForAddToPlaylist = null
            },
            onSelectPlaylist = { pId ->
                val success = storage.addSongToPlaylist(pId, s)
                val pName = playlists.find { it.id == pId }?.name ?: "playlist"
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) "Added to $pName" else "Already in $pName"
                    )
                }
            },
            onCreateNewPlaylist = {
                showCreatePlaylistDialog = true
            }
        )
    }

    // Rename Playlist Dialog
    playlistToRename?.let { pl ->
        RenamePlaylistDialog(
            currentName = pl.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                storage.renamePlaylist(pl.id, newName)
                playlistToRename = null
                scope.launch { snackbarHostState.showSnackbar("Playlist renamed to \"$newName\"") }
            }
        )
    }

    // Delete Playlist Dialog
    playlistToDelete?.let { pl ->
        ConfirmDeleteDialog(
            title = "Delete Playlist",
            message = "Are you sure you want to delete \"${pl.name}\"? This action cannot be undone.",
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                storage.deletePlaylist(pl.id)
                playlistToDelete = null
                if (currentScreen is Screen.PlaylistDetail) {
                    navigateBack()
                }
                scope.launch { snackbarHostState.showSnackbar("Playlist deleted") }
            }
        )
    }
}
