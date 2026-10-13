/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import moe.rgsekai.sekaitune.BuildConfig
import moe.rgsekai.sekaitune.playback.PlayerConnection
import moe.rgsekai.sekaitune.ui.screens.LOGIN_ROUTE
import moe.rgsekai.sekaitune.ui.screens.Screens
import moe.rgsekai.sekaitune.ui.screens.navigationBuilder
import moe.rgsekai.sekaitune.ui.tv.components.TvTab
import moe.rgsekai.sekaitune.ui.tv.components.TvTopBar
import moe.rgsekai.sekaitune.ui.tv.screens.TvArtistScreen
import moe.rgsekai.sekaitune.ui.tv.screens.TvHomeScreen
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus
import moe.rgsekai.sekaitune.viewmodels.HomeViewModel

const val TV_HOME_ROUTE = "tv_home"

/**
 * Root container for the TV UI.
 * Reuses the application's existing [NavHostController] and [PlayerConnection].
 * Displays the top pill bar and routes TV/placeholder screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvRoot(
    navController: NavHostController,
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val topBarFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: TV_HOME_ROUTE

    // Request initial focus on the top bar
    LaunchedEffect(Unit) {
        topBarFocusRequester.safeRequestFocus()
    }

    val selectedTab = when {
        currentRoute == TV_HOME_ROUTE || currentRoute == Screens.Home.route -> TvTab.HOME
        currentRoute == Screens.NewRelease.route || currentRoute == Screens.MoodAndGenres.route -> TvTab.EXPLORE
        currentRoute == Screens.Library.route -> TvTab.LIBRARY
        currentRoute == "ambient_mode" -> TvTab.NOW_PLAYING
        currentRoute == Screens.Search.route || currentRoute.startsWith("online_search") -> TvTab.SEARCH
        currentRoute == "settings" || currentRoute.startsWith("settings/") -> TvTab.SETTINGS
        currentRoute == LOGIN_ROUTE || currentRoute == "account" || currentRoute == "settings/account" -> TvTab.PROFILE
        else -> TvTab.HOME
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // TV Top Navigation Bar
        TvTopBar(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                when (tab) {
                    TvTab.HOME -> {
                        if (currentRoute != TV_HOME_ROUTE) {
                            navController.navigate(TV_HOME_ROUTE) {
                                popUpTo(TV_HOME_ROUTE) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    TvTab.EXPLORE -> {
                        navController.navigate(Screens.NewRelease.route) {
                            launchSingleTop = true
                        }
                    }
                    TvTab.LIBRARY -> {
                        navController.navigate(Screens.Library.route) {
                            launchSingleTop = true
                        }
                    }
                    TvTab.NOW_PLAYING -> {
                        navController.navigate("ambient_mode") {
                            launchSingleTop = true
                        }
                    }
                    TvTab.SEARCH -> {
                        navController.navigate(Screens.Search.route) {
                            launchSingleTop = true
                        }
                    }
                    TvTab.SETTINGS -> {
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                    TvTab.PROFILE -> {
                        navController.navigate("settings/account") {
                            launchSingleTop = true
                        }
                    }
                }
            },
            firstItemFocusRequester = topBarFocusRequester,
            selectedTabFocusRequester = topBarFocusRequester,
            downFocusRequester = contentFocusRequester,
            modifier = Modifier.fillMaxWidth(),
        )

        // TV Content Area: hosts NavHost with TvHomeScreen as TV start destination
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .tvContentFocusHandoff(
                    upFocusRequester = topBarFocusRequester,
                    contentFocusRequester = contentFocusRequester,
                ),
        ) {
            NavHost(
                navController = navController,
                startDestination = TV_HOME_ROUTE,
                enterTransition = { fadeIn(tween(150)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(150)) },
                popExitTransition = { fadeOut(tween(150)) },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(TV_HOME_ROUTE) {
                    TvHomeScreen(
                        homeViewModel = homeViewModel,
                        playerConnection = playerConnection,
                        navController = navController,
                        firstShelfFocusRequester = contentFocusRequester,
                        upFocusRequester = topBarFocusRequester,
                    )
                }

                // Reuse existing application navigation graph for placeholder screens and sub-screens
                navigationBuilder(
                    navController = navController,
                    scrollBehavior = topAppBarScrollBehavior,
                    latestVersionName = { BuildConfig.VERSION_NAME },
                )

                // TV-optimized Artist screen
                composable(
                    route = "artist/{artistId}",
                    arguments = listOf(
                        navArgument("artistId") {
                            type = NavType.StringType
                        },
                    ),
                ) {
                    TvArtistScreen(
                        navController = navController,
                        playerConnection = playerConnection,
                        upFocusRequester = topBarFocusRequester,
                    )
                }
            }
        }
    }
}

/**
 * Focus handoff helper connecting the TV top bar and content area:
 * - Directs focus to [contentFocusRequester] or allows Down focus movement into the container.
 * - Sets focusProperties.up to [upFocusRequester] (the selected top bar tab).
 * - On unconsumed Up key event at the top of content, returns focus safely to [upFocusRequester].
 * - Never crashes if the content screen has nothing focusable.
 */
fun Modifier.tvContentFocusHandoff(
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester? = null,
): Modifier {
    return this
        .then(
            if (contentFocusRequester != null) {
                Modifier.focusRequester(contentFocusRequester)
            } else {
                Modifier
            },
        )
        .focusProperties {
            up = upFocusRequester
        }
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                upFocusRequester.safeRequestFocus()
            } else {
                false
            }
        }
}
