package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.medpulse.data.local.MedPulseDatabase
import com.example.medpulse.data.local.entity.NoteEntity
import com.example.medpulse.data.local.entity.TopicEntity
import com.example.medpulse.data.repository.MedPulseRepository
import com.example.medpulse.timer.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MedPulseApp : Application() {

    lateinit var database: MedPulseDatabase
        private set

    lateinit var repository: MedPulseRepository
        private set

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = MedPulseDatabase.getInstance(this)
        repository = MedPulseRepository(
            database.topicDao(),
            database.noteDao(),
            database.studySessionDao()
        )

        createNotificationChannels()
        seedInitialMedicalContentIfEmpty()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TimerForegroundService.CHANNEL_ID,
                "Study Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active study sessions, countdown timer, and quick controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun seedInitialMedicalContentIfEmpty() {
        appScope.launch {
            val existingTopics = repository.getAllTopics().first()
            if (existingTopics.isEmpty()) {
                val today = MedPulseRepository.getTodayDateString()

                val topic1 = TopicEntity(
                    title = "Cardiovascular Pharmacology: Beta-Blockers & CCBs",
                    subject = "Pharmacology",
                    targetMinutes = 50,
                    priority = "High",
                    notes = "High-yield mechanisms: selectivity of metoprolol vs propranolol, dihydropyridine vs non-DHP calcium channel blockers.",
                    date = today,
                    orderIndex = 0
                )
                val topic2 = TopicEntity(
                    title = "Neuroanatomy: Cranial Nerves III, VII & X Pathways",
                    subject = "Anatomy",
                    targetMinutes = 50,
                    priority = "High",
                    notes = "Exit foramina, parasympathetic ganglia (ciliary, pterygopalatine, otic, submandibular), lesion localizations.",
                    date = today,
                    orderIndex = 1
                )
                val topic3 = TopicEntity(
                    title = "Pulmonology: Arterial Blood Gas (ABG) & Acid-Base Disorders",
                    subject = "Internal Medicine",
                    targetMinutes = 40,
                    priority = "Medium",
                    notes = "Winter's formula, anion gap calculation, delta-delta ratio for mixed disorders.",
                    date = today,
                    orderIndex = 2
                )

                val id1 = database.topicDao().insertTopic(topic1)
                database.topicDao().insertTopic(topic2)
                database.topicDao().insertTopic(topic3)

                val note1 = NoteEntity(
                    title = "Antiarrhythmic Drug Classes (Vaughan Williams)",
                    body = """# Vaughan Williams Classification
• **Class I (Na+ Channel Blockers):**
  - IA: Quinidine, Procainamide, Disopyramide (prolongs APD/QT)
  - IB: Lidocaine, Mexiletine (shortens APD, best for ischemic VT)
  - IC: Flecainide, Propafenone (no effect on APD, markedly slows conduction)
• **Class II (Beta-Blockers):** Esmolol, Metoprolol (decrease SA/AV nodal conduction)
• **Class III (K+ Channel Blockers):** Amiodarone, Sotalol (prolong repolarization)
• **Class IV (Ca2+ Channel Blockers):** Verapamil, Diltiazem (slow AV nodal conduction)""",
                    subject = "Pharmacology",
                    tags = "High-Yield, Cardiology, USMLE",
                    linkedTopicId = id1,
                    linkedTopicTitle = topic1.title,
                    colorIndex = 1, // Yellow/Gold
                    isPinned = true
                )

                val note2 = NoteEntity(
                    title = "Rapid Acid-Base Stepwise Diagnostic Rule",
                    body = """1. Look at pH (< 7.35 = Acidemia, > 7.45 = Alkalemia)
2. Look at PaCO2 and HCO3- to determine primary respiratory vs metabolic process
3. If Metabolic Acidosis -> Calculate Anion Gap:
   AG = Na - (Cl + HCO3) [Normal = 10-12]
4. MUDPILES mnemonic for elevated AG:
   Methanol, Uremia, DKA, Propylene glycol, INH/Iron, Lactic acidosis, Ethylene glycol, Salicylates""",
                    subject = "Internal Medicine",
                    tags = "Clinical Pearls, Pulmonology, Nephrology",
                    colorIndex = 5, // Blue
                    isPinned = true
                )

                database.noteDao().insertNote(note1)
                database.noteDao().insertNote(note2)
            }
        }
    }
}
