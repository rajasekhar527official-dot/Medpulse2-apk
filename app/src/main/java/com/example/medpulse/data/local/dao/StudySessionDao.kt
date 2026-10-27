package com.example.medpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.medpulse.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY endedAtEpochMs DESC")
    fun getAllSessionsFlow(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE date = :date ORDER BY endedAtEpochMs DESC")
    fun getSessionsForDateFlow(date: String): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE date = :date AND sessionType = 'FOCUS'")
    fun getTotalFocusSecondsForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE date = :date AND sessionType = 'FOCUS'")
    fun getFocusSessionCountForDate(date: String): Flow<Int>

    @Query("SELECT DISTINCT date FROM study_sessions ORDER BY date DESC")
    fun getAllSessionDatesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
