package com.example.medpulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.medpulse.data.local.dao.NoteDao
import com.example.medpulse.data.local.dao.StudySessionDao
import com.example.medpulse.data.local.dao.TopicDao
import com.example.medpulse.data.local.entity.NoteEntity
import com.example.medpulse.data.local.entity.StudySessionEntity
import com.example.medpulse.data.local.entity.TopicEntity

@Database(
    entities = [
        TopicEntity::class,
        NoteEntity::class,
        StudySessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MedPulseDatabase : RoomDatabase() {

    abstract fun topicDao(): TopicDao
    abstract fun noteDao(): NoteDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: MedPulseDatabase? = null

        fun getInstance(context: Context): MedPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedPulseDatabase::class.java,
                    "medpulse_study.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
