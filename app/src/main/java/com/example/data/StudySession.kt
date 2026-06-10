package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val chapter: String,
    val durationMinutes: Int,
    val sessionType: String,
    val dateMillis: Long = System.currentTimeMillis()
)
