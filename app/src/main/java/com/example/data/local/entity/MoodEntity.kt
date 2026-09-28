package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moods")
data class MoodEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val mood: String, // Happy, Calm, Anxious, Sad, Stressed, Overwhelmed
    val score: Int, // 1 to 5
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
