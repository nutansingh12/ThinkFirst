package com.thinkfirst.android.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.thinkfirst.android.data.model.BadgeDTO
import kotlinx.coroutines.delay

@Composable
fun BadgeUnlockDialog(
    badges: List<BadgeDTO>,
    onDismiss: () -> Unit
) {
    var currentBadgeIndex by remember { mutableStateOf(0) }
    var showConfetti by remember { mutableStateOf(true) }

    // Auto-advance to next badge after 3 seconds
    LaunchedEffect(currentBadgeIndex) {
        if (currentBadgeIndex < badges.size - 1) {
            delay(3000)
            currentBadgeIndex++
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Confetti effect
                if (showConfetti) {
                    ConfettiEffect()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebration header
                    Text(
                        text = "🎉",
                        fontSize = 48.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Badge Unlocked!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Animated badge
                    AnimatedBadgeIcon(badges[currentBadgeIndex])

                    Spacer(modifier = Modifier.height(16.dp))

                    // Badge name
                    Text(
                        text = badges[currentBadgeIndex].name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge description
                    badges[currentBadgeIndex].description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress indicator if multiple badges
                    if (badges.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            badges.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == currentBadgeIndex)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentBadgeIndex < badges.size - 1) {
                            OutlinedButton(
                                onClick = { currentBadgeIndex++ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next")
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (currentBadgeIndex == badges.size - 1) "Awesome!" else "Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedBadgeIcon(badge: BadgeDTO) {
    // Scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                when (badge.rarity) {
                    "LEGENDARY" -> MaterialTheme.colorScheme.errorContainer
                    "EPIC" -> MaterialTheme.colorScheme.tertiaryContainer
                    "RARE" -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge.icon,
            fontSize = 64.sp
        )
    }
}

@Composable
fun ConfettiEffect() {
    // Simple confetti effect using emojis
    val confettiItems = remember {
        listOf("🎉", "✨", "⭐", "🌟", "💫", "🎊")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        confettiItems.forEachIndexed { index, emoji ->
            ConfettiPiece(
                emoji = emoji,
                delay = index * 100L
            )
        }
    }
}

@Composable
fun ConfettiPiece(emoji: String, delay: Long) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(1000)
        ),
        exit = fadeOut()
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(
                start = (0..300).random().dp,
                top = (0..100).random().dp
            )
        )
    }
}

