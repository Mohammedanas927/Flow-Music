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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.components.SongRowItem
import com.example.ui.theme.FlowBackground
import com.example.ui.theme.FlowCardBorder
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary

@Composable
fun ArtistDetailScreen(
    artistName: String,
    artistSongs: List<Song>,
    isLoading: Boolean,
    currentSongId: String?,
    isPlaying: Boolean,
    isLiked: (String) -> Boolean,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onLikeToggle: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    // Find artist avatar if available from song metadata
    val artistAvatar = artistSongs.firstOrNull { s ->
        s.artists.any { it.name.equals(artistName, ignoreCase = true) && it.image.isNotBlank() }
    }?.artists?.firstOrNull { it.name.equals(artistName, ignoreCase = true) }?.image
        ?: artistSongs.firstOrNull()?.artwork

    // Distinct albums
    val albums = artistSongs.mapNotNull { it.album.takeIf { a -> a.isNotBlank() } }.distinct()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlowBackground)
            .testTag("artist_detail_screen")
    ) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FlowTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Artist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FlowTextPrimary
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlowPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Artist Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!artistAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = artistAvatar,
                                    contentDescription = artistName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = FlowTextSecondary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = FlowTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${artistSongs.size} tracks available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FlowTextSecondary
                        )

                        if (artistSongs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onPlayAll,
                                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play All")
                                }

                                OutlinedButton(
                                    onClick = onShuffleAll,
                                    shape = RoundedCornerShape(24.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, FlowCardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        tint = FlowTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Shuffle", color = FlowTextPrimary)
                                }
                            }
                        }
                    }
                }

                // Popular Songs Header
                item {
                    Text(
                        text = "Popular Songs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = FlowTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Songs
                itemsIndexed(artistSongs, key = { index, s -> "${s.id}_$index" }) { index, song ->
                    SongRowItem(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        isPlaying = isPlaying,
                        isLiked = isLiked(song.id),
                        trackIndex = index + 1,
                        onSongClick = { onSongClick(song, artistSongs) },
                        onAlbumClick = onAlbumClick,
                        onLikeToggle = { onLikeToggle(song) },
                        onMoreClick = { onMoreClick(song) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
