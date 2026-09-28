package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.EmotionAnxiety
import com.example.ui.theme.EmotionCalm
import com.example.ui.theme.EmotionJoy
import com.example.ui.theme.EmotionSadness
import com.example.ui.theme.EmotionStress
import com.example.ui.theme.MindCareTeal
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmotionItem(
    val id: String,
    val name: String,
    val emoji: String,
    val subtitle: String,
    val primaryColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyMoodLoggerCard(
    isSupabaseConfigured: Boolean,
    onSaveMood: (mood: String, score: Int, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emotions = remember {
        listOf(
            EmotionItem("joy", "Joyful", "☀️", "Energized & bright", EmotionJoy),
            EmotionItem("calm", "Calm", "🌿", "Peaceful & steady", EmotionCalm),
            EmotionItem("grateful", "Grateful", "🌸", "Appreciative & warm", Color(0xFFEC4899)),
            EmotionItem("anxious", "Anxious", "⚡", "Restless or uneasy", EmotionAnxiety),
            EmotionItem("sad", "Sad", "🌧️", "Low energy or down", EmotionSadness),
            EmotionItem("stressed", "Stressed", "🔥", "Under pressure", EmotionStress),
            EmotionItem("frustrated", "Frustrated", "🛑", "Irritated or blocked", CrisisRed),
            EmotionItem("tired", "Exhausted", "🌙", "Depleted & weary", Color(0xFF6B7280))
        )
    }

    var selectedEmotion by remember { mutableStateOf(emotions[1]) } // Default to Calm
    var selectedScore by remember { mutableIntStateOf(4) }
    var noteText by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessBanner by remember { mutableStateOf(false) }

    val contextTags = listOf("Work & Study", "Sleep", "Family", "Health", "Social", "Mindfulness", "Exercise")

    val timeFormatter = remember { SimpleDateFormat("EEEE, MMMM d • h:mm a", Locale.getDefault()) }
    val currentFormattedTime = remember { timeFormatter.format(Date()) }

    val animatedBorderColor by animateColorAsState(
        targetValue = selectedEmotion.primaryColor,
        animationSpec = tween(300),
        label = "BorderColor"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_mood_logger_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with Mood icon and Supabase cloud sync badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(selectedEmotion.primaryColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedEmotion.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Mood Check-in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentFormattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Cloud Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSupabaseConfigured) MindCareTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("supabase_sync_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSupabaseConfigured) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (isSupabaseConfigured) MindCareTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSupabaseConfigured) "Supabase" else "Local Sync",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSupabaseConfigured) MindCareTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Emotion Selector Grid (2 rows of 4)
            Text(
                text = "Select your current emotion:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                emotions.forEach { emotion ->
                    val isSelected = emotion.id == selectedEmotion.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) emotion.primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) emotion.primaryColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedEmotion = emotion }
                            .testTag("emotion_selector_${emotion.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emotion.emoji, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = emotion.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) emotion.primaryColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Wellbeing & Intensity Rating (1-5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emotional Wellbeing Level:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (selectedScore) {
                        1 -> "1/5 • Very low"
                        2 -> "2/5 • Challenging"
                        3 -> "3/5 • Balanced"
                        4 -> "4/5 • Pleasant"
                        else -> "5/5 • Thriving"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = selectedEmotion.primaryColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    IconButton(
                        onClick = { selectedScore = i },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("mood_score_star_$i")
                    ) {
                        Icon(
                            imageVector = if (i <= selectedScore) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Rating $i",
                            tint = if (i <= selectedScore) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Context / Trigger tags
            Text(
                text = "What is influencing your mood? (optional):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                contextTags.forEach { tag ->
                    val isChecked = selectedTags.contains(tag)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            if (isChecked) selectedTags.remove(tag) else selectedTags.add(tag)
                        },
                        label = { Text(tag, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedEmotion.primaryColor.copy(alpha = 0.15f),
                            selectedLabelColor = selectedEmotion.primaryColor
                        ),
                        modifier = Modifier.testTag("mood_tag_$tag")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reflection Note Text Field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Reflection note (optional)") },
                placeholder = { Text("What made you feel this way today?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_note_field"),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedEmotion.primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Success feedback banner
            AnimatedVisibility(visible = showSuccessBanner) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MindCareTeal.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("mood_save_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MindCareTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logged ${selectedEmotion.name} with timestamp to database!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MindCareTeal
                        )
                    }
                }
            }

            // Save Action Button
            Button(
                onClick = {
                    isSaving = true
                    val fullNote = if (selectedTags.isNotEmpty()) {
                        val tagsString = selectedTags.joinToString(", ")
                        if (noteText.isNotBlank()) "$noteText [Tags: $tagsString]" else "Tags: $tagsString"
                    } else noteText

                    onSaveMood(selectedEmotion.name, selectedScore, fullNote)
                    showSuccessBanner = true
                    isSaving = false
                    noteText = ""
                    selectedTags.clear()
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = selectedEmotion.primaryColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_mood_to_supabase_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Mood Entry with Timestamp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Auto-dismiss success banner after 4 seconds
    LaunchedEffect(showSuccessBanner) {
        if (showSuccessBanner) {
            delay(4000)
            showSuccessBanner = false
        }
    }
}
