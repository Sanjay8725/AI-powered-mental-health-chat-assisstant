package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_events")
data class SafetyEventEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val conversationId: String,
    val riskLevel: String, // "moderate_risk", "imminent_crisis"
    val triggerPhrase: String,
    val actionTaken: String, // "show_crisis_hotline_dialog", "escalated_emergency_resources"
    val createdAt: Long = System.currentTimeMillis()
)
