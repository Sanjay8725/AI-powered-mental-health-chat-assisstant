package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_resources")
data class KnowledgeResourceEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String, // Coping, Grounding, Anxiety, Sleep, Depression, Crisis
    val content: String,
    val source: String,
    val url: String = "",
    val tags: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
