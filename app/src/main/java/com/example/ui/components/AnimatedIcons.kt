package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BitwardenBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fluent 2 Pure Logo Emblem Composable.
 * Represents the Fluent 2 dynamic shield vault emblem with zero text and transparent background.
 */
@Composable
fun FluentAppLogoEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "LS Pass Fluent 2 Logo",
        modifier = modifier.size(size)
    )
}

/**
 * Minimal Animated Icon wrapper with spring physics scale and color animation.
 */
@Composable
fun AnimatedMinimalIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            isSelected -> 1.15f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "iconTint"
    )

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = animatedTint,
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    )
}

/**
 * Minimal Animated Icon Button with tactile spring feedback.
 */
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Navigation tab icon with selection scale spring animation (bounce when selected).
 */
@Composable
fun AnimatedTabIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    isSelected: Boolean,
    activeColor: Color = BitwardenBlue,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.20f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabIconScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isSelected) -6f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tabIconRotation"
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 220),
        label = "tabIconColor"
    )

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = animatedColor,
        modifier = Modifier
            .scale(scale)
            .rotate(rotation)
    )
}

/**
 * Animated Copy Icon with 25-degree rotation tilt, scale pop, and temporary checkmark success flash.
 */
@Composable
fun AnimatedCopyIcon(
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = BitwardenBlue
) {
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val activeTint by animateColorAsState(
        targetValue = if (isCopied) Color(0xFF107C41) else tint,
        animationSpec = tween(durationMillis = 180),
        label = "copyTint"
    )

    AnimatedIconButton(
        onClick = {
            onCopy()
            scope.launch {
                isCopied = true
                launch { scale.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { rotation.animateTo(-25f, tween(100)) }
                delay(120)
                launch { rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                delay(1200)
                isCopied = false
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Copy Icon",
            tint = activeTint,
            modifier = Modifier
                .scale(scale.value)
                .rotate(rotation.value)
        )
    }
}

/**
 * Animated Lock/Unlock Icon with spring shackle rotation.
 */
@Composable
fun AnimatedLockIcon(
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val rotation by animateFloatAsState(
        targetValue = if (isLocked) 0f else -18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lockRotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isLocked) 1.0f else 1.12f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lockScale"
    )

    Icon(
        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
        contentDescription = if (isLocked) "Locked" else "Unlocked",
        tint = tint,
        modifier = modifier
            .scale(scale)
            .rotate(rotation)
    )
}

/**
 * Animated Refresh / Generator Icon with full 360 spin animation on trigger.
 */
@Composable
fun AnimatedRefreshIcon(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = BitwardenBlue
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    AnimatedIconButton(
        onClick = {
            onRefresh()
            scope.launch {
                rotation.snapTo(0f)
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                )
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh Icon",
            tint = tint,
            modifier = Modifier.rotate(rotation.value)
        )
    }
}

/**
 * Password Visibility Toggle Icon with scale-pop animation.
 */
@Composable
fun AnimatedVisibilityIcon(
    isVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "visibilityScale"
    )

    AnimatedIconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isVisible) "Hide password" else "Show password",
            tint = tint,
            modifier = Modifier.scale(scale)
        )
    }
}

/**
 * Favorite Star Icon with spring pulse animation when toggled.
 */
@Composable
fun AnimatedFavoriteIcon(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteColor: Color = Color(0xFFFFB100)
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "favoriteScale"
    )

    AnimatedIconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) favoriteColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.scale(scale)
        )
    }
}
