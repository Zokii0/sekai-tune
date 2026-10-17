/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.extensions.toMediaItem
import moe.rgsekai.sekaitune.home.HomeScreenState
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
import moe.rgsekai.sekaitune.viewmodels.HomeViewModel

/**
 * TV Home Screen built for horizontal shelves and D-pad navigation.
 * Uses existing [HomeViewModel] and [PlayerConnection].
 */
@Composable
fun TvHomeScreen(
    homeViewModel: HomeViewModel,
    playerConnection: PlayerConnection?,
    navController: NavController,
    modifier: Modifier = Modifier,
    firstShelfFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    val screenState by homeViewModel.screenState.collectAsState()
    val listState = rememberLazyListState()

    when (val state = screenState) {
        HomeScreenState.Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        HomeScreenState.Empty -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                Text(
                    text = "No items available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is HomeScreenState.Error -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                Text(
                    text = stringResource(state.messageResId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        is HomeScreenState.Success -> {
            val uiState = state.uiState

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 76.dp, bottom = 48.dp),
                modifier = modifier.fillMaxSize(),
            ) {
                var isFirstRenderedShelf = true

                // 1. Quick Picks Shelf
                if (uiState.quickPicks.isNotEmpty()) {
                    val shelfFirst = isFirstRenderedShelf
                    isFirstRenderedShelf = false

                    item(key = "shelf_quick_picks") {
                        val items = uiState.quickPicks.map { song ->
                            TvShelfItem(
                                id = song.song.id,
                                title = song.song.title,
                                subtitle = song.artists.joinToString { it.name },
                                thumbnailUrl = song.song.thumbnailUrl,
                                onClick = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = "Quick Picks",
                                            items = listOf(song.toMediaItem()),
                                        ),
                                    )
                                },
                            )
                        }

                        TvShelfRow(
                            title = stringResource(R.string.quick_picks),
                            items = items,
                            isFirstShelf = shelfFirst,
                            upFocusRequester = upFocusRequester,
                            firstItemFocusRequester = if (shelfFirst) firstShelfFocusRequester else null,
                        )
                    }
                }

                // 2. Forgotten Favorites Shelf
                if (uiState.forgottenFavorites.isNotEmpty()) {
                    val shelfFirst = isFirstRenderedShelf
                    isFirstRenderedShelf = false

                    item(key = "shelf_forgotten_favorites") {
                        val items = uiState.forgottenFavorites.map { song ->
                            TvShelfItem(
                                id = song.song.id,
                                title = song.song.title,
                                subtitle = song.artists.joinToString { it.name },
                                thumbnailUrl = song.song.thumbnailUrl,
                                onClick = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = "Forgotten Favorites",
                                            items = listOf(song.toMediaItem()),
                                        ),
                                    )
                                },
                            )
                        }

                        TvShelfRow(
                            title = stringResource(R.string.forgotten_favorites),
                            items = items,
                            isFirstShelf = shelfFirst,
                            upFocusRequester = upFocusRequester,
                            firstItemFocusRequester = if (shelfFirst) firstShelfFocusRequester else null,
                        )
                    }
                }

                // 3. Similar Recommendations Shelves
                uiState.similarRecommendations.forEachIndexed { index, recommendation ->
                    val shelfFirst = isFirstRenderedShelf
                    isFirstRenderedShelf = false

                    item(key = "shelf_similar_${recommendation.title.id}_$index") {
                        val items = recommendation.items.map { ytItem ->
                            ytItemToShelfItem(ytItem, playerConnection, navController)
                        }

                        TvShelfRow(
                            title = recommendation.title.title,
                            items = items,
                            isFirstShelf = shelfFirst,
                            upFocusRequester = upFocusRequester,
                            firstItemFocusRequester = if (shelfFirst) firstShelfFocusRequester else null,
                        )
                    }
                }

                // 4. Remote YouTube Home Sections
                uiState.homePage?.sections.orEmpty().forEachIndexed { index, section ->
                    if (section.items.isNotEmpty()) {
                        val shelfFirst = isFirstRenderedShelf
                        isFirstRenderedShelf = false

                        val sectionKey = "remote_${section.endpoint?.browseId ?: section.title}_$index"

                        item(key = sectionKey) {
                            val items = section.items.map { ytItem ->
                                ytItemToShelfItem(ytItem, playerConnection, navController)
                            }

                            TvShelfRow(
                                title = section.title,
                                items = items,
                                isFirstShelf = shelfFirst,
                                upFocusRequester = upFocusRequester,
                                firstItemFocusRequester = if (shelfFirst) firstShelfFocusRequester else null,
                            )
                        }
                    }
                }
            }
        }
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
