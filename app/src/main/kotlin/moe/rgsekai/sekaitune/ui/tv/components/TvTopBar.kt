/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.ui.utils.tvFocusOutline

enum class TvTab(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    HOME("Home", R.drawable.home_filled),
    EXPLORE("Explore", R.drawable.new_release),
    LIBRARY("Library", R.drawable.library_filled),
    NOW_PLAYING("Now Playing", R.drawable.play),
    SEARCH("Search", R.drawable.search),
    SETTINGS("Settings", R.drawable.settings),
    PROFILE("Profile", R.drawable.person),
}

/**
 * Top pill bar for TV: Home, Explore, Library, Now Playing, Search, Settings, Profile.
 * Focused item renders as a filled high-contrast pill.
 * Down D-pad moves into content; Up D-pad from content returns to the top bar.
 */
@Composable
fun TvTopBar(
    selectedTab: TvTab,
    onTabSelected: (TvTab) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    selectedTabFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    val tabs = remember { TvTab.values() }
    val focusManager = LocalFocusManager.current

    // Request focus on the active tab whenever selectedTab changes
    LaunchedEffect(selectedTab) {
        selectedTabFocusRequester?.safeRequestFocus()
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val isSelected = tab == selectedTab
                val defaultTabFocusRequester = remember { FocusRequester() }

                val pillShape = RoundedCornerShape(percent = 50)

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        isFocused -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color.Transparent
                    },
                    label = "tvTopBarBg",
                )

                val contentColor by animateColorAsState(
                    targetValue = when {
                        isFocused -> MaterialTheme.colorScheme.onPrimary
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "tvTopBarContent",
                )

                val pillModifier = Modifier
                    .then(
                        if (isSelected && selectedTabFocusRequester != null) {
                            Modifier.focusRequester(selectedTabFocusRequester)
                        } else if (index == 0 && selectedTabFocusRequester == null && firstItemFocusRequester != null) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier.focusRequester(defaultTabFocusRequester)
                        },
                    )
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        when (keyEvent.key) {
                            Key.DirectionLeft -> {
                                if (index == 0) true else false
                            }
                            Key.DirectionRight -> {
                                if (index == tabs.size - 1) true else false
                            }
                            Key.DirectionUp -> {
                                true // Contain focus, don't escape off-screen
                            }
                            Key.DirectionDown -> {
                                val moved = downFocusRequester?.safeRequestFocus() ?: false
                                if (moved) {
                                    true
                                } else {
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            }
                            Key.Back, Key.Escape -> {
                                val moved = downFocusRequester?.safeRequestFocus() ?: false
                                if (moved) {
                                    true
                                } else {
                                    val movedDown = focusManager.moveFocus(FocusDirection.Down)
                                    if (!movedDown) focusManager.moveFocus(FocusDirection.Next) else true
                                }
                            }
                            else -> false
                        }
                    }
                    .clip(pillShape)
                    .background(backgroundColor, pillShape)
                    .tvFocusOutline(
                        shape = pillShape,
                        borderWidth = 2.dp,
                        focusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                        scaleOnFocus = 1.05f,
                        interactionSource = interactionSource,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (isSelected) {
                                selectedTabFocusRequester?.safeRequestFocus()
                            } else {
                                onTabSelected(tab)
                            }
                        },
                    )
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 16.dp, vertical = 10.dp)

                Row(
                    modifier = pillModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
