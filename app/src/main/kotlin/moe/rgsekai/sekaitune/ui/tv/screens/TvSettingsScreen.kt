/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.rgsekai.sekaitune.BuildConfig
import moe.rgsekai.sekaitune.R
import moe.rgsekai.sekaitune.ui.component.LocalTvSettingsCategoryFocusRequester
import moe.rgsekai.sekaitune.ui.screens.StatsScreen
import moe.rgsekai.sekaitune.ui.screens.settings.AboutScreen
import moe.rgsekai.sekaitune.ui.screens.settings.AccountSettings
import moe.rgsekai.sekaitune.ui.screens.settings.AdvancedSettings
import moe.rgsekai.sekaitune.ui.screens.settings.AmbientModeSettingsScreen
import moe.rgsekai.sekaitune.ui.screens.settings.AodCustomizedScreen
import moe.rgsekai.sekaitune.ui.screens.settings.AppearanceSettings
import moe.rgsekai.sekaitune.ui.screens.settings.BackupAndRestore
import moe.rgsekai.sekaitune.ui.screens.settings.CanvasSettings
import moe.rgsekai.sekaitune.ui.screens.settings.ChangelogScreen
import moe.rgsekai.sekaitune.ui.screens.settings.ChiperSettings
import moe.rgsekai.sekaitune.ui.screens.settings.ContentSettings
import moe.rgsekai.sekaitune.ui.screens.settings.DebugSettings
import moe.rgsekai.sekaitune.ui.screens.settings.GlassEffectSettings
import moe.rgsekai.sekaitune.ui.screens.settings.IconScreen
import moe.rgsekai.sekaitune.ui.screens.settings.IntegrationsScreen
import moe.rgsekai.sekaitune.ui.screens.settings.InternetSettings
import moe.rgsekai.sekaitune.ui.screens.settings.LogcatScreen
import moe.rgsekai.sekaitune.ui.screens.settings.LyricsAnimationSettings
import moe.rgsekai.sekaitune.ui.screens.settings.LyricsSettings
import moe.rgsekai.sekaitune.ui.screens.settings.MusicTogetherScreen
import moe.rgsekai.sekaitune.ui.screens.settings.PalettePickerScreen
import moe.rgsekai.sekaitune.ui.screens.settings.PlayerSettings
import moe.rgsekai.sekaitune.ui.screens.settings.PoTokenScreen
import moe.rgsekai.sekaitune.ui.screens.settings.PrivacySettings
import moe.rgsekai.sekaitune.ui.screens.settings.StorageSettings
import moe.rgsekai.sekaitune.ui.screens.settings.SupportScreen
import moe.rgsekai.sekaitune.ui.screens.settings.ThemeCreatorScreen
import moe.rgsekai.sekaitune.ui.screens.settings.UpdateScreen
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.ui.utils.tvFocusOutline

/**
 * TV Settings categories matching phone Settings screen order.
 */
enum class TvSettingsCategory(
    val route: String,
) {
    ACCOUNT("settings/account"),
    STATS("stats"),
    APPEARANCE("settings/appearance"),
    CANVAS("settings/canvas"),
    PLAYER("settings/player"),
    LYRICS("settings/lyrics"),
    CONTENT("settings/content"),
    PRIVACY("settings/privacy"),
    STORAGE("settings/storage"),
    BACKUP_RESTORE("settings/backup_restore"),
    INTEGRATIONS("settings/integrations"),
    INTERNET("settings/internet"),
    ADVANCED("settings/advanced"),
    ABOUT("settings/about"),
}

/**
 * TV split-screen Settings layout (BitChord style):
 * - Left column (~30% width): Vertical list of categories in phone settings order.
 * - Right pane (~70% width): Embedded settings composables with TopAppBar hidden.
 * - Moving Up/Down in the left column changes the right pane preview without stealing focus.
 * - OK or Right moves focus into the right pane.
 * - Left from the right pane returns focus to the selected category.
 * - Up from the first category returns focus to the top bar Settings tab.
 * - Down at the last category stays put and consumes key.
 * - Back key: returns from right pane to left column, or from left column to previous screen.
 */
@Composable
fun TvSettingsScreen(
    navController: NavController,
    upFocusRequester: FocusRequester,
    initialCategory: TvSettingsCategory = TvSettingsCategory.ACCOUNT,
    activeSubRoute: String? = null,
    modifier: Modifier = Modifier,
) {
    val categories = remember { TvSettingsCategory.entries }
    var selectedIndex by remember { mutableIntStateOf(categories.indexOf(initialCategory).coerceAtLeast(0)) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val categoryFocusRequesters = remember {
        List(categories.size) { FocusRequester() }
    }

    var isRightPaneFocused by remember { mutableStateOf(false) }

    // Intercept back key when focus is in the right pane to return to the left category column
    BackHandler(enabled = isRightPaneFocused) {
        categoryFocusRequesters[selectedIndex].safeRequestFocus()
    }

    // Initial focus on the selected category row on entry
    LaunchedEffect(Unit) {
        if (categoryFocusRequesters.isNotEmpty()) {
            categoryFocusRequesters[selectedIndex].safeRequestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Left Column: ~30% width with category list
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.30f)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(top = 16.dp, start = 20.dp, end = 12.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
            )

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(categories) { index, category ->
                    val isSelected = index == selectedIndex
                    val isFirst = index == 0
                    val isLast = index == categories.size - 1

                    TvSettingsCategoryRow(
                        category = category,
                        isSelected = isSelected,
                        focusRequester = categoryFocusRequesters[index],
                        onFocused = {
                            selectedIndex = index
                        },
                        onMoveRight = {
                            focusManager.moveFocus(FocusDirection.Right)
                        },
                        onUpFromFirst = {
                            upFocusRequester.safeRequestFocus()
                        },
                        isFirst = isFirst,
                        isLast = isLast,
                    )
                }
            }
        }

        // Subtle vertical separator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        )

        // Right Pane: ~70% width with embedded category content
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.70f)
                .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                .onFocusChanged {
                    isRightPaneFocused = it.hasFocus
                },
        ) {
            val currentCategory = categories[selectedIndex]

            CompositionLocalProvider(
                LocalTvSettingsCategoryFocusRequester provides categoryFocusRequesters[selectedIndex],
            ) {
                TvSettingsSubScreenWrapper(
                    shiftTopBar = activeSubRoute != null || currentCategory != TvSettingsCategory.STATS,
                ) {
                    when {
                        activeSubRoute != null -> {
                            TvSettingsSubRouteContent(
                                subRoute = activeSubRoute,
                                navController = navController,
                            )
                        }
                        else -> {
                            TvSettingsCategoryContent(
                                category = currentCategory,
                                navController = navController,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual category row in the TV Settings left column.
 */
@Composable
private fun TvSettingsCategoryRow(
    category: TvSettingsCategory,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onMoveRight: () -> Unit,
    onUpFromFirst: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isItemFocused by interactionSource.collectIsFocusedAsState()

    val title = when (category) {
        TvSettingsCategory.ACCOUNT -> stringResource(R.string.account)
        TvSettingsCategory.STATS -> stringResource(R.string.settings_stats_title)
        TvSettingsCategory.APPEARANCE -> stringResource(R.string.appearance)
        TvSettingsCategory.CANVAS -> stringResource(R.string.SekaiTune_canvas)
        TvSettingsCategory.PLAYER -> stringResource(R.string.settings_playback_title)
        TvSettingsCategory.LYRICS -> stringResource(R.string.lyrics)
        TvSettingsCategory.CONTENT -> stringResource(R.string.content)
        TvSettingsCategory.PRIVACY -> stringResource(R.string.privacy)
        TvSettingsCategory.STORAGE -> stringResource(R.string.storage)
        TvSettingsCategory.BACKUP_RESTORE -> stringResource(R.string.backup_restore)
        TvSettingsCategory.INTEGRATIONS -> stringResource(R.string.integrations)
        TvSettingsCategory.INTERNET -> stringResource(R.string.internet)
        TvSettingsCategory.ADVANCED -> stringResource(R.string.advanced)
        TvSettingsCategory.ABOUT -> stringResource(R.string.about)
    }

    val icon: Painter = when (category) {
        TvSettingsCategory.ACCOUNT -> painterResource(R.drawable.account)
        TvSettingsCategory.STATS -> painterResource(R.drawable.stats)
        TvSettingsCategory.APPEARANCE -> painterResource(R.drawable.palette)
        TvSettingsCategory.CANVAS -> painterResource(R.drawable.slow_motion_video)
        TvSettingsCategory.PLAYER -> painterResource(R.drawable.music_note)
        TvSettingsCategory.LYRICS -> painterResource(R.drawable.lyrics)
        TvSettingsCategory.CONTENT -> painterResource(R.drawable.language)
        TvSettingsCategory.PRIVACY -> painterResource(R.drawable.security)
        TvSettingsCategory.STORAGE -> painterResource(R.drawable.storage)
        TvSettingsCategory.BACKUP_RESTORE -> painterResource(R.drawable.backup)
        TvSettingsCategory.INTEGRATIONS -> painterResource(R.drawable.integration)
        TvSettingsCategory.INTERNET -> painterResource(R.drawable.wifi_proxy)
        TvSettingsCategory.ADVANCED -> painterResource(R.drawable.tune)
        TvSettingsCategory.ABOUT -> painterResource(R.drawable.info)
    }

    val itemShape = RoundedCornerShape(12.dp)

    val containerColor by animateColorAsState(
        targetValue = when {
            isItemFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "categoryContainerColor",
    )

    val contentColor = when {
        isItemFocused || isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = itemShape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .tvFocusOutline(shape = itemShape)
            .focusable(interactionSource = interactionSource)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocused()
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionUp -> {
                        if (isFirst) {
                            onUpFromFirst()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        if (isLast) {
                            // Down at the last category stays put and consumes key
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.Spacebar, Key.NumPadEnter -> {
                        onMoveRight()
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onMoveRight,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || isItemFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isItemFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * TV wrapper that clips and offsets the standard TopAppBar off-screen so embedded screens
 * render cleanly without trapping D-pad focus on their back arrow button.
 */
@Composable
fun TvSettingsSubScreenWrapper(
    modifier: Modifier = Modifier,
    shiftTopBar: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        if (shiftTopBar) {
            Layout(
                content = content,
                modifier = Modifier.fillMaxSize(),
            ) { measurables, constraints ->
                val topBarHeight = 64.dp.roundToPx()
                val placeable = measurables.first().measure(
                    constraints.copy(
                        minHeight = constraints.maxHeight + topBarHeight,
                        maxHeight = constraints.maxHeight + topBarHeight,
                    ),
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(0, -topBarHeight)
                }
            }
        } else {
            content()
        }
    }
}

/**
 * Renders the content of the selected settings category by embedding the existing composable.
 */
@Composable
private fun TvSettingsCategoryContent(
    category: TvSettingsCategory,
    navController: NavController,
) {
    when (category) {
        TvSettingsCategory.ACCOUNT -> AccountSettings(navController, latestVersionName = BuildConfig.VERSION_NAME)
        TvSettingsCategory.STATS -> StatsScreen(navController)
        TvSettingsCategory.APPEARANCE -> AppearanceSettings(navController)
        TvSettingsCategory.CANVAS -> CanvasSettings(navController)
        TvSettingsCategory.PLAYER -> PlayerSettings(navController)
        TvSettingsCategory.LYRICS -> LyricsSettings(navController)
        TvSettingsCategory.CONTENT -> ContentSettings(navController)
        TvSettingsCategory.PRIVACY -> PrivacySettings(navController)
        TvSettingsCategory.STORAGE -> StorageSettings(navController)
        TvSettingsCategory.BACKUP_RESTORE -> BackupAndRestore(navController)
        TvSettingsCategory.INTEGRATIONS -> IntegrationsScreen(navController)
        TvSettingsCategory.INTERNET -> InternetSettings(navController)
        TvSettingsCategory.ADVANCED -> AdvancedSettings(navController)
        TvSettingsCategory.ABOUT -> AboutScreen(navController)
    }
}

/**
 * Renders an embedded sub-route destination in the TV settings right pane.
 */
@Composable
private fun TvSettingsSubRouteContent(
    subRoute: String,
    navController: NavController,
) {
    when (subRoute) {
        "settings/appearance/liquid_glass" -> GlassEffectSettings(navController)
        "settings/appearance/ambient_mode" -> AmbientModeSettingsScreen(navController)
        "settings/appearance/icon" -> IconScreen(navController)
        "settings/appearance/aod_customized" -> AodCustomizedScreen(navController)
        "settings/appearance/palette_picker" -> PalettePickerScreen(navController)
        "settings/appearance/lyrics_animations" -> LyricsAnimationSettings(navController)
        "settings/appearance/theme_creator" -> ThemeCreatorScreen(navController)
        "settings/player/chiper" -> ChiperSettings(navController)
        "settings/music_together" -> MusicTogetherScreen(navController)
        "settings/misc" -> DebugSettings(navController)
        "settings/logcat" -> LogcatScreen(navController)
        "settings/po_token" -> PoTokenScreen(navController)
        "settings/changelog" -> ChangelogScreen(navController)
        "settings/update" -> UpdateScreen(navController)
        "settings/support" -> SupportScreen(navController)
        else -> AboutScreen(navController)
    }
}
