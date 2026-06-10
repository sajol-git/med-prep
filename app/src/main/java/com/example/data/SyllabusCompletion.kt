package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "syllabus_completions")
data class SyllabusCompletion(
    @PrimaryKey val chapterKey: String, // format: "subjectName_chapterName"
    val subjectName: String,
    val chapterName: String,
    val isCompleted: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface SyllabusCompletionDao {
    @Query("SELECT * FROM syllabus_completions")
    fun getAllCompletionsFlow(): Flow<List<SyllabusCompletion>>

    @Query("SELECT * FROM syllabus_completions")
    suspend fun getAllCompletionsImmediate(): List<SyllabusCompletion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: SyllabusCompletion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletions(completions: List<SyllabusCompletion>)

    @Query("DELETE FROM syllabus_completions")
    suspend fun clearAllCompletions()
}
