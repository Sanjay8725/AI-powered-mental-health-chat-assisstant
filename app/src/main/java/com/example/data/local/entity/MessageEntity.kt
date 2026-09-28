package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String = "neutral", // joy, sadness, anxiety, stress, anger, neutral
    val safetyLabel: String = "safe", // safe, moderate_risk, crisis
    val isSafetyEscalation: Boolean = false
)
