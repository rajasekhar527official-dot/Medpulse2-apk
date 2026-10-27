package com.example.medpulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionType: String, // "FOCUS" or "BREAK"
    val durationSeconds: Int,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val linkedTopicId: Long? = null,
    val linkedTopicTitle: String? = null,
    val date: String, // YYYY-MM-DD
    val isCompleted: Boolean = true
)
