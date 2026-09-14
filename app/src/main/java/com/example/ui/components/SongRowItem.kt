package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.SongUtils
import com.example.ui.theme.FlowAccent
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowPrimaryLight
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary
import com.example.ui.theme.FlowTextTertiary

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    onSongClick: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((String) -> Unit)? = null,
    onLikeToggle: () -> Unit,
    onMoreClick: () -> Unit,
    trackIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSongClick() }
            .testTag("song_row_${song.id}"),
        color = if (isCurrent) FlowPrimaryLight.copy(alpha = 0.6f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Track Index
            if (trackIndex != null) {
                Text(
                    text = "$trackIndex",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isCurrent) FlowPrimary else FlowTextTertiary,
                    modifier = Modifier.width(28.dp)
                )
            }

            // Artwork
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = FlowTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = if (isCurrent) FlowPrimary else FlowTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.explicit) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Explicit,
                            contentDescription = "Explicit",
                            tint = FlowTextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = FlowTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(enabled = onArtistClick != null) {
                                if (song.artists.isNotEmpty()) {
                                    onArtistClick?.invoke(song.artists.first().name)
                                } else if (song.artist.isNotBlank()) {
                                    onArtistClick?.invoke(song.artist)
                                }
                            }
                    )

                    if (song.album.isNotBlank() && song.album != song.title) {
                        Text(
                            text = " • ${song.album}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = FlowTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(enabled = onAlbumClick != null) {
                                    onAlbumClick?.invoke(song.album)
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Duration
            if (song.duration > 0) {
                Text(
                    text = SongUtils.formatDuration(song.duration),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = FlowTextSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Like toggle
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) FlowAccent else FlowTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More options
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = FlowTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
