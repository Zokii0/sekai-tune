/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.ui.utils.tvFocusOutline

/**
 * Model representing a focusable song row in TV detail lists.
 */
data class TvSongItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

/**
 * Shared TV Detail Layout for albums, playlists, and collection screens:
 * - Left pane: Cover art, title, subtitle, Play / Shuffle and optional extra action buttons.
 * - Right pane: Vertically scrolling song list, with each row focusable and D-pad compliant.
 * - Focus rules: First focus on Play, Up from top row returns to top bar tab,
 *   Down on last row stays put, Left from song row returns to left buttons, Back key pops navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvDetailLayout(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    songs: List<TvSongItem>,
    modifier: Modifier = Modifier,
    coverPlaceholderIcon: Int = R.drawable.album,
    isRoundCover: Boolean = false,
    isLoading: Boolean = false,
    emptyMessage: String = "No songs available",
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    playEnabled: Boolean = true,
    shuffleEnabled: Boolean = true,
    upFocusRequester: FocusRequester? = null,
    onBack: () -> Unit,
    extraButtons: @Composable (ColumnScope.(FocusRequester) -> Unit)? = null,
) {
    val playButtonFocusRequester = remember { FocusRequester() }
    val shuffleButtonFocusRequester = remember { FocusRequester() }
    val firstSongFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var lastFocusedActionRequester by remember { mutableStateOf(playButtonFocusRequester) }

    // Intercept hardware and remote Back key
    BackHandler {
        onBack()
    }

    // Initial focus on Play button
    LaunchedEffect(Unit) {
        if (!playButtonFocusRequester.safeRequestFocus()) {
            delay(50)
            playButtonFocusRequester.safeRequestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Back || keyEvent.key == Key.Escape)) {
                    onBack()
                    true
                } else {
                    false
                }
            },
    ) {
        // --- LEFT PANE (Cover Art, Title, Subtitle, Play / Shuffle / Extra Buttons) ---
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .padding(start = 32.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val coverShape = if (isRoundCover) CircleShape else RoundedCornerShape(14.dp)

            // Cover Image
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(coverShape)
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
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(coverPlaceholderIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(68.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Subtitle
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. Play Button
                TvDetailActionButton(
                    iconRes = R.drawable.play,
                    text = stringResource(R.string.play),
                    focusRequester = playButtonFocusRequester,
                    enabled = playEnabled,
                    onFocused = { lastFocusedActionRequester = playButtonFocusRequester },
                    onPreviewKeyEvent = { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@TvDetailActionButton false
                        when (keyEvent.key) {
                            Key.DirectionUp -> {
                                upFocusRequester?.safeRequestFocus() ?: true
                            }
                            Key.DirectionLeft -> {
                                true // Stay put
                            }
                            Key.DirectionRight -> {
                                val moved = firstSongFocusRequester.safeRequestFocus()
                                if (!moved) focusManager.moveFocus(FocusDirection.Right) else true
                            }
                            else -> false
                        }
                    },
                    onClick = onPlayClick,
                )

                // 2. Shuffle Button
                TvDetailActionButton(
                    iconRes = R.drawable.shuffle,
                    text = stringResource(R.string.shuffle),
                    focusRequester = shuffleButtonFocusRequester,
                    enabled = shuffleEnabled,
                    onFocused = { lastFocusedActionRequester = shuffleButtonFocusRequester },
                    onPreviewKeyEvent = { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@TvDetailActionButton false
                        when (keyEvent.key) {
                            Key.DirectionLeft -> {
                                true // Stay put
                            }
                            Key.DirectionRight -> {
                                val moved = firstSongFocusRequester.safeRequestFocus()
                                if (!moved) focusManager.moveFocus(FocusDirection.Right) else true
                            }
                            Key.DirectionDown -> {
                                if (extraButtons == null) {
                                    true // Last button, stays put
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    },
                    onClick = onShuffleClick,
                )

                // Extra buttons if provided
                extraButtons?.invoke(this, firstSongFocusRequester)
            }
        }

        // --- RIGHT PANE (Vertically scrolling song list) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { index, song -> "${song.id}_$index" },
                    ) { index, song ->
                        val isFirstSong = index == 0
                        val isLastSong = index == songs.size - 1

                        TvSongRow(
                            song = song,
                            index = index + 1,
                            isFirstSong = isFirstSong,
                            isLastSong = isLastSong,
                            firstSongFocusRequester = if (isFirstSong) firstSongFocusRequester else null,
                            upFocusRequester = upFocusRequester,
                            leftFocusRequester = lastFocusedActionRequester,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Focusable row for a song inside the TV detail screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvSongRow(
    song: TvSongItem,
    index: Int,
    isFirstSong: Boolean,
    isLastSong: Boolean,
    firstSongFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.primary
            song.isActive -> MaterialTheme.colorScheme.surfaceVariant
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "songRowBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onPrimary
            song.isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "songRowContent",
    )

    val subtitleColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "songRowSubtitle",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (firstSongFocusRequester != null) Modifier.focusRequester(firstSongFocusRequester) else Modifier)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionUp -> {
                        if (isFirstSong && upFocusRequester != null) {
                            upFocusRequester.safeRequestFocus()
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        if (isLastSong) {
                            true // Stay put on the last row
                        } else {
                            false
                        }
                    }
                    Key.DirectionLeft -> {
                        leftFocusRequester?.safeRequestFocus() ?: true
                    }
                    Key.DirectionRight -> {
                        true // Stay put
                    }
                    Key.Menu -> {
                        song.onLongClick?.invoke()
                        true
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
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = song.onClick,
                onLongClick = song.onLongClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Track Index / Active Indicator
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (song.isActive && song.isPlaying) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = subtitleColor,
                )
            }
        }

        // Small Thumbnail (optional)
        if (song.thumbnailUrl != null) {
            val context = LocalContext.current
            val imageRequest = remember(song.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Title and Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isFocused || song.isActive) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (!song.subtitle.isNullOrBlank()) {
                Text(
                    text = song.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Duration text
        if (!song.durationText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = song.durationText,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
            )
        }
    }
}

/**
 * TV action button with high contrast outline and focus state.
 */
@Composable
fun TvDetailActionButton(
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
        label = "detailActionBtnBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onPrimary
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "detailActionBtnContent",
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
