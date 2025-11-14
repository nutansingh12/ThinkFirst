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
import com.thinkfirst.android.data.model.MascotMessageDTO

/**
 * Quizzy the Owl mascot message component
 * Displays encouraging messages with owl icon and speech bubble
 */
@Composable
fun QuizzyMessage(
    mascotMessage: MascotMessageDTO?,
    modifier: Modifier = Modifier
) {
    if (mascotMessage == null) return

    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(mascotMessage) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + 
                slideInVertically(animationSpec = tween(500)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Quizzy's owl icon with animation
            QuizzyOwlIcon()
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Speech bubble
            SpeechBubble(
                message = mascotMessage.message,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuizzyOwlIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "owl_bounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "owl_scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🦉",
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpeechBubble(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quizzy the Owl",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Compact version of Quizzy message for inline display
 */
@Composable
fun QuizzyMessageCompact(
    mascotMessage: MascotMessageDTO?,
    modifier: Modifier = Modifier
) {
    if (mascotMessage == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🦉",
            fontSize = 20.sp
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = mascotMessage.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

