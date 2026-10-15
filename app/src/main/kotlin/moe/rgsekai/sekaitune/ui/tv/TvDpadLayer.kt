/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import moe.rgsekai.sekaitune.ui.screens.LOGIN_ROUTE
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.rgsekai.sekaitune.ui.utils.LocalIsTv
import moe.rgsekai.sekaitune.ui.utils.safeRequestFocus

/**
 * Custom TV Indication implemented via [IndicationNodeFactory] that delegates
 * press feedback to the standard ripple while drawing a clean TV focus outline
 * whenever an interactive element receives focus.
 */
class TvOutlineIndication(
    private val delegateIndication: IndicationNodeFactory,
    private val borderColor: Color,
    private val borderWidth: Dp = 2.dp,
    private val cornerRadius: Dp = 12.dp,
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): Modifier.Node {
        return TvOutlineIndicationNode(
            interactionSource = interactionSource,
            delegateIndication = delegateIndication,
            borderColor = borderColor,
            borderWidth = borderWidth,
            cornerRadius = cornerRadius,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TvOutlineIndication) return false
        return delegateIndication == other.delegateIndication &&
            borderColor == other.borderColor &&
            borderWidth == other.borderWidth &&
            cornerRadius == other.cornerRadius
    }

    override fun hashCode(): Int {
        var result = delegateIndication.hashCode()
        result = 31 * result + borderColor.hashCode()
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        return result
    }
}

private class TvOutlineIndicationNode(
    private val interactionSource: InteractionSource,
    private val delegateIndication: IndicationNodeFactory,
    private val borderColor: Color,
    private val borderWidth: Dp,
    private val cornerRadius: Dp,
) : DelegatingNode(), DrawModifierNode {

    private var isFocused: Boolean = false

    init {
        delegate(delegateIndication.create(interactionSource))
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> {
                        isFocused = true
                        invalidateDraw()
                    }
                    is FocusInteraction.Unfocus -> {
                        isFocused = false
                        invalidateDraw()
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (isFocused) {
            val strokePx = borderWidth.toPx()
            val radiusPx = cornerRadius.toPx()
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(size.width - strokePx, size.height - strokePx),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx),
            )
        }
    }
}

/**
 * Generic D-pad navigation layer applied once around the TV content area.
 *
 * Responsibilities:
 * 1. Provides a unified [LocalIndication] (press ripple + TV focus outline) for all child screens.
 * 2. Fallback key handler using [onKeyEvent] (bubble phase, so child screens' handlers win):
 *    - Unconsumed Up/Down/Left/Right moves focus using [FocusDirection].
 *    - Unconsumed Up at the top of content moves focus to [upFocusRequester] (selected top bar tab).
 *    - Unconsumed Down/Left/Right at bounds consumes the key so focus is never lost.
 * 3. Initial focus: after each navigation destination change, if no child has focus, gently requests
 *    focus on the first focusable item in the content area without stealing focus from screens with
 *    their own focus logic.
 * 4. Back navigation: handles back gracefully (pop to previous screen, go to Home tab, or exit on Home).
 */
@Composable
fun TvDpadLayer(
    navController: NavController,
    currentRoute: String?,
    upFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!LocalIsTv.current) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? Activity

    val isSettingsRoute = currentRoute == "settings" || currentRoute?.startsWith("settings/") == true || currentRoute == "stats"
    val isLoginRoute = currentRoute == LOGIN_ROUTE || currentRoute?.startsWith("$LOGIN_ROUTE?") == true || currentRoute?.contains("login") == true
    var isContentFocused by remember { mutableStateOf(false) }

    val isTopBarRoute = currentRoute == TV_HOME_ROUTE ||
        currentRoute == "new_release" ||
        currentRoute == "mood_and_genres" ||
        currentRoute == "library" ||
        currentRoute == "ambient_mode" ||
        currentRoute == "search" ||
        currentRoute == "settings" ||
        currentRoute == "settings/account"

    // Initial focus on route change: wait for composition and let child screens set their own focus first
    LaunchedEffect(currentRoute) {
        if (isLoginRoute) return@LaunchedEffect
        delay(60)
        if (!isTopBarRoute && !isContentFocused) {
            val requested = contentFocusRequester?.safeRequestFocus() ?: false
            if (!requested) {
                focusManager.moveFocus(FocusDirection.Down)
            }
        }
    }

    // Root TV BackHandler: pop backstack, navigate to Home tab, or finish
    BackHandler(enabled = !isLoginRoute) {
        val canPop = navController.previousBackStackEntry != null
        val isHomeRoute = currentRoute == TV_HOME_ROUTE

        if (canPop && !isHomeRoute) {
            val popped = navController.popBackStack()
            if (!popped && currentRoute != TV_HOME_ROUTE) {
                navController.navigate(TV_HOME_ROUTE) {
                    popUpTo(TV_HOME_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (!isHomeRoute) {
            navController.navigate(TV_HOME_ROUTE) {
                popUpTo(TV_HOME_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            activity?.finish()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val defaultRipple = ripple()
    val tvIndication = remember(primaryColor, defaultRipple) {
        TvOutlineIndication(
            delegateIndication = defaultRipple,
            borderColor = primaryColor.copy(alpha = 0.85f),
            borderWidth = 2.dp,
            cornerRadius = 12.dp,
        )
    }

    CompositionLocalProvider(
        LocalIndication provides tvIndication,
    ) {
        Box(
            modifier = modifier
                .onFocusChanged { focusState ->
                    isContentFocused = focusState.hasFocus
                }
                .onKeyEvent { keyEvent ->
                    if (isLoginRoute) return@onKeyEvent false
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionUp -> {
                            val moved = focusManager.moveFocus(FocusDirection.Up)
                            if (moved) {
                                true
                            } else if (!isSettingsRoute) {
                                // Nothing focusable above in the content area:
                                // Move focus up to the selected top bar tab
                                upFocusRequester.safeRequestFocus()
                                true
                            } else {
                                true
                            }
                        }
                        Key.DirectionDown -> {
                            focusManager.moveFocus(FocusDirection.Down)
                            // Consume to retain focus and never lose it at end of list
                            true
                        }
                        Key.DirectionLeft -> {
                            focusManager.moveFocus(FocusDirection.Left)
                            // Consume to retain focus and never lose it at left edge
                            true
                        }
                        Key.DirectionRight -> {
                            focusManager.moveFocus(FocusDirection.Right)
                            // Consume to retain focus and never lose it at right edge
                            true
                        }
                        else -> false
                    }
                },
        ) {
            content()
        }
    }
}
