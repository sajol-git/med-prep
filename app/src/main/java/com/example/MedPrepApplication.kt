package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.StudyRepository
import com.example.data.FirebaseBackupManager

class MedPrepApplication : Application() {
    val database: AppDatabase by lazy {
        try {
            Room.databaseBuilder(
                this,
                AppDatabase::class.java,
                "med_prep_database"
            ).fallbackToDestructiveMigration().build()
        } catch (t: Throwable) {
            t.printStackTrace()
            Room.inMemoryDatabaseBuilder(
                this,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }

    val repository: StudyRepository by lazy {
        StudyRepository(this, database.studySessionDao(), database.syllabusCompletionDao())
    }

    val backupManager: FirebaseBackupManager by lazy {
        FirebaseBackupManager(this, database.studySessionDao(), database.syllabusCompletionDao())
    }

    override fun onCreate() {
        super.onCreate()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Dhaka"))
    }
}
