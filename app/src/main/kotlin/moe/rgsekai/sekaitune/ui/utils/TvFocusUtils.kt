/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import moe.rgsekai.sekaitune.utils.isTvDevice as isTvDeviceUtil

/**
 * Detects whether the device is an Android TV / Google TV.
 */
fun Context.isTvDevice(): Boolean = isTvDeviceUtil()

/**
 * CompositionLocal indicating whether the app is currently running in Android TV mode.
 * Defaults to false (mobile/phone/tablet).
 */
val LocalIsTv = staticCompositionLocalOf { false }

/**
 * Safely requests focus on a [FocusRequester] without crashing if it is not yet attached to a node.
 * Catches [IllegalStateException] and any other exception to guarantee crash safety.
 * Returns true if focus request succeeded, false otherwise.
 */
fun FocusRequester.safeRequestFocus(): Boolean {
    return try {
        requestFocus()
        true
    } catch (e: IllegalStateException) {
        false
    } catch (e: Exception) {
        false
    }
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
    borderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color? = null,
    scaleOnFocus: Float = 1.045f,
    interactionSource: InteractionSource? = null,
): Modifier = composed {
    if (!LocalIsTv.current) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    val isInteractionFocused = interactionSource?.collectIsFocusedAsState()?.value == true
    val effectivelyFocused = isFocused || isInteractionFocused

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeBorderColor = focusedBorderColor ?: primaryColor
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    val animatedBorderColor by animateColorAsState(
        targetValue = if (effectivelyFocused) activeBorderColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tvFocusBorderColor",
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (effectivelyFocused) scaleOnFocus else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tvFocusScale",
    )

    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged {
            isFocused = it.isFocused || it.hasFocus
            if (it.isFocused || it.hasFocus) {
                coroutineScope.launch {
                    runCatching {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
        }
        .scale(animatedScale)
        .then(
            if (effectivelyFocused || animatedBorderColor != Color.Transparent) {
                Modifier.border(
                    BorderStroke(width = borderWidth, color = animatedBorderColor),
                    shape = shape,
                )
            } else {
                Modifier
            },
        )
}
