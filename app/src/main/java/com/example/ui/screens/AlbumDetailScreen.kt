package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
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
fun AlbumDetailScreen(
    albumName: String,
    albumSongs: List<Song>,
    isLoading: Boolean,
    currentSongId: String?,
    isPlaying: Boolean,
    isLiked: (String) -> Boolean,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onArtistClick: (String) -> Unit,
    onLikeToggle: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumCover = albumSongs.firstOrNull()?.artwork
    val artistName = albumSongs.firstOrNull()?.artist ?: ""
    val year = albumSongs.firstOrNull()?.year

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlowBackground)
            .testTag("album_detail_screen")
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
                text = "Album",
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
                // Album Banner
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!albumCover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = albumCover,
                                        contentDescription = albumName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = FlowTextSecondary,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = FlowTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FlowTextSecondary
                                )
                                if (!year.isNullOrBlank()) {
                                    Text(
                                        text = "$year • ${albumSongs.size} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FlowTextSecondary
                                    )
                                } else {
                                    Text(
                                        text = "${albumSongs.size} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FlowTextSecondary
                                    )
                                }
                            }
                        }

                        if (albumSongs.isNotEmpty()) {
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

                // Tracks
                itemsIndexed(albumSongs, key = { index, s -> "${s.id}_$index" }) { index, song ->
                    SongRowItem(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        isPlaying = isPlaying,
                        isLiked = isLiked(song.id),
                        trackIndex = index + 1,
                        onSongClick = { onSongClick(song, albumSongs) },
                        onArtistClick = onArtistClick,
                        onLikeToggle = { onLikeToggle(song) },
                        onMoreClick = { onMoreClick(song) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
