package com.example.medpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val subject: String = "",
    val tags: String = "", // Comma-separated tags
    val linkedTopicId: Long? = null,
    val linkedTopicTitle: String? = null,
    val colorIndex: Int = 0, // 0: White, 1: Yellow, 2: Peach, 3: Pink, 4: Green, 5: Blue, 6: Purple
    val isPinned: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
