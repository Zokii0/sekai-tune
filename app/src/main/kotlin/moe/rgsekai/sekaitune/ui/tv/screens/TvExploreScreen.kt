/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.ui.tv.components.TvShelfItem
import moe.rgsekai.sekaitune.ui.tv.components.TvShelfRow
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.viewmodels.NewReleaseUiState
import moe.rgsekai.sekaitune.viewmodels.NewReleaseViewModel

private data class TvExploreSection(
    val key: String,
    val title: String,
    val items: List<TvShelfItem>,
)

/**
 * TV Explore Screen displaying New Releases shelf rows directly
 * without mobile-only top bar, back arrow, or summary cards.
 */
@Composable
fun TvExploreScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    firstShelfFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        delay(60)
        firstShelfFocusRequester?.safeRequestFocus()
    }

    LaunchedEffect(uiState) {
        if (uiState is NewReleaseUiState.Success) {
            delay(50)
            firstShelfFocusRequester?.safeRequestFocus()
        }
    }

    when (val state = uiState) {
        NewReleaseUiState.Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        NewReleaseUiState.Empty -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                Text(
                    text = stringResource(R.string.no_results_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        NewReleaseUiState.Error -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "New releases are temporarily unavailable",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "SekaiTune could not load this YouTube Music section. Try again later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = viewModel::retry,
                        modifier = Modifier.then(
                            if (firstShelfFocusRequester != null) {
                                Modifier.focusRequester(firstShelfFocusRequester)
                            } else Modifier,
                        ),
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        is NewReleaseUiState.Success -> {
            val albumsTitle = stringResource(R.string.albums)
            val singlesTitle = stringResource(R.string.singles)
            val epsTitle = stringResource(R.string.ep)

            val sections = remember(state.content, albumsTitle, singlesTitle, epsTitle) {
                buildList {
                    if (state.content.albums.isNotEmpty()) {
                        add(
                            TvExploreSection(
                                key = "explore_albums",
                                title = albumsTitle,
                                items = state.content.albums.map { album ->
                                    TvShelfItem(
                                        id = album.id,
                                        title = album.title,
                                        subtitle = album.artists.orEmpty().joinToString { it.name }.ifEmpty { album.year?.toString() },
                                        thumbnailUrl = album.thumbnail,
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                    )
                                },
                            ),
                        )
                    }
                    if (state.content.singles.isNotEmpty()) {
                        add(
                            TvExploreSection(
                                key = "explore_singles",
                                title = singlesTitle,
                                items = state.content.singles.map { album ->
                                    TvShelfItem(
                                        id = album.id,
                                        title = album.title,
                                        subtitle = album.artists.orEmpty().joinToString { it.name }.ifEmpty { album.year?.toString() },
                                        thumbnailUrl = album.thumbnail,
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                    )
                                },
                            ),
                        )
                    }
                    if (state.content.eps.isNotEmpty()) {
                        add(
                            TvExploreSection(
                                key = "explore_eps",
                                title = epsTitle,
                                items = state.content.eps.map { album ->
                                    TvShelfItem(
                                        id = album.id,
                                        title = album.title,
                                        subtitle = album.artists.orEmpty().joinToString { it.name }.ifEmpty { album.year?.toString() },
                                        thumbnailUrl = album.thumbnail,
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                    )
                                },
                            ),
                        )
                    }
                }
            }

            if (sections.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier.fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                    modifier = modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = sections,
                        key = { _, section -> section.key },
                    ) { index, section ->
                        val isFirst = index == 0
                        val isLast = index == sections.size - 1

                        TvShelfRow(
                            title = section.title,
                            items = section.items,
                            isFirstShelf = isFirst,
                            isLastShelf = isLast,
                            upFocusRequester = upFocusRequester,
                            firstItemFocusRequester = if (isFirst) firstShelfFocusRequester else null,
                        )
                    }
                }
            }
        }
    }
}
