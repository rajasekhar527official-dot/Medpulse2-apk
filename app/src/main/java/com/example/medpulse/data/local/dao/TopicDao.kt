package com.example.medpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medpulse.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics WHERE isArchived = 0 AND date = :date ORDER BY orderIndex ASC, id ASC")
    fun getActiveTopicsForDateFlow(date: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE date = :date ORDER BY isCompleted ASC, orderIndex ASC, id ASC")
    fun getAllTopicsForDateFlow(date: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE isArchived = 1 ORDER BY date DESC, id DESC")
    fun getArchivedTopicsFlow(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY date DESC, orderIndex ASC")
    fun getAllTopicsFlow(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: Long): TopicEntity?

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    fun getTopicByIdFlow(id: Long): Flow<TopicEntity?>

    @Query("SELECT DISTINCT date FROM topics ORDER BY date DESC")
    fun getAllDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM topics WHERE title LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTopics(query: String): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long>

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)
}
