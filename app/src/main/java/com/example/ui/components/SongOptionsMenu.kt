package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.FlowAccent
import com.example.ui.theme.FlowCardBorder
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsMenu(
    song: Song?,
    isLiked: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLike: () -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit
) {
    if (song == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header with song info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (song.artwork.isNotBlank()) {
                    AsyncImage(
                        model = song.artwork,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FlowTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = FlowTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = FlowCardBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            MenuActionItem(
                icon = Icons.Default.PlayArrow,
                text = "Play",
                onClick = {
                    onPlay()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = Icons.Default.PlaylistPlay,
                text = "Play next",
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                text = "Add to queue",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = Icons.Default.PlaylistAdd,
                text = "Add to playlist",
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                text = if (isLiked) "Remove from Liked Songs" else "Save to Liked Songs",
                iconTint = if (isLiked) FlowAccent else FlowTextPrimary,
                onClick = {
                    onToggleLike()
                    onDismiss()
                }
            )

            if (song.artist.isNotBlank()) {
                val primaryArtist = if (song.artists.isNotEmpty()) song.artists.first().name else song.artist
                MenuActionItem(
                    icon = Icons.Default.Person,
                    text = "Go to artist ($primaryArtist)",
                    onClick = {
                        onGoToArtist(primaryArtist)
                        onDismiss()
                    }
                )
            }

            if (song.album.isNotBlank()) {
                MenuActionItem(
                    icon = Icons.Default.Album,
                    text = "Go to album (${song.album})",
                    onClick = {
                        onGoToAlbum(song.album)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MenuActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = FlowTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = FlowTextPrimary
        )
    }
}
