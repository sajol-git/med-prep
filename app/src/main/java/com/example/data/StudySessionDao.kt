package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<StudySession>)

    @Query("SELECT * FROM study_sessions ORDER BY dateMillis DESC")
    suspend fun getAllSessionsImmediate(): List<StudySession>

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE dateMillis >= :startOfDay AND dateMillis <= :endOfDay")
    fun getStudyDurationForDay(startOfDay: Long, endOfDay: Long): Flow<Int?>
}
