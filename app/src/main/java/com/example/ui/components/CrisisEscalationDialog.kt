package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrisisRed
import com.example.ui.theme.CrisisRedBackground

@Composable
fun CrisisEscalationDialog(
    onDismiss: () -> Unit,
    onOpenBreathing: () -> Unit
) {
    val context = LocalContext.current
    var showGroundingGuide by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("crisis_escalation_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with SOS icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(CrisisRedBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Crisis Safety Support",
                                tint = CrisisRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Immediate Support",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CrisisRed
                            )
                            Text(
                                text = "You are not alone. Help is here 24/7.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_crisis_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compassionate Scope statement per IEEE requirement
                Card(
                    colors = CardDefaults.cardColors(containerColor = CrisisRedBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Safety Protocol Activated",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MindCare AI is a wellness assistant and cannot replace professional crisis intervention or medical emergency care. Trained human counselors are available right now to listen and support you safely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Hotline: 988
                EmergencyContactCard(
                    title = "988 Suicide & Crisis Lifeline",
                    subtitle = "Call or text 988 • Free, confidential, 24/7 in US & Canada",
                    badge = "Immediate Help",
                    badgeColor = CrisisRed,
                    actionText = "Call 988",
                    icon = Icons.Default.Call,
                    onAction = { dialNumber(context, "988") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Crisis Text Line: 741741
                EmergencyContactCard(
                    title = "Crisis Text Line",
                    subtitle = "Text HOME to 741741 to connect with a crisis counselor",
                    badge = "Text / SMS",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    actionText = "Text 741741",
                    icon = Icons.AutoMirrored.Filled.Message,
                    onAction = { sendSms(context, "741741", "HOME") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Local Emergency Services (911)
                EmergencyContactCard(
                    title = "Local Emergency Services (911 / 112)",
                    subtitle = "For immediate physical danger or medical emergency",
                    badge = "Emergency",
                    badgeColor = Color(0xFFB91C1C),
                    actionText = "Call 911",
                    icon = Icons.Default.Call,
                    onAction = { dialNumber(context, "911") }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Grounding Exercise toggle
                Text(
                    text = "Gentle Grounding Right Now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGroundingGuide = !showGroundingGuide },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_grounding_button")
                    ) {
                        Text(if (showGroundingGuide) "Hide 5-4-3-2-1" else "5-4-3-2-1 Guide")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenBreathing()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_breathing_from_crisis")
                    ) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Calm Breathing")
                    }
                }

                if (showGroundingGuide) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "5-4-3-2-1 Grounding Practice",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("1. Look around and name 5 things you can SEE.", style = MaterialTheme.typography.bodySmall)
                            Text("2. Notice 4 things you can physically TOUCH.", style = MaterialTheme.typography.bodySmall)
                            Text("3. Listen for 3 distinct things you can HEAR.", style = MaterialTheme.typography.bodySmall)
                            Text("4. Identify 2 things you can SMELL.", style = MaterialTheme.typography.bodySmall)
                            Text("5. Take 1 slow, deep diaphragmatic BREATH.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_crisis_dialog")
                ) {
                    Text("Return to Wellness Assistant")
                }
            }
        }
    }
}

@Composable
fun EmergencyContactCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(actionText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun dialNumber(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun sendSms(context: Context, number: String, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", text)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
