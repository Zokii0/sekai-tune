/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rgsekai.sekaitune.LocalDatabase
import moe.rgsekai.sekaitune.LocalPlayerConnection
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.db.entities.PlaylistSongMap
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.ui.utils.tvFocusOutline

/**
 * Metadata bundle needed by [TvSongMenu].
 */
data class TvSongMenuTarget(
    val id: String,
    val title: String,
    val artistsText: String? = null,
    val artistId: String? = null,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val thumbnailUrl: String? = null,
    val shareUrl: String? = null,
    val mediaItem: MediaItem? = null,
    val focusRequester: FocusRequester? = null,
)

private data class TvSongMenuAction(
    val iconRes: Int,
    val title: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Centered TV dialog opened by long-press OK / Menu key on a song row.
 * Entries:
 * 1. Play next
 * 2. Add to queue
 * 3. Add to playlist
 * 4. Go to artist
 * 5. Go to album
 * 6. Share
 *
 * Focus behavior:
 * - Focus starts on the first entry.
 * - Up/Down moves through entries; boundaries keep focus inside.
 * - Back closes dialog and safely returns focus to the calling row.
 */
@Composable
fun TvSongMenu(
    target: TvSongMenuTarget,
    navController: NavController,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current

    val firstItemFocusRequester = remember { FocusRequester() }
    val playlistFirstItemFocusRequester = remember { FocusRequester() }

    var isChoosingPlaylist by remember { mutableStateOf(false) }

    fun safeDismissAndRestore() {
        onDismiss()
        coroutineScope.launch {
            delay(50)
            target.focusRequester?.safeRequestFocus()
        }
    }

    BackHandler {
        if (isChoosingPlaylist) {
            isChoosingPlaylist = false
        } else {
            safeDismissAndRestore()
        }
    }

    LaunchedEffect(isChoosingPlaylist) {
        delay(60)
        if (isChoosingPlaylist) {
            playlistFirstItemFocusRequester.safeRequestFocus()
        } else {
            firstItemFocusRequester.safeRequestFocus()
        }
    }

    Dialog(
        onDismissRequest = { safeDismissAndRestore() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .width(420.dp)
                .heightIn(max = 520.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Back || keyEvent.key == Key.Escape)) {
                        if (isChoosingPlaylist) {
                            isChoosingPlaylist = false
                            true
                        } else {
                            safeDismissAndRestore()
                            true
                        }
                    } else {
                        false
                    }
                },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                // Header (Thumbnail + Title + Subtitle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (target.thumbnailUrl != null) {
                        val imageRequest = remember(target.thumbnailUrl) {
                            ImageRequest.Builder(context)
                                .data(target.thumbnailUrl)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = target.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!target.artistsText.isNullOrBlank()) {
                            Text(
                                text = target.artistsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                if (isChoosingPlaylist) {
                    // --- SUBMENU: Select Playlist ---
                    val playlists by database.playlistsByCreateDateAsc().collectAsStateWithLifecycle(initialValue = emptyList())

                    Text(
                        text = stringResource(R.string.add_to_playlist),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )

                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_playlists_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(playlists, key = { _, pl -> pl.playlist.id }) { index, playlist ->
                                val isFirst = index == 0
                                val isLast = index == playlists.size - 1

                                TvSongMenuRow(
                                    iconRes = R.drawable.playlist_add,
                                    title = playlist.playlist.name,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    focusRequester = if (isFirst) playlistFirstItemFocusRequester else null,
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val maxPos = database.maxPlaylistSongPosition(playlist.playlist.id) ?: -1
                                            database.insert(
                                                PlaylistSongMap(
                                                    playlistId = playlist.playlist.id,
                                                    songId = target.id,
                                                    position = maxPos + 1,
                                                ),
                                            )
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.added_to_playlist, playlist.playlist.name),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                                safeDismissAndRestore()
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                } else {
                    // --- PRIMARY 6 ACTIONS ---
                    val playNextTitle = stringResource(R.string.play_next)
                    val addToQueueTitle = stringResource(R.string.add_to_queue)
                    val addToPlaylistTitle = stringResource(R.string.add_to_playlist)
                    val viewArtistTitle = stringResource(R.string.view_artist)
                    val viewAlbumTitle = stringResource(R.string.view_album)
                    val shareTitle = stringResource(R.string.share)

                    val actions = remember(target, playNextTitle, addToQueueTitle, addToPlaylistTitle, viewArtistTitle, viewAlbumTitle, shareTitle) {
                        listOf(
                            TvSongMenuAction(
                                iconRes = R.drawable.playlist_play,
                                title = playNextTitle,
                                onClick = {
                                    target.mediaItem?.let {
                                        playerConnection?.playNext(it)
                                        Toast.makeText(context, playNextTitle, Toast.LENGTH_SHORT).show()
                                    }
                                    safeDismissAndRestore()
                                },
                            ),
                            TvSongMenuAction(
                                iconRes = R.drawable.queue_music,
                                title = addToQueueTitle,
                                onClick = {
                                    target.mediaItem?.let {
                                        playerConnection?.addToQueue(it)
                                        Toast.makeText(context, addToQueueTitle, Toast.LENGTH_SHORT).show()
                                    }
                                    safeDismissAndRestore()
                                },
                            ),
                            TvSongMenuAction(
                                iconRes = R.drawable.playlist_add,
                                title = addToPlaylistTitle,
                                onClick = {
                                    isChoosingPlaylist = true
                                },
                            ),
                            TvSongMenuAction(
                                iconRes = R.drawable.artist,
                                title = viewArtistTitle,
                                enabled = target.artistId != null,
                                onClick = {
                                    if (target.artistId != null) {
                                        safeDismissAndRestore()
                                        navController.navigate("artist/${target.artistId}")
                                    }
                                },
                            ),
                            TvSongMenuAction(
                                iconRes = R.drawable.album,
                                title = viewAlbumTitle,
                                enabled = target.albumId != null,
                                onClick = {
                                    if (target.albumId != null) {
                                        safeDismissAndRestore()
                                        navController.navigate("album/${target.albumId}")
                                    }
                                },
                            ),
                            TvSongMenuAction(
                                iconRes = R.drawable.share,
                                title = shareTitle,
                                onClick = {
                                    safeDismissAndRestore()
                                    val shareUrl = target.shareUrl ?: "https://music.youtube.com/watch?v=${target.id}"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                },
                            ),
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(actions) { index, action ->
                            val isFirst = index == 0
                            val isLast = index == actions.size - 1

                            TvSongMenuRow(
                                iconRes = action.iconRes,
                                title = action.title,
                                enabled = action.enabled,
                                isFirst = isFirst,
                                isLast = isLast,
                                focusRequester = if (isFirst) firstItemFocusRequester else null,
                                onClick = action.onClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Focusable row item inside [TvSongMenu].
 */
@Composable
private fun TvSongMenuRow(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "menuRowBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            isFocused -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "menuRowContent",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionUp -> {
                        if (isFirst) {
                            true // Stay put on top
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        if (isLast) {
                            true // Stay put on bottom
                        } else {
                            false
                        }
                    }
                    Key.DirectionLeft, Key.DirectionRight -> {
                        true // Consume horizontal movement inside dialog
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(backgroundColor, shape)
            .tvFocusOutline(
                shape = shape,
                borderWidth = 1.75.dp,
                focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                scaleOnFocus = 1.02f,
                interactionSource = interactionSource,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource, enabled = enabled)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
