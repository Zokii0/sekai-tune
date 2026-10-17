/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.rgsekai.sekaitune.LocalPlayerConnection
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.db.entities.Song
import moe.rgsekai.sekaitune.extensions.toMediaItem
import moe.rgsekai.sekaitune.playback.queues.ListQueue
import moe.rgsekai.sekaitune.ui.tv.TV_HOME_ROUTE
import moe.rgsekai.sekaitune.ui.tv.components.TvDetailLayout
import moe.rgsekai.sekaitune.ui.tv.components.TvSongItem
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenu
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenuTarget
import moe.rgsekai.sekaitune.utils.makeTimeString
import moe.rgsekai.sekaitune.viewmodels.LocalPlaylistViewModel

/**
 * TV Detail Screen for Local Playlists (`local_playlist/{playlistId}`).
 */
@Composable
fun TvLocalPlaylistScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val playlistWithSongs by viewModel.playlist.collectAsStateWithLifecycle()
    val playlistSongs by viewModel.playlistSongs.collectAsStateWithLifecycle()

    val isPlaying by playerConnection?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }

    val playlistName = playlistWithSongs?.playlist?.name.orEmpty()
    val coverThumbnail = playlistWithSongs?.playlist?.thumbnailUrl
        ?: playlistSongs.firstOrNull()?.song?.song?.thumbnailUrl

    val songs = remember(playlistSongs, mediaMetadata, isPlaying, playlistName) {
        playlistSongs.mapIndexed { index, playlistSong ->
            val song = playlistSong.song
            val isActive = mediaMetadata?.id == song.id
            val isCurrentPlaying = isActive && isPlaying

            TvSongItem(
                id = song.id,
                title = song.song.title,
                subtitle = song.artists.joinToString { it.name }.ifEmpty { playlistName },
                thumbnailUrl = song.song.thumbnailUrl,
                durationText = makeTimeString(song.song.duration * 1000L),
                isActive = isActive,
                isPlaying = isCurrentPlaying,
                onClick = {
                    if (playerConnection != null) {
                        if (isActive) {
                            if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                        } else {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = playlistName,
                                    items = playlistSongs.map { it.song.toMediaItem() },
                                    startIndex = index,
                                ),
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
        title = playlistName,
        subtitle = stringResource(R.string.playlists),
        thumbnailUrl = coverThumbnail,
        songs = songs,
        isLoading = playlistWithSongs == null,
        emptyMessage = stringResource(R.string.playlist_is_empty),
        coverPlaceholderIcon = R.drawable.queue_music,
        onPlayClick = {
            if (playlistSongs.isNotEmpty()) {
                playerConnection?.playQueue(
                    ListQueue(
                        title = playlistName,
                        items = playlistSongs.map { it.song.toMediaItem() },
                    ),
                )
            }
        },
        onShuffleClick = {
            if (playlistSongs.isNotEmpty()) {
                playerConnection?.playQueue(
                    ListQueue(
                        title = playlistName,
                        items = playlistSongs.map { it.song.toMediaItem() }.shuffled(),
                    ),
                )
            }
        },
        playEnabled = playlistSongs.isNotEmpty(),
        shuffleEnabled = playlistSongs.isNotEmpty(),
        upFocusRequester = upFocusRequester,
        onBack = {
            if (!navController.popBackStack()) {
                navController.navigate(TV_HOME_ROUTE) {
                    popUpTo(TV_HOME_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
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
