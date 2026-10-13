/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus

/**
 * Common data model for a card item in a TV shelf row.
 */
data class TvShelfItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val isRound: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Horizontal TV shelf displaying a list of [TvShelfItem]s with safe D-pad boundary containment.
 */
@Composable
fun TvShelfRow(
    title: String,
    items: List<TvShelfItem>,
    modifier: Modifier = Modifier,
    isFirstShelf: Boolean = false,
    isLastShelf: Boolean = false,
    upFocusRequester: FocusRequester? = null,
    firstItemFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = 32.dp,
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 6.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.id}_$index" },
            ) { index, item ->
                val itemModifier = Modifier
                    .then(
                        if (index == 0 && firstItemFocusRequester != null) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        when (keyEvent.key) {
                            Key.DirectionLeft -> {
                                if (index == 0) {
                                    if (leftFocusRequester != null) {
                                        leftFocusRequester.safeRequestFocus()
                                        true
                                    } else {
                                        // At the left end of the list: consume key to retain focus
                                        true
                                    }
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (index == items.size - 1) {
                                    // At the right end of the list: consume key to retain focus
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionUp -> {
                                if (isFirstShelf && upFocusRequester != null) {
                                    // Returning up to the TV top bar safely
                                    val moved = upFocusRequester.safeRequestFocus()
                                    if (moved) true else false
                                } else {
                                    false
                                }
                            }
                            Key.DirectionDown -> {
                                if (isLastShelf) {
                                    // At the last row: stay put and consume key
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }

                TvCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                    onClick = item.onClick,
                    isRound = item.isRound,
                    modifier = itemModifier,
                )
            }
        }
    }
}
