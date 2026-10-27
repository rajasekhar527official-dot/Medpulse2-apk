package com.example.medpulse.data.repository

import com.example.medpulse.data.local.dao.NoteDao
import com.example.medpulse.data.local.dao.StudySessionDao
import com.example.medpulse.data.local.dao.TopicDao
import com.example.medpulse.data.local.entity.NoteEntity
import com.example.medpulse.data.local.entity.StudySessionEntity
import com.example.medpulse.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedPulseRepository(
    private val topicDao: TopicDao,
    private val noteDao: NoteDao,
    private val sessionDao: StudySessionDao
) {
    companion object {
        fun getTodayDateString(): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return formatter.format(Date())
        }
    }

    // --- TOPICS ---
    fun getActiveTopicsForDate(date: String): Flow<List<TopicEntity>> =
        topicDao.getActiveTopicsForDateFlow(date)

    fun getAllTopicsForDate(date: String): Flow<List<TopicEntity>> =
        topicDao.getAllTopicsForDateFlow(date)

    fun getArchivedTopics(): Flow<List<TopicEntity>> =
        topicDao.getArchivedTopicsFlow()

    fun getAllTopics(): Flow<List<TopicEntity>> =
        topicDao.getAllTopicsFlow()

    fun searchTopics(query: String): Flow<List<TopicEntity>> =
        topicDao.searchTopics(query)

    fun getAllTopicDates(): Flow<List<String>> =
        topicDao.getAllDistinctDates()

    suspend fun getTopicById(id: Long): TopicEntity? =
        topicDao.getTopicById(id)

    suspend fun addTopic(
        title: String,
        subject: String,
        targetMinutes: Int = 50,
        priority: String = "Medium",
        notes: String = "",
        date: String = getTodayDateString()
    ): Long {
        val topic = TopicEntity(
            title = title.trim(),
            subject = subject.trim(),
            targetMinutes = targetMinutes,
            priority = priority,
            notes = notes.trim(),
            date = date
        )
        return topicDao.insertTopic(topic)
    }

    suspend fun updateTopic(topic: TopicEntity) {
        topicDao.updateTopic(topic)
    }

    suspend fun toggleTopicCompletion(topic: TopicEntity) {
        val updated = if (topic.isCompleted) {
            topic.copy(isCompleted = false, completedAtEpochMs = null)
        } else {
            topic.copy(isCompleted = true, completedAtEpochMs = System.currentTimeMillis())
        }
        topicDao.updateTopic(updated)
    }

    suspend fun archiveTopic(topic: TopicEntity) {
        topicDao.updateTopic(topic.copy(isArchived = true))
    }

    suspend fun unarchiveTopic(topic: TopicEntity) {
        topicDao.updateTopic(topic.copy(isArchived = false))
    }

    suspend fun rescheduleTopic(topic: TopicEntity, newDate: String) {
        topicDao.updateTopic(topic.copy(date = newDate))
    }

    suspend fun reuseTopicForDate(sourceTopic: TopicEntity, targetDate: String): Long {
        val newTopic = TopicEntity(
            title = sourceTopic.title,
            subject = sourceTopic.subject,
            targetMinutes = sourceTopic.targetMinutes,
            priority = sourceTopic.priority,
            notes = sourceTopic.notes,
            date = targetDate,
            isCompleted = false,
            completedAtEpochMs = null,
            isArchived = false
        )
        return topicDao.insertTopic(newTopic)
    }

    suspend fun deleteTopic(topic: TopicEntity) {
        topicDao.deleteTopic(topic)
    }

    suspend fun deleteTopicById(id: Long) {
        topicDao.deleteTopicById(id)
    }

    // --- NOTES ---
    fun getAllNotes(): Flow<List<NoteEntity>> =
        noteDao.getAllNotesFlow()

    fun getNotesBySubject(subject: String): Flow<List<NoteEntity>> =
        noteDao.getNotesBySubjectFlow(subject)

    fun searchNotes(query: String): Flow<List<NoteEntity>> =
        noteDao.searchNotesFlow(query)

    fun getNotesForTopic(topicId: Long): Flow<List<NoteEntity>> =
        noteDao.getNotesForTopicFlow(topicId)

    suspend fun getNoteById(id: Long): NoteEntity? =
        noteDao.getNoteById(id)

    suspend fun saveNote(
        id: Long = 0,
        title: String,
        body: String,
        subject: String = "",
        tags: String = "",
        linkedTopicId: Long? = null,
        linkedTopicTitle: String? = null,
        colorIndex: Int = 0,
        isPinned: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val note = if (id == 0L) {
            NoteEntity(
                title = title.trim(),
                body = body,
                subject = subject.trim(),
                tags = tags.trim(),
                linkedTopicId = linkedTopicId,
                linkedTopicTitle = linkedTopicTitle,
                colorIndex = colorIndex,
                isPinned = isPinned,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        } else {
            val existing = noteDao.getNoteById(id)
            val createdAt = existing?.createdAtEpochMs ?: now
            NoteEntity(
                id = id,
                title = title.trim(),
                body = body,
                subject = subject.trim(),
                tags = tags.trim(),
                linkedTopicId = linkedTopicId,
                linkedTopicTitle = linkedTopicTitle,
                colorIndex = colorIndex,
                isPinned = isPinned,
                createdAtEpochMs = createdAt,
                updatedAtEpochMs = now
            )
        }
        return noteDao.insertNote(note)
    }

    suspend fun toggleNotePin(note: NoteEntity) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    // --- SESSIONS ---
    fun getAllSessions(): Flow<List<StudySessionEntity>> =
        sessionDao.getAllSessionsFlow()

    fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>> =
        sessionDao.getSessionsForDateFlow(date)

    fun getTotalFocusSecondsForDate(date: String): Flow<Int> =
        sessionDao.getTotalFocusSecondsForDate(date)

    fun getFocusSessionCountForDate(date: String): Flow<Int> =
        sessionDao.getFocusSessionCountForDate(date)

    suspend fun recordSession(
        sessionType: String,
        durationSeconds: Int,
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        linkedTopicId: Long?,
        linkedTopicTitle: String?,
        date: String = getTodayDateString()
    ): Long {
        if (durationSeconds <= 0) return 0L
        val session = StudySessionEntity(
            sessionType = sessionType,
            durationSeconds = durationSeconds,
            startedAtEpochMs = startedAtEpochMs,
            endedAtEpochMs = endedAtEpochMs,
            linkedTopicId = linkedTopicId,
            linkedTopicTitle = linkedTopicTitle,
            date = date,
            isCompleted = true
        )
        return sessionDao.insertSession(session)
    }

    suspend fun deleteSessionById(id: Long) {
        sessionDao.deleteSessionById(id)
    }
}
