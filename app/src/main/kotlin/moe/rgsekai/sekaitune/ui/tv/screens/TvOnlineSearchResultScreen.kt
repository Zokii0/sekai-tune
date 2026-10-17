/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import moe.rgsekai.sekaitune.LocalPlayerConnection
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.extensions.toMediaItem
import moe.rgsekai.sekaitune.extensions.togglePlayPause
import moe.rgsekai.sekaitune.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import moe.rgsekai.sekaitune.innertube.models.AlbumItem
import moe.rgsekai.sekaitune.innertube.models.ArtistItem
import moe.rgsekai.sekaitune.innertube.models.PlaylistItem
import moe.rgsekai.sekaitune.innertube.models.SongItem
import moe.rgsekai.sekaitune.innertube.models.WatchEndpoint
import moe.rgsekai.sekaitune.innertube.models.YTItem
import moe.rgsekai.sekaitune.models.toMediaMetadata
import moe.rgsekai.sekaitune.playback.queues.YouTubeQueue
import moe.rgsekai.sekaitune.ui.component.EmptyPlaceholder
import moe.rgsekai.sekaitune.ui.component.shimmer.ListItemPlaceHolder
import moe.rgsekai.sekaitune.ui.component.shimmer.ShimmerHost
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenu
import moe.rgsekai.sekaitune.ui.tv.components.TvSongMenuTarget
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.ui.utils.tvFocusOutline
import moe.rgsekai.sekaitune.utils.joinByBullet
import moe.rgsekai.sekaitune.utils.makeTimeString
import moe.rgsekai.sekaitune.viewmodels.OnlineSearchViewModel

/**
 * TV-optimized Search Results screen:
 * - Automatically locks search filter to Song (FILTER_SONG).
 * - Completely removes filter chips row and container.
 * - Completely removes "Top results" header.
 * - Removes dead space above content so song results begin directly below TV top bar.
 * - Custom TV song row with polished focus outline, generous internal padding, and background highlight.
 * - Supports TV D-pad focus, remote playback control, and context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvOnlineSearchResultScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    // Automatically set filter to Songs
    remember(viewModel) {
        if (viewModel.filter.value != FILTER_SONG) {
            viewModel.filter.value = FILTER_SONG
        }
    }

    LaunchedEffect(viewModel) {
        if (viewModel.filter.value != FILTER_SONG) {
            viewModel.filter.value = FILTER_SONG
        }
    }

    val playerConnection = LocalPlayerConnection.current
    val isPlaying by playerConnection?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }
    val searchSort by viewModel.sort.collectAsStateWithLifecycle()

    val itemsPage = viewModel.viewStateMap[FILTER_SONG.value]
    val songItems = remember(itemsPage?.items, searchSort) {
        viewModel.sortedItems(itemsPage?.items.orEmpty().distinctBy { it.id }, searchSort)
    }

    val lazyListState = rememberLazyListState()
    var selectedSongForMenu by remember { mutableStateOf<SongItem?>(null) }

    // Pagination: load more songs when scrolling near end
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading_more" }
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                viewModel.loadMore()
            }
        }
    }

    // Auto-focus the first song item once loaded
    var hasRequestedInitialFocus by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(songItems.isNotEmpty()) {
        if (songItems.isNotEmpty() && !hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            delay(100)
            firstFocusRequester?.safeRequestFocus()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (itemsPage == null) {
            // Initial loading shimmer
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 48.dp,
                    end = 48.dp,
                    top = 16.dp,
                    bottom = 48.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = 8) {
                    ShimmerHost {
                        ListItemPlaceHolder()
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        } else if (songItems.isEmpty()) {
            // Empty results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmptyPlaceholder(
                    icon = R.drawable.search,
                    text = stringResource(R.string.no_results_found),
                )
            }
        } else {
            // Song results list
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 48.dp,
                    end = 48.dp,
                    top = 76.dp,
                    bottom = 48.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = songItems,
                    key = { _, item -> item.id },
                ) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == songItems.lastIndex
                    val defaultRowFocusRequester = remember { FocusRequester() }
                    val itemFocusRequester = if (isFirst) {
                        firstFocusRequester ?: defaultRowFocusRequester
                    } else {
                        defaultRowFocusRequester
                    }

                    val isActive = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    }

                    TvSearchResultSongRow(
                        item = item,
                        index = index,
                        isLast = isLast,
                        isActive = isActive,
                        isPlaying = isPlaying,
                        focusRequester = itemFocusRequester,
                        upFocusRequester = upFocusRequester,
                        onClick = {
                            if (playerConnection != null) {
                                when (item) {
                                    is SongItem -> {
                                        if (isActive) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    item.endpoint ?: WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata(),
                                                ),
                                            )
                                        }
                                    }
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            }
                        },
                        onLongClick = {
                            if (item is SongItem) {
                                selectedSongForMenu = item
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (itemsPage.continuation != null) {
                    item(key = "loading_more") {
                        ShimmerHost {
                            repeat(3) {
                                ListItemPlaceHolder()
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Context / Action menu for selected song row
    selectedSongForMenu?.let { song ->
        TvSongMenu(
            target = TvSongMenuTarget(
                id = song.id,
                title = song.title,
                artistsText = song.artists.joinToString { it.name },
                artistId = song.artists.firstOrNull()?.id,
                albumId = song.album?.id,
                albumTitle = song.album?.name,
                thumbnailUrl = song.thumbnail,
                shareUrl = song.shareLink,
                mediaItem = song.toMediaMetadata().toMediaItem(),
            ),
            navController = navController,
            onDismiss = { selectedSongForMenu = null },
        )
    }
}

/**
 * Focusable, TV-tailored song row designed specifically for TV search results:
 * - Rounded focus border outline that doesn't clip the thumbnail or screen edges.
 * - Smooth animated background highlight when focused.
 * - Comfortable internal padding (16dp horizontal, 8dp vertical).
 * - Full remote control support (OK/Enter to play, Menu/Long-press for options, D-pad navigation).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvSearchResultSongRow(
    item: YTItem,
    index: Int,
    isLast: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            isActive -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "tvSearchRowBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onSurface
            isActive -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 150),
        label = "tvSearchRowContentColor",
    )

    val subtitleColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            isActive -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "tvSearchRowSubtitleColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val keyCode = keyEvent.nativeKeyEvent.keyCode
                when {
                    keyEvent.key == Key.DirectionUp || keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                        if (index == 0 && upFocusRequester != null) {
                            upFocusRequester.safeRequestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    keyEvent.key == Key.DirectionDown || keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isLast) {
                            true
                        } else {
                            false
                        }
                    }
                    keyEvent.key == Key.DirectionLeft || keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> true
                    keyEvent.key == Key.DirectionRight || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> true
                    keyEvent.key == Key.Menu || keyCode == KeyEvent.KEYCODE_MENU -> {
                        onLongClick()
                        true
                    }
                    else -> false
                }
            }
            .clip(shape)
            .background(backgroundColor, shape)
            .tvFocusOutline(
                shape = shape,
                borderWidth = 2.dp,
                focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                scaleOnFocus = 1.0f,
                interactionSource = interactionSource,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.play else R.drawable.pause),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Title and Subtitle
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isFocused || isActive) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = when (item) {
                is SongItem -> joinByBullet(
                    item.artists.joinToString { it.name },
                    makeTimeString(item.duration?.times(1000L)),
                    item.viewCountText,
                )
                is AlbumItem -> joinByBullet(item.artists?.joinToString { it.name }, item.year?.toString())
                is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                else -> null
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Trailing 3-dots context menu icon
        IconButton(
            onClick = onLongClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                tint = subtitleColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
