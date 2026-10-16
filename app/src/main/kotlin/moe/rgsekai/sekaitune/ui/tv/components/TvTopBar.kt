/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.ui.component.GlassComponent
import moe.rgsekai.sekaitune.ui.component.LocalGlassEffectConfig
import moe.rgsekai.sekaitune.ui.component.isGlassSupported
import moe.rgsekai.sekaitune.ui.component.liquidGlass
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
}

/**
 * Detached floating frosted glass pill top bar for TV:
 * Home, Explore, Library, Now Playing, Search, Settings.
 * - Detached from edges with floating top and horizontal margins.
 * - Rounded from both sides into a pill capsule container.
 * - Authentic frosted glass theme with translucent sheen, specular rim border, and ambient shadow.
 * - Focused item renders as a filled high-contrast pill.
 * - Down D-pad moves into content; Up D-pad from content returns to the top bar.
 */
@OptIn(ExperimentalComposeUiApi::class)
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
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val glassConfig = LocalGlassEffectConfig.current
    val useLiquidGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR) && isGlassSupported()

    val barShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 10.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Frosted Glass Menu Row Container (detached from screen edges, rounded from both sides)
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = barShape,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                )
                .clip(barShape)
                .then(
                    if (useLiquidGlass) {
                        Modifier.liquidGlass(
                            config = glassConfig,
                            shape = barShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .background(Color(0xB8161722)) // Deep translucent smoky glass base
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x38FFFFFF), // Top specular reflection highlight
                            Color(0x18FFFFFF), // Mid frosted glass sheen
                            Color(0x0AFFFFFF), // Lower translucent glass
                        ),
                    ),
                )
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.40f), // Crisp top light rim
                                Color.White.copy(alpha = 0.16f), // Subtle side rim
                                Color.White.copy(alpha = 0.08f), // Soft bottom rim
                            ),
                        ),
                    ),
                    shape = barShape,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.then(
                    if (selectedTabFocusRequester != null) {
                        Modifier.focusProperties {
                            enter = { selectedTabFocusRequester }
                        }
                    } else {
                        Modifier
                    },
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            isSelected -> Color.White.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        },
                        label = "tvTopBarBg",
                    )

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            isFocused -> MaterialTheme.colorScheme.onPrimary
                            isSelected -> Color.White
                            else -> Color.White.copy(alpha = 0.72f)
                        },
                        label = "tvTopBarContent",
                    )

                    val itemBorder = when {
                        isFocused -> null
                        isSelected -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        else -> null
                    }

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

                            val keyCode = keyEvent.nativeKeyEvent.keyCode
                            val isDown = keyEvent.key == Key.DirectionDown || keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                            val isLeft = keyEvent.key == Key.DirectionLeft || keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                            val isRight = keyEvent.key == Key.DirectionRight || keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                            val isUp = keyEvent.key == Key.DirectionUp || keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                            val isBack = keyEvent.key == Key.Escape || keyEvent.key == Key.Back || keyCode == android.view.KeyEvent.KEYCODE_BACK || keyCode == android.view.KeyEvent.KEYCODE_ESCAPE

                            when {
                                isLeft -> {
                                    if (index == 0) true else false
                                }
                                isRight -> {
                                    if (index == tabs.size - 1) true else false
                                }
                                isUp -> {
                                    true // Contain focus, don't escape off-screen
                                }
                                isDown -> {
                                    val moved = downFocusRequester?.safeRequestFocus() ?: false
                                    if (!moved) {
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                    true
                                }
                                isBack -> {
                                    if (backDispatcher != null) {
                                        backDispatcher.onBackPressed()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        }
                        .clip(pillShape)
                        .background(backgroundColor, pillShape)
                        .then(
                            if (itemBorder != null) {
                                Modifier.border(itemBorder, pillShape)
                            } else {
                                Modifier
                            },
                        )
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
                                    val moved = downFocusRequester?.safeRequestFocus() ?: false
                                    if (!moved) {
                                        selectedTabFocusRequester?.safeRequestFocus()
                                    }
                                } else {
                                    onTabSelected(tab)
                                }
                            },
                        )
                        .focusable(interactionSource = interactionSource)
                        .padding(horizontal = 16.dp, vertical = 9.dp)

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
}
