package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [StudySession::class, SyllabusCompletion::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun syllabusCompletionDao(): SyllabusCompletionDao
}
