/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rgsekai.sekaitune.LocalPlayerConnection
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.db.entities.Song
import moe.rgsekai.sekaitune.extensions.toMediaItem
import moe.rgsekai.sekaitune.playback.queues.ListQueue
import moe.rgsekai.sekaitune.ui.tv.components.TvDetailLayout
import moe.rgsekai.sekaitune.ui.tv.components.TvSongItem
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenu
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenuTarget
import moe.rgsekai.sekaitune.utils.makeTimeString
import moe.rgsekai.sekaitune.viewmodels.AutoPlaylistViewModel

/**
 * TV Detail Screen for Auto-generated Playlists (`auto_playlist/{playlist}?tab={tab}`).
 * Handles "liked", "downloaded", and "uploaded" collections.
 */
@Composable
fun TvAutoPlaylistScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val songsList by viewModel.likedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val isPlaying by playerConnection?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }

    val playlistType = viewModel.playlist.lowercase()
    val playlistTitle = when (playlistType) {
        "liked" -> stringResource(R.string.filter_liked) + " " + stringResource(R.string.songs)
        "downloaded" -> stringResource(R.string.filter_downloaded) + " " + stringResource(R.string.songs)
        "uploaded" -> "Uploaded Songs"
        else -> viewModel.playlist.replaceFirstChar { it.uppercase() }
    }

    val placeholderIcon = when (playlistType) {
        "liked" -> R.drawable.favorite
        "downloaded" -> R.drawable.download
        "uploaded" -> R.drawable.cloud_upload
        else -> R.drawable.queue_music
    }

    val coverThumbnail = songsList.firstOrNull()?.song?.thumbnailUrl

    val songItems = remember(songsList, mediaMetadata, isPlaying, playlistTitle) {
        songsList.mapIndexed { index, song ->
            val isActive = mediaMetadata?.id == song.id
            val isCurrentPlaying = isActive && isPlaying

            TvSongItem(
                id = song.id,
                title = song.song.title,
                subtitle = song.artists.joinToString { it.name }.ifEmpty { playlistTitle },
                thumbnailUrl = song.song.thumbnailUrl,
                durationText = makeTimeString(song.song.duration * 1000L),
                isActive = isActive,
                isPlaying = isCurrentPlaying,
                onClick = {
                    if (playerConnection != null) {
                        if (isActive) {
                            if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                        } else {
                            viewModel.playTrack(
                                songs = songsList,
                                startIndex = index,
                                onToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                onReady = { mediaItems, idx ->
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = playlistTitle,
                                            items = mediaItems,
                                            startIndex = idx,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                },
                onLongClick = {
                    selectedSongForMenu = song
                },
            )
        }
    }

    TvDetailLayout(
        title = playlistTitle,
        subtitle = stringResource(R.string.playlists),
        thumbnailUrl = coverThumbnail,
        songs = songItems,
        isLoading = isRefreshing && songsList.isEmpty(),
        emptyMessage = stringResource(R.string.playlist_is_empty),
        coverPlaceholderIcon = placeholderIcon,
        onPlayClick = {
            if (songsList.isNotEmpty()) {
                viewModel.playAll(
                    songs = songsList,
                    shuffle = false,
                    onToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                    onReady = { mediaItems ->
                        playerConnection?.playQueue(
                            ListQueue(
                                title = playlistTitle,
                                items = mediaItems,
                            ),
                        )
                    },
                )
            }
        },
        onShuffleClick = {
            if (songsList.isNotEmpty()) {
                viewModel.playAll(
                    songs = songsList,
                    shuffle = true,
                    onToast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                    onReady = { mediaItems ->
                        playerConnection?.playQueue(
                            ListQueue(
                                title = playlistTitle,
                                items = mediaItems,
                            ),
                        )
                    },
                )
            }
        },
        playEnabled = songsList.isNotEmpty(),
        shuffleEnabled = songsList.isNotEmpty(),
        upFocusRequester = upFocusRequester,
        onBack = { navController.navigateUp() },
        modifier = modifier,
    )

    // Context / Action menu for selected song row
    selectedSongForMenu?.let { song ->
        TvSongMenu(
            target = TvSongMenuTarget(
                id = song.id,
                title = song.song.title,
                artistsText = song.artists.joinToString { it.name },
                artistId = song.artists.firstOrNull()?.id,
                albumId = song.song.albumId,
                thumbnailUrl = song.song.thumbnailUrl,
                shareUrl = "https://music.youtube.com/watch?v=${song.id}",
                mediaItem = song.toMediaItem(),
            ),
            navController = navController,
            onDismiss = { selectedSongForMenu = null },
        )
    }
}
