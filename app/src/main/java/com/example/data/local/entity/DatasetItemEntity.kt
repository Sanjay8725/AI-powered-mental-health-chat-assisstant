package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dataset_items")
data class DatasetItemEntity(
    @PrimaryKey
    val id: String,
    val prompt: String,
    val response: String,
    val category: String, // Intent/Topic: coping, anxiety, depression, crisis, wellness, general
    val emotion: String = "neutral", // joy, sadness, anxiety, stress, anger, neutral
    val safetyLevel: String = "safe", // safe, warning, crisis
    val source: String = "imported_csv", // "default_ieee_dataset", "user_csv"
    val importedAt: Long = System.currentTimeMillis()
)
