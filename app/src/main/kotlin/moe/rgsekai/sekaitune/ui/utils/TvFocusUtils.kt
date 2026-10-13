/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.SystemClock
import android.view.KeyEvent
import android.webkit.WebView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Detects whether the device is an Android TV / Google TV.
 */
fun Context.isTvDevice(): Boolean {
    val isTelevisionUiMode =
        (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    return isTelevisionUiMode ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}

/**
 * A [WebView] that translates TV remote D-pad events into web-compatible navigation events:
 * - [KeyEvent.KEYCODE_DPAD_DOWN] -> [KeyEvent.KEYCODE_TAB] (advances focus to the next interactive web element)
 * - [KeyEvent.KEYCODE_DPAD_UP] -> Shift + [KeyEvent.KEYCODE_TAB] (moves focus backwards)
 * - [KeyEvent.KEYCODE_DPAD_CENTER] / [KeyEvent.KEYCODE_NUMPAD_ENTER] -> [KeyEvent.KEYCODE_ENTER] (activates focused element)
 *
 * No-op on mobile devices when [isTv] is false.
 */
open class TvDpadNavigableWebView(
    context: Context,
    val isTv: Boolean = false,
) : WebView(context) {
    init {
        if (isTv) {
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isTv) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
                        super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        val now = SystemClock.uptimeMillis()
                        super.dispatchKeyEvent(
                            KeyEvent(
                                now,
                                now,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_TAB,
                                0,
                                KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON,
                            ),
                        )
                        super.dispatchKeyEvent(
                            KeyEvent(
                                now,
                                now,
                                KeyEvent.ACTION_UP,
                                KeyEvent.KEYCODE_TAB,
                                0,
                                KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON,
                            ),
                        )
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                        super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                        return true
                    }
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

/**
 * CompositionLocal indicating whether the app is currently running in Android TV mode.
 * Defaults to false (mobile/phone/tablet).
 */
val LocalIsTv = staticCompositionLocalOf { false }

/**
 * Safely requests focus on a [FocusRequester] without crashing if it is not yet attached to a node.
 * Returns true if focus request succeeded, false otherwise.
 */
fun FocusRequester.safeRequestFocus(): Boolean {
    return runCatching {
        requestFocus()
        true
    }.getOrDefault(false)
}

/**
 * Modifier that draws a subtle, elegant focus outline and smooth "pop out" scale
 * when the element receives D-pad or keyboard focus on TV devices.
 * Automatically requests bringIntoView on focus so the element is not hidden behind bars.
 * Zero overhead/no-op on phone/mobile devices.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvFocusOutline(
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 1.75.dp,
    focusedBorderColor: Color? = null,
    scaleOnFocus: Float = 1.045f,
    interactionSource: InteractionSource? = null,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val activeBorderColor = focusedBorderColor ?: primaryColor.copy(alpha = 0.85f)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) activeBorderColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tvFocusBorderColor",
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tvFocusScale",
    )

    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) {
                coroutineScope.launch {
                    runCatching {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
        }
        .scale(animatedScale)
        .then(
            if (isFocused) {
                Modifier.border(
                    BorderStroke(width = borderWidth, color = animatedBorderColor),
                    shape = shape,
                )
            } else {
                Modifier
            },
        )
}

/**
 * Checks whether a [FocusRequester] is currently attached to at least one active modifier node.
 * Uses reflection safely so unattached nodes never trigger Compose's stdout warning or fail silently.
 */
fun FocusRequester.isAttached(): Boolean = runCatching {
    val field = FocusRequester::class.java.getDeclaredField("focusRequesterNodes")
    field.isAccessible = true
    val nodes = field.get(this) ?: return false
    val sizeMethod = runCatching { nodes.javaClass.getMethod("getSize") }.getOrNull()
        ?: runCatching { nodes.javaClass.getMethod("size") }.getOrNull()
    val size = (sizeMethod?.invoke(nodes) as? Number)?.toInt()
        ?: (runCatching { nodes.javaClass.getMethod("isEmpty").invoke(nodes) as? Boolean }.getOrNull()?.let { if (it) 0 else 1 })
        ?: 0
    size > 0
}.getOrDefault(false)

/**
 * Modifier to bridge focus navigation directions (up, down, left, right) on TV.
 * When the destination requester is not yet composed/attached (e.g. inside a LazyColumn/Grid/Column),
 * it scrolls the list to bring the item into composition/view, then requests focus.
 * Zero-op on non-TV devices.
 */
fun Modifier.tvFocusBridge(
    up: FocusRequester? = null,
    down: FocusRequester? = null,
    left: FocusRequester? = null,
    right: FocusRequester? = null,
    lazyListState: LazyListState? = null,
    lazyGridState: LazyGridState? = null,
    scrollState: ScrollState? = null,
    targetDownIndex: Int = 0,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    this
        .focusProperties {
            if (up != null && lazyListState == null && lazyGridState == null && scrollState == null) this.up = up
            if (left != null) this.left = left
            if (right != null) this.right = right
        }
        .onKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

            when (keyEvent.key) {
                Key.DirectionDown, Key.PageDown -> {
                    // 1. If explicit down target is specified and already attached/composed, focus it directly
                    if (down != null && down.isAttached()) {
                        down.requestFocus()
                        return@onKeyEvent true
                    }

                    // 2. If down target is NOT specified, try 2D moveFocus first
                    if (down == null && focusManager.moveFocus(FocusDirection.Down)) {
                        return@onKeyEvent true
                    }

                    // 3. If target is off-screen/uncomposed, scroll container to bring into view, then focus
                    if (scrollState != null) {
                        scope.launch {
                            runCatching {
                                scrollState.scrollTo(0)
                                var focused = false
                                if (down != null) {
                                    for (attempt in 0..10) {
                                        if (down.isAttached()) {
                                            down.requestFocus()
                                            focused = true
                                            break
                                        }
                                        kotlinx.coroutines.delay(16)
                                    }
                                }
                                if (!focused) {
                                    if (down != null) runCatching { down.requestFocus() }
                                    if (!focusManager.moveFocus(FocusDirection.Down)) {
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                }
                            }
                        }
                        return@onKeyEvent true
                    } else if (lazyListState != null) {
                        scope.launch {
                            runCatching {
                                val total = lazyListState.layoutInfo.totalItemsCount
                                if (total > 0) {
                                    val idx = targetDownIndex.coerceIn(0, total - 1)
                                    lazyListState.scrollToItem(idx)
                                }
                                var focused = false
                                if (down != null) {
                                    for (attempt in 0..10) {
                                        if (down.isAttached()) {
                                            down.requestFocus()
                                            focused = true
                                            break
                                        }
                                        kotlinx.coroutines.delay(16)
                                    }
                                }
                                if (!focused) {
                                    if (down != null) runCatching { down.requestFocus() }
                                    if (!focusManager.moveFocus(FocusDirection.Down)) {
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                }
                            }
                        }
                        return@onKeyEvent true
                    } else if (lazyGridState != null) {
                        scope.launch {
                            runCatching {
                                val total = lazyGridState.layoutInfo.totalItemsCount
                                if (total > 0) {
                                    val idx = targetDownIndex.coerceIn(0, total - 1)
                                    lazyGridState.scrollToItem(idx)
                                }
                                var focused = false
                                if (down != null) {
                                    for (attempt in 0..10) {
                                        if (down.isAttached()) {
                                            down.requestFocus()
                                            focused = true
                                            break
                                        }
                                        kotlinx.coroutines.delay(16)
                                    }
                                }
                                if (!focused) {
                                    if (down != null) runCatching { down.requestFocus() }
                                    if (!focusManager.moveFocus(FocusDirection.Down)) {
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                }
                            }
                        }
                        return@onKeyEvent true
                    }

                    // 4. Fallback linear Next
                    if (focusManager.moveFocus(FocusDirection.Next)) {
                        return@onKeyEvent true
                    }
                    false
                }
                Key.DirectionUp, Key.PageUp -> {
                    if (up != null && up.isAttached()) {
                        up.requestFocus()
                        if (scrollState != null && scrollState.value > 0) {
                            scope.launch {
                                runCatching { scrollState.animateScrollTo(0) }
                            }
                        } else if (lazyListState != null && lazyListState.firstVisibleItemIndex > 0) {
                            scope.launch {
                                runCatching { lazyListState.animateScrollToItem(0) }
                            }
                        } else if (lazyGridState != null && lazyGridState.firstVisibleItemIndex > 0) {
                            scope.launch {
                                runCatching { lazyGridState.animateScrollToItem(0) }
                            }
                        }
                        return@onKeyEvent true
                    }
                    if (focusManager.moveFocus(FocusDirection.Up)) {
                        return@onKeyEvent true
                    }

                    if (scrollState != null && scrollState.canScrollBackward) {
                        scope.launch {
                            runCatching {
                                scrollState.animateScrollTo(0)
                                if (up != null) {
                                    for (attempt in 0..10) {
                                        if (up.isAttached()) {
                                            up.requestFocus()
                                            return@launch
                                        }
                                        kotlinx.coroutines.delay(16)
                                    }
                                }
                                focusManager.moveFocus(FocusDirection.Up)
                            }
                        }
                        return@onKeyEvent true
                    } else if (lazyListState != null && lazyListState.canScrollBackward) {
                        scope.launch {
                            runCatching {
                                lazyListState.animateScrollToItem(0)
                                if (up != null) {
                                    for (attempt in 0..10) {
                                        if (up.isAttached()) {
                                            up.requestFocus()
                                            return@launch
                                        }
                                        kotlinx.coroutines.delay(16)
                                    }
                                }
                                focusManager.moveFocus(FocusDirection.Up)
                            }
                        }
                        return@onKeyEvent true
                    } else if (lazyGridState != null && lazyGridState.canScrollBackward) {
                        scope.launch {
                            runCatching {
                                lazyGridState.animateScrollToItem(0)
                                if (up != null) {
                                    for (attempt in 0..10) {
                                        if (up.isAttached()) {
                                            up.requestFocus()
                                            return@launch
                                        }
                                        kotlinx.coroutines.delay(16)
                                    }
                                }
                                focusManager.moveFocus(FocusDirection.Up)
                            }
                        }
                        return@onKeyEvent true
                    }
                    false
                }
                Key.DirectionLeft -> {
                    if (left != null && left.isAttached()) {
                        left.requestFocus()
                        true
                    } else {
                        focusManager.moveFocus(FocusDirection.Left)
                    }
                }
                Key.DirectionRight -> {
                    if (right != null && right.isAttached()) {
                        right.requestFocus()
                        true
                    } else {
                        focusManager.moveFocus(FocusDirection.Right)
                    }
                }
                else -> false
            }
        }
}

/**
 * Attaches D-pad vertical scrolling logic to a LazyColumn in bubbling phase (onKeyEvent).
 * FIRST attempts to move focus via focusManager.moveFocus(direction).
 * If no composed item can take focus, it scrolls the list to bring off-screen items
 * into composition and retries moveFocus once after the scroll completes.
 * Zero-op on non-TV devices.
 */
fun Modifier.tvDpadListScroll(
    lazyListState: LazyListState,
    coroutineScope: CoroutineScope,
    scrollAmountDp: Dp = 250.dp,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val scrollPx = with(density) { scrollAmountDp.toPx() }

    this.onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

        val direction = when (keyEvent.key) {
            Key.DirectionDown, Key.PageDown -> FocusDirection.Down
            Key.DirectionUp, Key.PageUp -> FocusDirection.Up
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            else -> null
        } ?: return@onKeyEvent false

        // 1. FIRST attempt to move focus in the given direction among currently composed items
        val moved = focusManager.moveFocus(direction)
        if (moved) {
            return@onKeyEvent true
        }

        // 2. If moveFocus returned false: off-screen items may need to be composed via scroll
        when (direction) {
            FocusDirection.Down -> {
                if (lazyListState.canScrollForward) {
                    coroutineScope.launch {
                        runCatching {
                            val layoutInfo = lazyListState.layoutInfo
                            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                            val targetIndex = (lastVisibleItem?.index ?: lazyListState.firstVisibleItemIndex) + 1
                            if (targetIndex < layoutInfo.totalItemsCount) {
                                lazyListState.animateScrollToItem(targetIndex)
                            } else {
                                lazyListState.scrollBy(scrollPx)
                            }
                            // Retry moveFocus once after scroll finishes
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }
                    true
                } else {
                    true // Consume key so focus stays on the last item!
                }
            }
            FocusDirection.Up -> {
                if (lazyListState.canScrollBackward) {
                    coroutineScope.launch {
                        runCatching {
                            val firstVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
                            val targetIndex = ((firstVisibleItem?.index ?: lazyListState.firstVisibleItemIndex) - 1).coerceAtLeast(0)
                            lazyListState.animateScrollToItem(targetIndex)
                            // Retry moveFocus once after scroll finishes
                            focusManager.moveFocus(FocusDirection.Up)
                        }
                    }
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}

/**
 * Attaches D-pad vertical scrolling logic to a scrollable Column (Modifier.verticalScroll).
 * FIRST attempts to move focus via focusManager.moveFocus(direction).
 * If moveFocus returns false, it scrolls the Column smoothly and retries moveFocus.
 * Zero-op on non-TV devices.
 */
fun Modifier.tvDpadScroll(
    scrollState: ScrollState,
    coroutineScope: CoroutineScope,
    scrollAmountDp: Dp = 200.dp,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val scrollPx = with(density) { scrollAmountDp.toPx() }

    this.onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

        val direction = when (keyEvent.key) {
            Key.DirectionDown, Key.PageDown -> FocusDirection.Down
            Key.DirectionUp, Key.PageUp -> FocusDirection.Up
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            else -> null
        } ?: return@onKeyEvent false

        // 1. FIRST attempt to move focus in the given direction
        val moved = focusManager.moveFocus(direction)
        if (moved) {
            return@onKeyEvent true
        }

        // 2. If moveFocus returned false: scroll container to bring off-screen items into view
        when (direction) {
            FocusDirection.Down -> {
                if (scrollState.canScrollForward) {
                    coroutineScope.launch {
                        runCatching {
                            scrollState.animateScrollBy(scrollPx)
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }
                    true
                } else true // Consume so focus stays on last item!
            }
            FocusDirection.Up -> {
                if (scrollState.canScrollBackward) {
                    coroutineScope.launch {
                        runCatching {
                            scrollState.animateScrollBy(-scrollPx)
                            focusManager.moveFocus(FocusDirection.Up)
                        }
                    }
                    true
                } else false
            }
            else -> false
        }
    }
}

/**
 * Attaches D-pad vertical scrolling logic to a LazyVerticalGrid.
 * FIRST attempts to move focus via focusManager.moveFocus(direction).
 * If no composed item can take focus, it scrolls the grid to bring off-screen items
 * into composition and retries moveFocus once after the scroll completes.
 * Zero-op on non-TV devices.
 */
fun Modifier.tvDpadGridScroll(
    lazyGridState: LazyGridState,
    coroutineScope: CoroutineScope,
    scrollAmountDp: Dp = 250.dp,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val scrollPx = with(density) { scrollAmountDp.toPx() }

    this.onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

        val direction = when (keyEvent.key) {
            Key.DirectionDown, Key.PageDown -> FocusDirection.Down
            Key.DirectionUp, Key.PageUp -> FocusDirection.Up
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            else -> null
        } ?: return@onKeyEvent false

        // 1. FIRST attempt to move focus in the given direction
        val moved = focusManager.moveFocus(direction)
        if (moved) {
            return@onKeyEvent true
        }

        // 2. If moveFocus returned false: off-screen items may need to be composed via scroll
        when (direction) {
            FocusDirection.Down -> {
                if (lazyGridState.canScrollForward) {
                    coroutineScope.launch {
                        runCatching {
                            val layoutInfo = lazyGridState.layoutInfo
                            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                            val targetIndex = (lastVisibleItem?.index ?: lazyGridState.firstVisibleItemIndex) + 1
                            if (targetIndex < layoutInfo.totalItemsCount) {
                                lazyGridState.animateScrollToItem(targetIndex)
                            } else {
                                lazyGridState.scrollBy(scrollPx)
                            }
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }
                    true
                } else {
                    true // Consume so focus stays on last item!
                }
            }
            FocusDirection.Up -> {
                if (lazyGridState.canScrollBackward) {
                    coroutineScope.launch {
                        runCatching {
                            val firstVisibleItem = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull()
                            val targetIndex = ((firstVisibleItem?.index ?: lazyGridState.firstVisibleItemIndex) - 1).coerceAtLeast(0)
                            lazyGridState.animateScrollToItem(targetIndex)
                            focusManager.moveFocus(FocusDirection.Up)
                        }
                    }
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}

/**
 * Modifier for Sliders on TV to adjust values with Left/Right D-pad and move away with Up/Down.
 * Zero-op on non-TV devices.
 */
fun Modifier.tvSliderDpad(
    value: Float,
    onValueChange: (Float) -> Unit,
    step: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current
    this
        .tvFocusOutline(shape = RoundedCornerShape(8.dp))
        .onKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

            when (keyEvent.key) {
                Key.DirectionLeft -> {
                    val newValue = (value - step).coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(newValue)
                    true
                }
                Key.DirectionRight -> {
                    val newValue = (value + step).coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(newValue)
                    true
                }
                Key.DirectionUp -> {
                    focusManager.moveFocus(FocusDirection.Up)
                }
                Key.DirectionDown -> {
                    focusManager.moveFocus(FocusDirection.Down)
                }
                else -> false
            }
        }
}

/**
 * ONE shared TV-only helper for the settings sub-screen top-bar back button.
 * - Draws TV circular focus outline.
 * - On D-pad Down / PageDown: tries moveFocus(Down); if that returns false, requests focus on firstContentItemRequester;
 *   if that fails, falls back to moveFocus(Next).
 * - On D-pad Right: if onRightActionRequester != null, requests focus on that action icon; else tries moveFocus(Right).
 * No-op on mobile.
 */
fun Modifier.settingsTopBarBack(
    firstContentItemRequester: FocusRequester? = null,
    onRightActionRequester: FocusRequester? = null,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current

    this
        .tvFocusOutline(shape = CircleShape)
        .onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

            when (keyEvent.key) {
                Key.DirectionDown, Key.PageDown -> {
                    if (focusManager.moveFocus(FocusDirection.Down)) {
                        true
                    } else if (firstContentItemRequester != null) {
                        val moved = runCatching {
                            firstContentItemRequester.requestFocus()
                            true
                        }.getOrDefault(false)
                        if (moved) true else focusManager.moveFocus(FocusDirection.Next)
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                }
                Key.DirectionRight -> {
                    if (onRightActionRequester != null) {
                        val moved = runCatching {
                            onRightActionRequester.requestFocus()
                            true
                        }.getOrDefault(false)
                        if (moved) true else focusManager.moveFocus(FocusDirection.Right)
                    } else {
                        focusManager.moveFocus(FocusDirection.Right)
                    }
                }
                Key.DirectionUp, Key.DirectionLeft -> {
                    if (focusManager.moveFocus(if (keyEvent.key == Key.DirectionUp) FocusDirection.Up else FocusDirection.Left)) {
                        true
                    } else {
                        true // Consume so focus cannot escape to system
                    }
                }
                else -> false
            }
        }
}

/**
 * Shared TV-only helper for settings top-bar action icons (e.g. Key icon, Overflow menu).
 * - Draws TV circular focus outline.
 * - On D-pad Left: moves focus to backButtonRequester or tries moveFocus(Left).
 * - On D-pad Down / PageDown: moves focus to firstContentItemRequester or tries moveFocus(Down).
 * No-op on mobile.
 */
fun Modifier.settingsTopBarAction(
    backButtonRequester: FocusRequester? = null,
    firstContentItemRequester: FocusRequester? = null,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current

    this
        .tvFocusOutline(shape = CircleShape)
        .onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

            when (keyEvent.key) {
                Key.DirectionLeft -> {
                    if (backButtonRequester != null) {
                        val moved = runCatching {
                            backButtonRequester.requestFocus()
                            true
                        }.getOrDefault(false)
                        if (moved) true else focusManager.moveFocus(FocusDirection.Left)
                    } else {
                        focusManager.moveFocus(FocusDirection.Left)
                    }
                }
                Key.DirectionDown, Key.PageDown -> {
                    if (focusManager.moveFocus(FocusDirection.Down)) {
                        true
                    } else if (firstContentItemRequester != null) {
                        val moved = runCatching {
                            firstContentItemRequester.requestFocus()
                            true
                        }.getOrDefault(false)
                        if (moved) true else focusManager.moveFocus(FocusDirection.Next)
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                }
                Key.DirectionUp, Key.DirectionRight -> {
                    if (focusManager.moveFocus(if (keyEvent.key == Key.DirectionUp) FocusDirection.Up else FocusDirection.Right)) {
                        true
                    } else {
                        true // Consume so focus cannot escape to system
                    }
                }
                else -> false
            }
        }
}

/**
 * Shared TV-only helper for the first content item in settings sub-screens.
 * - On D-pad Up / PageUp: tries moveFocus(Up); if that returns false, requests focus on backButtonRequester;
 *   if that fails, falls back to moveFocus(Previous).
 * No-op on mobile.
 */
fun Modifier.settingsFirstContentItem(
    backButtonRequester: FocusRequester? = null,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    val focusManager = LocalFocusManager.current

    this.onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        when (keyEvent.key) {
            Key.DirectionUp, Key.PageUp -> {
                if (focusManager.moveFocus(FocusDirection.Up)) {
                    true
                } else if (backButtonRequester != null) {
                    val moved = runCatching {
                        backButtonRequester.requestFocus()
                        true
                    }.getOrDefault(false)
                    if (moved) true else focusManager.moveFocus(FocusDirection.Previous)
                } else {
                    focusManager.moveFocus(FocusDirection.Previous)
                }
            }
            else -> false
        }
    }
}

