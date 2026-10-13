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
import moe.rgsekai.sekaitune.playback.queues.LocalAlbumRadio
import moe.rgsekai.sekaitune.ui.tv.components.TvDetailLayout
import moe.rgsekai.sekaitune.ui.tv.components.TvSongItem
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenu
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenuTarget
import moe.rgsekai.sekaitune.utils.makeTimeString
import moe.rgsekai.sekaitune.viewmodels.AlbumUiState
import moe.rgsekai.sekaitune.viewmodels.AlbumViewModel

/**
 * TV Detail Screen for Albums (`album/{albumId}`).
 */
@Composable
fun TvAlbumScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current
    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isPlaying by playerConnection?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }

    val songs = remember(albumWithSongs, mediaMetadata, isPlaying) {
        val currentAlbum = albumWithSongs ?: return@remember emptyList()
        currentAlbum.songs.mapIndexed { index, song ->
            val isActive = mediaMetadata?.id == song.id
            val isCurrentPlaying = isActive && isPlaying

            TvSongItem(
                id = song.id,
                title = song.song.title,
                subtitle = song.artists.joinToString { it.name }.ifEmpty { currentAlbum.album.title },
                thumbnailUrl = song.song.thumbnailUrl ?: currentAlbum.album.thumbnailUrl,
                durationText = makeTimeString(song.song.duration * 1000L),
                isActive = isActive,
                isPlaying = isCurrentPlaying,
                onClick = {
                    if (playerConnection != null) {
                        if (isActive) {
                            if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                        } else {
                            playerConnection.playQueue(
                                LocalAlbumRadio(currentAlbum, startIndex = index),
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
        title = albumWithSongs?.album?.title.orEmpty(),
        subtitle = albumWithSongs?.artists?.joinToString { it.name },
        thumbnailUrl = albumWithSongs?.album?.thumbnailUrl,
        songs = songs,
        isLoading = uiState is AlbumUiState.Loading && albumWithSongs == null,
        emptyMessage = stringResource(R.string.album_not_found),
        coverPlaceholderIcon = R.drawable.album,
        onPlayClick = {
            albumWithSongs?.let { albumData ->
                if (albumData.songs.isNotEmpty()) {
                    playerConnection?.playQueue(LocalAlbumRadio(albumData))
                }
            }
        },
        onShuffleClick = {
            albumWithSongs?.let { albumData ->
                if (albumData.songs.isNotEmpty()) {
                    playerConnection?.playQueue(
                        LocalAlbumRadio(albumData.copy(songs = albumData.songs.shuffled())),
                    )
                }
            }
        },
        playEnabled = albumWithSongs?.songs?.isNotEmpty() == true,
        shuffleEnabled = albumWithSongs?.songs?.isNotEmpty() == true,
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
                albumId = albumWithSongs?.album?.id,
                albumTitle = albumWithSongs?.album?.title,
                thumbnailUrl = song.song.thumbnailUrl ?: albumWithSongs?.album?.thumbnailUrl,
                shareUrl = "https://music.youtube.com/watch?v=${song.id}",
                mediaItem = song.toMediaItem(),
            ),
            navController = navController,
            onDismiss = { selectedSongForMenu = null },
        )
    }
}
