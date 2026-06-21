package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chapter_exam_results")
data class ChapterExamResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterKey: String, // format: "subjectName_chapterName"
    val subjectName: String,
    val chapterName: String,
    val percentage: Double, // The score percentage (usually out of 100)
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChapterExamResultDao {
    @Query("SELECT * FROM chapter_exam_results WHERE chapterKey = :chapterKey ORDER BY timestamp ASC")
    fun getResultsForChapterFlow(chapterKey: String): Flow<List<ChapterExamResult>>

    @Query("SELECT * FROM chapter_exam_results ORDER BY timestamp ASC")
    fun getAllResultsFlow(): Flow<List<ChapterExamResult>>

    @Query("SELECT * FROM chapter_exam_results ORDER BY timestamp ASC")
    suspend fun getAllResultsImmediate(): List<ChapterExamResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ChapterExamResult)

    @Query("DELETE FROM chapter_exam_results WHERE id = :id")
    suspend fun deleteResult(id: Int)

    @Query("DELETE FROM chapter_exam_results")
    suspend fun clearAllResults()
}
