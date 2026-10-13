/*
 * Sekai Tune (2026)
 * © Sekai Tune - github.com/rgsekai/sekai-tune
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rgsekai.sekaitune.ui.tv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import moe.rgsekai.sekaitune.R

val TvCardWidth: Dp = 160.dp
val TvCardCornerRadius: Dp = 12.dp

/**
 * 10-foot TV media card: album art pops out with a subtle focus outline,
 * while song title and subtitle sit cleanly beneath without an enclosing border.
 */
@Composable
fun TvCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRound: Boolean = false,
    cardWidth: Dp = TvCardWidth,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape: Shape = if (isRound) CircleShape else RoundedCornerShape(TvCardCornerRadius)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tvCardScale",
    )

    val activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) activeBorderColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tvCardBorderColor",
    )

    Column(
        modifier = modifier
            .width(cardWidth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource),
        horizontalAlignment = if (isRound) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        // --- ALBUM ART ONLY (Pops out with subtle outline) ---
        Box(
            modifier = Modifier
                .size(cardWidth)
                .scale(scale)
                .then(
                    if (isFocused) {
                        Modifier.border(
                            BorderStroke(width = 1.75.dp, color = animatedBorderColor),
                            shape = shape,
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val context = LocalContext.current
            val imageRequest = remember(thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                placeholder = painterResource(id = if (isRound) R.drawable.person else R.drawable.ic_empty_state),
                error = painterResource(id = if (isRound) R.drawable.person else R.drawable.ic_empty_state),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SONG INFO (Outside outline, no border) ---
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
