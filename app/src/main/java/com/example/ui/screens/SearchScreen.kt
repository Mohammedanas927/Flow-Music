package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ArtistItem
import com.example.data.model.Song
import com.example.ui.components.SongRowItem
import com.example.ui.theme.FlowBackground
import com.example.ui.theme.FlowCardBorder
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowPrimaryLight
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary
import com.example.ui.theme.FlowTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<Song>,
    isSearching: Boolean,
    searchError: String?,
    currentSongId: String?,
    isPlaying: Boolean,
    isLiked: (String) -> Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onLikeToggle: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var selectedFilter by remember { mutableStateOf("All") }

    val shortcutCategories = listOf(
        "Tamil", "Malayalam", "Trending", "Hindi", "English",
        "Anirudh", "Sid Sriram", "A.R. Rahman", "Katchi Sera", "Why This Kolaveri"
    )

    // Extracted artists and albums from results
    val artistsInResults = remember(searchResults) {
        val list = mutableListOf<ArtistItem>()
        val seen = mutableSetOf<String>()
        for (song in searchResults) {
            for (artist in song.artists) {
                if (artist.name.isNotBlank() && !seen.contains(artist.name.lowercase())) {
                    seen.add(artist.name.lowercase())
                    list.add(artist)
                }
            }
        }
        list
    }

    val albumsInResults = remember(searchResults) {
        val list = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        for (song in searchResults) {
            if (song.album.isNotBlank() && !seen.contains(song.album.lowercase())) {
                seen.add(song.album.lowercase())
                list.add(song)
            }
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlowBackground)
            .testTag("search_screen")
    ) {
        // Search Bar Area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "What do you want to play?",
                            color = FlowTextSecondary,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = FlowPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                onQueryChange("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = FlowTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearch(query)
                    }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowCardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = FlowBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )

                // Filter tabs when search results exist
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("All", "Songs", "Artists", "Albums").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FlowPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isSearching -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = FlowPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (query.isNotBlank()) "Searching for \"$query\"..." else "Finding music...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlowTextSecondary
                            )
                        }
                    }
                }

                searchError != null -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Unable to load music right now.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FlowTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = searchError,
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onSearch(query) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Try again")
                            }
                        }
                    }
                }

                query.isNotBlank() && searchResults.isEmpty() -> {
                    // Zero results state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = FlowTextTertiary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No music found for \"$query\".",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FlowTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Check for spelling mistakes or try a different song or artist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                searchResults.isNotEmpty() -> {
                    // Search Results List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            Text(
                                text = "Search results for \"$query\"",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = FlowTextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        // Artists Row (if "All" or "Artists" selected)
                        if ((selectedFilter == "All" || selectedFilter == "Artists") && artistsInResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Artists",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FlowTextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(artistsInResults, key = { it.id.ifEmpty { it.name } }) { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(88.dp)
                                                .clickable { onArtistClick(artist.name) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (artist.image.isNotBlank()) {
                                                    AsyncImage(
                                                        model = artist.image,
                                                        contentDescription = artist.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = FlowTextSecondary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = FlowTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Albums Row (if "All" or "Albums" selected)
                        if ((selectedFilter == "All" || selectedFilter == "Albums") && albumsInResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FlowTextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(albumsInResults, key = { "album_${it.id}" }) { albumSong ->
                                        Column(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .clickable { onAlbumClick(albumSong.album) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                if (albumSong.artwork.isNotBlank()) {
                                                    AsyncImage(
                                                        model = albumSong.artwork,
                                                        contentDescription = albumSong.album,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = albumSong.album,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = FlowTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = albumSong.artist,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = FlowTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Songs Section (if "All" or "Songs" selected)
                        if (selectedFilter == "All" || selectedFilter == "Songs") {
                            item {
                                Text(
                                    text = "Songs",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FlowTextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(searchResults, key = { it.id }) { song ->
                                SongRowItem(
                                    song = song,
                                    isCurrent = song.id == currentSongId,
                                    isPlaying = isPlaying,
                                    isLiked = isLiked(song.id),
                                    onSongClick = { onSongClick(song, searchResults) },
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    onLikeToggle = { onLikeToggle(song) },
                                    onMoreClick = { onMoreClick(song) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                }

                else -> {
                    // Default Search Landing Page
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Search for songs, artists, albums and more",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = FlowTextPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Explore popular genres, languages, and trending artists.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FlowTextSecondary,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }

                        item {
                            Text(
                                text = "Quick Searches",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FlowTextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                shortcutCategories.forEach { shortcut ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = FlowPrimaryLight,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, FlowPrimary.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable {
                                            onQueryChange(shortcut)
                                            onSearch(shortcut)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = FlowPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = shortcut,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = FlowPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                }
            }
        }
    }
}
