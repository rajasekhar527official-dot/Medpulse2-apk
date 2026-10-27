package com.example.medpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val targetMinutes: Int = 50,
    val priority: String = "Medium", // High, Medium, Low
    val notes: String = "",
    val date: String, // YYYY-MM-DD
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
