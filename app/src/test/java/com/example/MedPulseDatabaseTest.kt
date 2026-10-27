package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.medpulse.data.local.MedPulseDatabase
import com.example.medpulse.data.local.dao.NoteDao
import com.example.medpulse.data.local.dao.StudySessionDao
import com.example.medpulse.data.local.dao.TopicDao
import com.example.medpulse.data.local.entity.NoteEntity
import com.example.medpulse.data.local.entity.StudySessionEntity
import com.example.medpulse.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MedPulseDatabaseTest {

    private lateinit var db: MedPulseDatabase
    private lateinit var topicDao: TopicDao
    private lateinit var noteDao: NoteDao
    private lateinit var sessionDao: StudySessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MedPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        topicDao = db.topicDao()
        noteDao = db.noteDao()
        sessionDao = db.studySessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndQueryTopic() = runBlocking {
        val topic = TopicEntity(
            title = "Cardiology Antiarrhythmics",
            subject = "Pharmacology",
            targetMinutes = 50,
            priority = "High",
            date = "2026-10-27"
        )
        val id = topicDao.insertTopic(topic)
        assertTrue(id > 0)

        val retrieved = topicDao.getTopicById(id)
        assertNotNull(retrieved)
        assertEquals("Cardiology Antiarrhythmics", retrieved?.title)
        assertEquals(false, retrieved?.isCompleted)

        // Toggle completion
        topicDao.updateTopic(retrieved!!.copy(isCompleted = true, completedAtEpochMs = 123456L))
        val updated = topicDao.getTopicById(id)
        assertEquals(true, updated?.isCompleted)
        assertEquals(123456L, updated?.completedAtEpochMs)
    }

    @Test
    fun testNotePinningAndSearch() = runBlocking {
        val note1 = NoteEntity(
            title = "Winter's Formula",
            body = "Expected PaCO2 = 1.5 * HCO3 + 8 (+/- 2)",
            subject = "Pulmonology",
            tags = "Acid-Base, ABG",
            isPinned = false,
            updatedAtEpochMs = 1000L
        )
        val note2 = NoteEntity(
            title = "High-Yield Cranial Nerves",
            body = "CN III, VII, IX, X carry parasympathetic fibers",
            subject = "Anatomy",
            tags = "Neuro",
            isPinned = true,
            updatedAtEpochMs = 2000L
        )
        noteDao.insertNote(note1)
        noteDao.insertNote(note2)

        val allNotes = noteDao.getAllNotesFlow().first()
        assertEquals(2, allNotes.size)
        // Pinned notes must appear first
        assertTrue(allNotes[0].isPinned)
        assertEquals("High-Yield Cranial Nerves", allNotes[0].title)

        // Test search
        val searchResults = noteDao.searchNotesFlow("formula").first()
        assertEquals(1, searchResults.size)
        assertEquals("Winter's Formula", searchResults[0].title)
    }

    @Test
    fun testStudySessionAggregation() = runBlocking {
        val session1 = StudySessionEntity(
            sessionType = "FOCUS",
            durationSeconds = 3000,
            startedAtEpochMs = 1000L,
            endedAtEpochMs = 4000L,
            date = "2026-10-27"
        )
        val session2 = StudySessionEntity(
            sessionType = "FOCUS",
            durationSeconds = 1500,
            startedAtEpochMs = 5000L,
            endedAtEpochMs = 6500L,
            date = "2026-10-27"
        )
        val breakSession = StudySessionEntity(
            sessionType = "BREAK",
            durationSeconds = 600,
            startedAtEpochMs = 7000L,
            endedAtEpochMs = 7600L,
            date = "2026-10-27"
        )

        sessionDao.insertSession(session1)
        sessionDao.insertSession(session2)
        sessionDao.insertSession(breakSession)

        val totalFocusSeconds = sessionDao.getTotalFocusSecondsForDate("2026-10-27").first()
        assertEquals(4500, totalFocusSeconds) // 3000 + 1500

        val focusCount = sessionDao.getFocusSessionCountForDate("2026-10-27").first()
        assertEquals(2, focusCount)
    }
}
