package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ArtistItem
import com.example.data.model.Song
import com.example.ui.components.MusicSectionRow
import com.example.ui.theme.FlowBackground
import com.example.ui.theme.FlowCardBorder
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowPrimaryLight
import com.example.ui.theme.FlowSecondary
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    categories: Map<String, List<Song>>,
    recentlyPlayed: List<Song>,
    isLoading: Boolean,
    currentSongId: String?,
    isPlaying: Boolean,
    isLiked: (String) -> Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onLikeToggle: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = getGreetingForTime()

    // Extract top artists across categories
    val topArtists = rememberTopArtists(categories)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FlowBackground)
            .testTag("home_screen")
    ) {
        if (isLoading && categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = FlowPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Finding music for you...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FlowTextSecondary
                    )
                }
            }
        } else if (categories.isEmpty() && recentlyPlayed.isEmpty()) {
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FlowTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try again")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Top Brand & Greeting Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Brand bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = FlowPrimary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Flow Music",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Flow Music",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = (-0.5).sp,
                                            fontSize = 22.sp
                                        ),
                                        color = FlowTextPrimary
                                    )
                                    Text(
                                        text = "Created By Mohammed anas",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.2.sp,
                                            fontSize = 12.sp
                                        ),
                                        color = FlowPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Greeting Hero
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = FlowTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search Quick Trigger Bar
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSearchClick() },
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, FlowCardBorder),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = FlowTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "What do you want to play?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FlowTextSecondary
                                )
                            }
                        }
                    }
                }

                // Recently Played Section (if any exists)
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        MusicSectionRow(
                            title = "Recently Played",
                            subtitle = "Jump back in",
                            songs = recentlyPlayed,
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            isLiked = isLiked,
                            onSongClick = onSongClick,
                            onArtistClick = onArtistClick,
                            onLikeToggle = onLikeToggle,
                            onMoreClick = onMoreClick
                        )
                    }
                }

                // Quick Picks (first 6 items of Trending or Popular)
                val quickPicks = categories["Trending Now"] ?: categories["Popular"] ?: emptyList()
                if (quickPicks.isNotEmpty()) {
                    item {
                        QuickPicksSection(
                            songs = quickPicks.take(6),
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            onSongClick = { song -> onSongClick(song, quickPicks) }
                        )
                    }
                }

                // Popular Tamil
                categories["Tamil Hits"]?.let { songs ->
                    if (songs.isNotEmpty()) {
                        item {
                            MusicSectionRow(
                                title = "Popular Tamil",
                                subtitle = "Top chartbusters and melodies",
                                songs = songs,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                isLiked = isLiked,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onLikeToggle = onLikeToggle,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }

                // Popular Malayalam
                categories["Malayalam Hits"]?.let { songs ->
                    if (songs.isNotEmpty()) {
                        item {
                            MusicSectionRow(
                                title = "Popular Malayalam",
                                subtitle = "Soulful hits and new rhythms",
                                songs = songs,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                isLiked = isLiked,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onLikeToggle = onLikeToggle,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }

                // Top Artists Section
                if (topArtists.isNotEmpty()) {
                    item {
                        TopArtistsRow(
                            artists = topArtists,
                            onArtistClick = onArtistClick
                        )
                    }
                }

                // Trending Now
                categories["Trending Now"]?.let { songs ->
                    if (songs.isNotEmpty()) {
                        item {
                            MusicSectionRow(
                                title = "Trending Now",
                                subtitle = "Most played right now",
                                songs = songs,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                isLiked = isLiked,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onLikeToggle = onLikeToggle,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }

                // New Releases
                categories["New Releases"]?.let { songs ->
                    if (songs.isNotEmpty()) {
                        item {
                            MusicSectionRow(
                                title = "New Releases",
                                subtitle = "Fresh tracks this week",
                                songs = songs,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                isLiked = isLiked,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onLikeToggle = onLikeToggle,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }

                // Hindi Hits
                categories["Hindi Hits"]?.let { songs ->
                    if (songs.isNotEmpty()) {
                        item {
                            MusicSectionRow(
                                title = "Hindi Hits",
                                subtitle = "Bollywood & Indie favorites",
                                songs = songs,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                isLiked = isLiked,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onLikeToggle = onLikeToggle,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }

                // English Hits
                categories["English Hits"]?.let { songs ->
                    if (songs.isNotEmpty()) {
                        item {
                            MusicSectionRow(
                                title = "English Hits",
                                subtitle = "Global pop and trending beats",
                                songs = songs,
                                currentSongId = currentSongId,
                                isPlaying = isPlaying,
                                isLiked = isLiked,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onLikeToggle = onLikeToggle,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPicksSection(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Quick Picks",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ),
            color = FlowTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2-column or list grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val chunked = songs.chunked(2)
            chunked.forEach { rowSongs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSongs.forEach { song ->
                        val isCurrent = song.id == currentSongId
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSongClick(song) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) FlowPrimaryLight else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FlowCardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (song.artwork.isNotBlank()) {
                                        AsyncImage(
                                            model = song.artwork,
                                            contentDescription = song.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isCurrent) FlowPrimary else FlowTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = FlowTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (rowSongs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopArtistsRow(
    artists: List<ArtistItem>,
    onArtistClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Top Artists",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ),
            color = FlowTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists, key = { it.id.ifEmpty { it.name } }) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(96.dp)
                        .clickable { onArtistClick(artist.name) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
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
                                tint = FlowTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
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

private fun getGreetingForTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun rememberTopArtists(categories: Map<String, List<Song>>): List<ArtistItem> {
    val artists = mutableListOf<ArtistItem>()
    val seen = mutableSetOf<String>()
    for (list in categories.values) {
        for (song in list) {
            for (artist in song.artists) {
                if (artist.name.isNotBlank() && !seen.contains(artist.name.lowercase())) {
                    seen.add(artist.name.lowercase())
                    artists.add(artist)
                }
            }
        }
    }
    return artists.take(12)
}
