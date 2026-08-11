package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager

/**
 * Password Strength Level Classification
 */
enum class StrengthLevel(
    val label: String,
    val score: Int, // 0 to 5
    val activeSegments: Int,
    val color: Color
) {
    EMPTY("No Password", 0, 0, Color(0xFF9E9E9E)),
    VERY_WEAK("Very Weak", 1, 1, Color(0xFFE53935)),
    WEAK("Weak", 2, 2, Color(0xFFFB8C00)),
    FAIR("Fair", 3, 3, Color(0xFFFBC02D)),
    STRONG("Strong", 4, 4, Color(0xFF4CAF50)),
    VERY_STRONG("Very Strong", 5, 5, Color(0xFF2E7D32));

    companion object {
        fun evaluate(password: String): StrengthLevel {
            if (password.isEmpty()) return EMPTY

            val entropy = CryptoManager.calculateEntropy(password)
            val length = password.length

            val hasUpper = password.any { it.isUpperCase() }
            val hasLower = password.any { it.isLowerCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecial = password.any { !it.isLetterOrDigit() }

            val characterTypesCount = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }

            return when {
                length < 6 || entropy < 25 -> VERY_WEAK
                length < 8 || (entropy < 40 && characterTypesCount <= 2) -> WEAK
                entropy < 60 || characterTypesCount < 3 -> FAIR
                entropy < 80 || length < 14 -> STRONG
                else -> VERY_STRONG
            }
        }
    }
}

/**
 * Reusable Password Strength Meter UI component that displays security level,
 * entropy bits, animated 5-segment bar, character requirement chips, and smart security tips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier,
    showRequirements: Boolean = true,
    showTips: Boolean = true
) {
    val strengthLevel = StrengthLevel.evaluate(password)
    val entropy = CryptoManager.calculateEntropy(password)

    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val isMinLength = password.length >= 12

    val animatedColor by animateColorAsState(
        targetValue = strengthLevel.color,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "strengthColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("password_strength_meter")
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (password.isNotEmpty()) animatedColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Top Header Row: Icon + Label + Entropy Bits
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Password Security",
                    tint = animatedColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (password.isEmpty()) "Password Strength" else "Strength: ${strengthLevel.label}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (password.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else animatedColor
                )
            }

            if (password.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = animatedColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${entropy.toInt()} bits entropy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = animatedColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5-Segment Progress Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (step in 1..5) {
                val isActive = step <= strengthLevel.activeSegments
                val segmentTargetColor = if (isActive) animatedColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

                val segmentColor by animateColorAsState(
                    targetValue = segmentTargetColor,
                    animationSpec = tween(durationMillis = 250),
                    label = "segmentColor_$step"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segmentColor)
                )
            }
        }

        // Requirements Chips Row
        if (showRequirements && password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RequirementChip(label = "12+ Chars", isFulfilled = isMinLength)
                RequirementChip(label = "Uppercase (A-Z)", isFulfilled = hasUpper)
                RequirementChip(label = "Lowercase (a-z)", isFulfilled = hasLower)
                RequirementChip(label = "Numbers (0-9)", isFulfilled = hasDigit)
                RequirementChip(label = "Symbols (!@#)", isFulfilled = hasSpecial)
            }
        }

        // Smart Security Tip Banner
        if (showTips && password.isNotEmpty() && strengthLevel != StrengthLevel.VERY_STRONG) {
            Spacer(modifier = Modifier.height(8.dp))
            val tipText = when {
                password.length < 8 -> "Tip: Increase password length to at least 12 characters."
                !hasUpper || !hasLower -> "Tip: Combine both uppercase (A-Z) and lowercase (a-z) letters."
                !hasDigit || !hasSpecial -> "Tip: Add numbers and special symbols (e.g. !@#$)."
                else -> "Tip: Longer passwords or passphrases offer superior cryptographic resistance."
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Security Tip",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tipText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RequirementChip(
    label: String,
    isFulfilled: Boolean
) {
    val chipBg = if (isFulfilled) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val chipContent = if (isFulfilled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = chipBg,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isFulfilled) Color(0xFF2E7D32).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isFulfilled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isFulfilled) "Met" else "Missing",
                tint = chipContent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isFulfilled) FontWeight.SemiBold else FontWeight.Normal,
                color = chipContent
            )
        }
    }
}
