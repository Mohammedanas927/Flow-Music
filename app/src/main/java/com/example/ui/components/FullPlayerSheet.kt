package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.SongUtils
import com.example.player.RepeatMode
import com.example.ui.theme.FlowAccent
import com.example.ui.theme.FlowCardBorder
import com.example.ui.theme.FlowPrimary
import com.example.ui.theme.FlowPrimaryLight
import com.example.ui.theme.FlowTextPrimary
import com.example.ui.theme.FlowTextSecondary
import com.example.ui.theme.FlowTextTertiary

@Composable
fun FullPlayerSheet(
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosMs: Int,
    durationMs: Int,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isLiked: Boolean,
    volume: Float,
    isMuted: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onArtistClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    val totalMs = if (durationMs > 0) durationMs else (song.duration * 1000)
    val displayedPosMs = if (isSeeking) {
        (seekProgress * totalMs).toInt()
    } else {
        currentPosMs
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("full_player_sheet"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Dismiss, "Now Playing", Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = FlowTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = FlowTextTertiary
                    )
                    Text(
                        text = if (song.album.isNotBlank()) song.album else "Flow Music",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FlowTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = FlowTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
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
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title, Artist, and Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = FlowTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (song.explicit) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Explicit,
                                contentDescription = "Explicit",
                                tint = FlowTextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = FlowTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            if (song.artists.isNotEmpty()) {
                                onArtistClick(song.artists.first().name)
                            } else if (song.artist.isNotBlank()) {
                                onArtistClick(song.artist)
                            }
                        }
                    )
                }

                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) FlowAccent else FlowTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Slider
            val progressFraction = if (totalMs > 0) {
                (displayedPosMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = if (isSeeking) seekProgress else progressFraction,
                onValueChange = {
                    isSeeking = true
                    seekProgress = it
                },
                onValueChangeFinished = {
                    val targetMs = (seekProgress * totalMs).toInt()
                    onSeekTo(targetMs)
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = FlowPrimary,
                    activeTrackColor = FlowPrimary,
                    inactiveTrackColor = FlowCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Duration labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SongUtils.formatDuration(displayedPosMs / 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowTextSecondary
                )
                Text(
                    text = SongUtils.formatDuration(totalMs / 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls Row: Shuffle, Previous, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) FlowPrimary else FlowTextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = FlowTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Button (Large circular)
                Surface(
                    shape = CircleShape,
                    color = FlowPrimary,
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(8.dp, CircleShape)
                        .clickable { onPlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = FlowTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) FlowPrimary else FlowTextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Volume bar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isMuted || volume == 0f -> Icons.Default.VolumeMute
                            volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "Mute",
                        tint = FlowTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = FlowPrimary,
                        activeTrackColor = FlowPrimary,
                        inactiveTrackColor = FlowCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: Add to Playlist, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = FlowTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = FlowTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
