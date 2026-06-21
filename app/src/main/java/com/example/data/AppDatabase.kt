package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StudySession::class, SyllabusCompletion::class, ChapterExamResult::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun syllabusCompletionDao(): SyllabusCompletionDao
    abstract fun chapterExamResultDao(): ChapterExamResultDao
}
