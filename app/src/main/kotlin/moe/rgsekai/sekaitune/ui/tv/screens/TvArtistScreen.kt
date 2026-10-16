/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.ui.tv.TV_HOME_ROUTE
import moe.rgsekai.sekaitune.extensions.toMediaItem
import moe.rgsekai.sekaitune.innertube.models.AlbumItem
import moe.rgsekai.sekaitune.innertube.models.ArtistItem
import moe.rgsekai.sekaitune.innertube.models.PlaylistItem
import moe.rgsekai.sekaitune.innertube.models.SongItem
import moe.rgsekai.sekaitune.innertube.models.WatchEndpoint
import moe.rgsekai.sekaitune.innertube.models.YTItem
import moe.rgsekai.sekaitune.models.toMediaMetadata
import moe.rgsekai.sekaitune.playback.PlayerConnection
import moe.rgsekai.sekaitune.playback.queues.ListQueue
import moe.rgsekai.sekaitune.playback.queues.YouTubeQueue
import moe.rgsekai.sekaitune.ui.tv.components.TvShelfItem
import moe.rgsekai.sekaitune.ui.tv.components.TvShelfRow
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.ui.utils.tvFocusOutline
import moe.rgsekai.sekaitune.viewmodels.ArtistBlockState
import moe.rgsekai.sekaitune.viewmodels.ArtistViewModel

private data class TvArtistShelfData(
    val title: String,
    val items: List<TvShelfItem>,
)

/**
 * Android TV Artist Screen:
 * - Left pane: Circular artist image, artist name, and Play / Shuffle / Radio action buttons.
 * - Right pane: Vertically scrolling shelves (Songs, Albums, Singles, Related artists).
 * - Fixed layout (not collapsing, no top app bar that depends on scroll).
 * - Initial focus on Play button; Back key pops navigation; Up at top returns to top bar;
 *   Down at last row stays put and consumes key.
 */
@Composable
fun TvArtistScreen(
    navController: NavController,
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val playButtonFocusRequester = remember { FocusRequester() }
    val shuffleButtonFocusRequester = remember { FocusRequester() }
    val radioButtonFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var lastFocusedActionRequester by remember { mutableStateOf(playButtonFocusRequester) }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Intercept hardware and remote Back key
    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate(TV_HOME_ROUTE) {
                popUpTo(TV_HOME_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Collect data from existing ArtistViewModel
    val loadedArtistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsStateWithLifecycle()
    val loadedLibrarySongs by viewModel.librarySongs.collectAsStateWithLifecycle()
    val loadedLibraryAlbums by viewModel.libraryAlbums.collectAsStateWithLifecycle()
    val blockState by viewModel.blockState.collectAsStateWithLifecycle()
    val isArtistBlocked = (blockState as? ArtistBlockState.Success)?.isBlocked == true

    val artistPage = remember(loadedArtistPage, isArtistBlocked) {
        if (isArtistBlocked) {
            loadedArtistPage?.copy(
                artist = loadedArtistPage.artist.copy(
                    playEndpoint = null,
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                ),
                sections = emptyList(),
            )
        } else {
            loadedArtistPage
        }
    }

    val librarySongs = remember(loadedLibrarySongs, isArtistBlocked) {
        loadedLibrarySongs.takeUnless { isArtistBlocked }.orEmpty()
    }
    val libraryAlbums = remember(loadedLibraryAlbums, isArtistBlocked) {
        loadedLibraryAlbums.takeUnless { isArtistBlocked }.orEmpty()
    }

    val artistName = artistPage?.artist?.title
        ?: libraryArtist?.artist?.name
        ?: stringResource(R.string.unknown_artist)
    val thumbnailUrl = artistPage?.artist?.thumbnail
        ?: libraryArtist?.artist?.thumbnailUrl
    val subtitleText = artistPage?.artist?.subscriberCountText
        ?: artistPage?.artist?.monthlyListenerCountText

    // Request initial focus on the Play button
    LaunchedEffect(Unit) {
        if (!playButtonFocusRequester.safeRequestFocus()) {
            delay(50)
            playButtonFocusRequester.safeRequestFocus()
        }
    }

    // Build section shelf data from YouTube Music or local library fallback
    val displaySections = remember(artistPage, librarySongs, libraryAlbums) {
        val ytmSections = artistPage?.sections.orEmpty().filter { it.items.isNotEmpty() }
        if (ytmSections.isNotEmpty()) {
            ytmSections.map { section ->
                TvArtistShelfData(
                    title = section.title,
                    items = section.items.map { ytItemToShelfItem(it, playerConnection, navController) },
                )
            }
        } else if (librarySongs.isNotEmpty() || libraryAlbums.isNotEmpty()) {
            buildList {
                if (librarySongs.isNotEmpty()) {
                    add(
                        TvArtistShelfData(
                            title = "Songs",
                            items = librarySongs.map { song ->
                                TvShelfItem(
                                    id = song.song.id,
                                    title = song.song.title,
                                    subtitle = song.artists.joinToString { it.name },
                                    thumbnailUrl = song.song.thumbnailUrl,
                                    onClick = {
                                        playerConnection?.playQueue(
                                            ListQueue(
                                                title = artistName,
                                                items = librarySongs.map { it.toMediaItem() },
                                            ),
                                        )
                                    },
                                )
                            },
                        ),
                    )
                }
                if (libraryAlbums.isNotEmpty()) {
                    add(
                        TvArtistShelfData(
                            title = "Albums",
                            items = libraryAlbums.map { album ->
                                TvShelfItem(
                                    id = album.album.id,
                                    title = album.album.title,
                                    subtitle = album.artists.joinToString { it.name },
                                    thumbnailUrl = album.album.thumbnailUrl,
                                    onClick = {
                                        navController.navigate("album/${album.album.id}")
                                    },
                                )
                            },
                        ),
                    )
                }
            }
        } else {
            emptyList()
        }
    }

    val shelfFocusRequesters = remember(displaySections.size) {
        List(displaySections.size) { FocusRequester() }
    }
    val shelfListStates = remember(displaySections.size) {
        List(displaySections.size) { LazyListState() }
    }
    var lastFocusedShelfIndex by remember { mutableIntStateOf(0) }
    var lastFocusedShelfItemRequester by remember { mutableStateOf<FocusRequester?>(null) }

    val focusShelfFromActionButtons: () -> Boolean = {
        val exactMoved = lastFocusedShelfItemRequester?.safeRequestFocus() ?: false
        if (exactMoved) {
            true
        } else {
            val sectionIndex = lastFocusedShelfIndex.coerceIn(0, (displaySections.size - 1).coerceAtLeast(0))
            val currentShelfRequester = shelfFocusRequesters.getOrNull(sectionIndex)
            val currentShelfMoved = currentShelfRequester?.safeRequestFocus() ?: false
            if (currentShelfMoved) {
                true
            } else if (sectionIndex != 0 && shelfFocusRequesters.getOrNull(0)?.safeRequestFocus() == true) {
                true
            } else if (displaySections.isNotEmpty()) {
                coroutineScope.launch {
                    listState.scrollToItem(sectionIndex)
                    shelfListStates.getOrNull(sectionIndex)?.scrollToItem(0)
                    delay(50)
                    val moved = shelfFocusRequesters.getOrNull(sectionIndex)?.safeRequestFocus() ?: false
                    if (!moved && sectionIndex != 0) {
                        listState.scrollToItem(0)
                        shelfListStates.getOrNull(0)?.scrollToItem(0)
                        delay(50)
                        shelfFocusRequesters.getOrNull(0)?.safeRequestFocus()
                    }
                }
                true
            } else {
                true
            }
        }
    }

    val hasRadio = artistPage?.artist?.radioEndpoint != null

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    if (backDispatcher != null) {
                        backDispatcher.onBackPressed()
                    } else if (!navController.popBackStack()) {
                        navController.navigate(TV_HOME_ROUTE) {
                            popUpTo(TV_HOME_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    true
                } else {
                    false
                }
            },
    ) {
        // --- LEFT PANE (Circle Image, Name, Play / Shuffle / Radio) ---
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .padding(start = 32.dp, end = 20.dp, top = 76.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Artist Image (Circle)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnailUrl != null) {
                    val context = LocalContext.current
                    val imageRequest = remember(thumbnailUrl) {
                        ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Artist Name
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            if (!subtitleText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Play, Shuffle, Radio
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. Play Button (Initial focus target)
                TvActionButton(
                    iconRes = R.drawable.play,
                    text = stringResource(R.string.play),
                    focusRequester = playButtonFocusRequester,
                    onFocused = { lastFocusedActionRequester = playButtonFocusRequester },
                    onPreviewKeyEvent = { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@TvActionButton false
                        when (keyEvent.key) {
                            Key.DirectionUp -> {
                                upFocusRequester?.safeRequestFocus() ?: true
                            }
                            Key.DirectionDown -> {
                                shuffleButtonFocusRequester.safeRequestFocus()
                            }
                            Key.DirectionLeft -> {
                                true // Stay put
                            }
                            Key.DirectionRight -> {
                                focusShelfFromActionButtons()
                            }
                            else -> false
                        }
                    },
                    onClick = {
                        val playEndpoint = artistPage?.artist?.playEndpoint ?: artistPage?.artist?.shuffleEndpoint
                        if (playEndpoint != null) {
                            playerConnection?.playQueue(YouTubeQueue(playEndpoint))
                        } else {
                            val firstSong = artistPage?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.firstOrNull()
                            if (firstSong != null) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        firstSong.endpoint ?: WatchEndpoint(videoId = firstSong.id),
                                        firstSong.toMediaMetadata(),
                                    ),
                                )
                            } else if (librarySongs.isNotEmpty()) {
                                playerConnection?.playQueue(
                                    ListQueue(
                                        title = artistName,
                                        items = librarySongs.map { it.toMediaItem() },
                                    ),
                                )
                            }
                        }
                    },
                )

                // 2. Shuffle Button
                TvActionButton(
                    iconRes = R.drawable.shuffle,
                    text = stringResource(R.string.shuffle),
                    focusRequester = shuffleButtonFocusRequester,
                    onFocused = { lastFocusedActionRequester = shuffleButtonFocusRequester },
                    onPreviewKeyEvent = { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@TvActionButton false
                        when (keyEvent.key) {
                            Key.DirectionUp -> {
                                playButtonFocusRequester.safeRequestFocus()
                            }
                            Key.DirectionLeft -> {
                                true // Stay put
                            }
                            Key.DirectionRight -> {
                                focusShelfFromActionButtons()
                            }
                            Key.DirectionDown -> {
                                if (hasRadio) {
                                    radioButtonFocusRequester.safeRequestFocus()
                                } else {
                                    true // Last button when radio is unavailable, stay put and consume key
                                }
                            }
                            else -> false
                        }
                    },
                    onClick = {
                        val shuffleEndpoint = artistPage?.artist?.shuffleEndpoint ?: artistPage?.artist?.playEndpoint
                        if (shuffleEndpoint != null) {
                            playerConnection?.playQueue(YouTubeQueue(shuffleEndpoint))
                        } else if (librarySongs.isNotEmpty()) {
                            playerConnection?.playQueue(
                                ListQueue(
                                    title = artistName,
                                    items = librarySongs.shuffled().map { it.toMediaItem() },
                                ),
                            )
                        } else {
                            val songs = artistPage?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>().orEmpty()
                            if (songs.isNotEmpty()) {
                                val song = songs.shuffled().first()
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        song.endpoint ?: WatchEndpoint(videoId = song.id),
                                        song.toMediaMetadata(),
                                    ),
                                )
                            }
                        }
                    },
                )

                // 3. Radio Button
                TvActionButton(
                    iconRes = R.drawable.radio,
                    text = stringResource(R.string.radio),
                    focusRequester = radioButtonFocusRequester,
                    enabled = hasRadio,
                    onFocused = { lastFocusedActionRequester = radioButtonFocusRequester },
                    onPreviewKeyEvent = { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@TvActionButton false
                        when (keyEvent.key) {
                            Key.DirectionUp -> {
                                shuffleButtonFocusRequester.safeRequestFocus()
                            }
                            Key.DirectionLeft -> {
                                true // Stay put
                            }
                            Key.DirectionRight -> {
                                focusShelfFromActionButtons()
                            }
                            Key.DirectionDown -> {
                                true // Last row button: stays put and consumes key
                            }
                            else -> false
                        }
                    },
                    onClick = {
                        artistPage?.artist?.radioEndpoint?.let { endpoint ->
                            playerConnection?.playQueue(YouTubeQueue(endpoint))
                        }
                    },
                )
            }
        }

        // --- RIGHT PANE (Vertically scrolling list of sections) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            if (artistPage == null && librarySongs.isEmpty() && libraryAlbums.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (displaySections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No items available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 76.dp, bottom = 48.dp, end = 24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = displaySections,
                        key = { index, section -> "section_${section.title}_$index" },
                    ) { index, section ->
                        val isFirst = index == 0
                        val isLast = index == displaySections.size - 1
                        val shelfRequester = shelfFocusRequesters.getOrNull(index) ?: remember { FocusRequester() }
                        val shelfState = shelfListStates.getOrNull(index) ?: rememberLazyListState()

                        TvShelfRow(
                            title = section.title,
                            items = section.items,
                            isFirstShelf = isFirst,
                            isLastShelf = isLast,
                            upFocusRequester = upFocusRequester,
                            firstItemFocusRequester = shelfRequester,
                            leftFocusRequester = lastFocusedActionRequester,
                            lazyListState = shelfState,
                            onItemFocused = { requester ->
                                lastFocusedShelfIndex = index
                                lastFocusedShelfItemRequester = requester
                            },
                            horizontalPadding = 20.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Focus-aware TV pill action button with distinct outline and smooth color transitions.
 */
@Composable
private fun TvActionButton(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    onFocused: () -> Unit = {},
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(percent = 50)

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "actionBtnBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onPrimary
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "actionBtnContent",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent(onPreviewKeyEvent)
            .clip(shape)
            .background(backgroundColor, shape)
            .tvFocusOutline(
                shape = shape,
                borderWidth = 2.dp,
                focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                scaleOnFocus = 1.05f,
                interactionSource = interactionSource,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun ytItemToShelfItem(
    ytItem: YTItem,
    playerConnection: PlayerConnection?,
    navController: NavController,
): TvShelfItem {
    return TvShelfItem(
        id = ytItem.id,
        title = ytItem.title,
        subtitle = (ytItem as? SongItem)?.artists?.joinToString { it.name }
            ?: (ytItem as? AlbumItem)?.artists?.joinToString { it.name }
            ?: (ytItem as? PlaylistItem)?.author?.name,
        thumbnailUrl = ytItem.thumbnail,
        isRound = ytItem is ArtistItem,
        onClick = {
            when (ytItem) {
                is SongItem -> {
                    playerConnection?.playQueue(
                        YouTubeQueue(
                            ytItem.endpoint ?: WatchEndpoint(videoId = ytItem.id),
                            ytItem.toMediaMetadata(),
                        ),
                    )
                }
                is AlbumItem -> navController.navigate("album/${ytItem.id}")
                is ArtistItem -> navController.navigate("artist/${ytItem.id}")
                is PlaylistItem -> navController.navigate("online_playlist/${ytItem.id}")
            }
        },
    )
}
