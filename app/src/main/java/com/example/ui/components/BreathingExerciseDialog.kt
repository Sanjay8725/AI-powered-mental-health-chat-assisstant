package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MindCareTeal
import com.example.ui.theme.MindCareTealLight
import kotlinx.coroutines.delay

enum class BreathingPattern(val label: String, val inhale: Int, val hold1: Int, val exhale: Int, val hold2: Int) {
    BOX("Box (4-4-4-4)", 4, 4, 4, 4),
    RELAX_478("Relaxing (4-7-8)", 4, 7, 8, 0)
}

@Composable
fun BreathingExerciseDialog(
    onDismiss: () -> Unit
) {
    var pattern by remember { mutableStateOf(BreathingPattern.BOX) }
    var isRunning by remember { mutableStateOf(true) }
    var currentPhase by remember { mutableStateOf("Inhale") }
    var secondsRemaining by remember { mutableIntStateOf(pattern.inhale) }
    var cycleCount by remember { mutableIntStateOf(1) }

    val targetScale = when (currentPhase) {
        "Inhale" -> 1.35f
        "Hold" -> 1.35f
        "Exhale" -> 0.85f
        else -> 0.85f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = secondsRemaining * 1000,
            easing = FastOutSlowInEasing
        ),
        label = "BreathingScale"
    )

    LaunchedEffect(isRunning, pattern) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            // Inhale
            currentPhase = "Inhale"
            for (i in pattern.inhale downTo 1) {
                secondsRemaining = i
                delay(1000)
            }

            // Hold 1
            if (pattern.hold1 > 0) {
                currentPhase = "Hold"
                for (i in pattern.hold1 downTo 1) {
                    secondsRemaining = i
                    delay(1000)
                }
            }

            // Exhale
            currentPhase = "Exhale"
            for (i in pattern.exhale downTo 1) {
                secondsRemaining = i
                delay(1000)
            }

            // Hold 2 (Box breathing)
            if (pattern.hold2 > 0) {
                currentPhase = "Rest & Hold"
                for (i in pattern.hold2 downTo 1) {
                    secondsRemaining = i
                    delay(1000)
                }
            }

            cycleCount++
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("breathing_exercise_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mindful Breathing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pattern selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BreathingPattern.values().forEach { p ->
                        FilterChip(
                            selected = pattern == p,
                            onClick = {
                                pattern = p
                                currentPhase = "Inhale"
                                secondsRemaining = p.inhale
                            },
                            label = { Text(p.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Animated breathing sphere
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(animatedScale)
                            .background(MindCareTealLight.copy(alpha = 0.25f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(animatedScale)
                            .background(MindCareTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentPhase,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            Text(
                                text = "$secondsRemaining",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Cycle $cycleCount • Deepen diaphragmatic expansion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (isRunning) "Pause" else "Resume")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Finished")
                    }
                }
            }
        }
    }
}
