package com.example.data

import android.content.Context
import com.example.receiver.TodayStudyWidget
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val context: Context,
    private val studySessionDao: StudySessionDao,
    private val syllabusCompletionDao: SyllabusCompletionDao
) {
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()

    suspend fun insertSession(session: StudySession) {
        studySessionDao.insertSession(session)
        TodayStudyWidget.updateAllWidgets(context)
    }

    fun getStudyDurationForDay(startOfDay: Long, endOfDay: Long): Flow<Int?> {
        return studySessionDao.getStudyDurationForDay(startOfDay, endOfDay)
    }

    // --- Syllabus Completions APIs ---
    val allSyllabusCompletions: Flow<List<SyllabusCompletion>> = syllabusCompletionDao.getAllCompletionsFlow()

    suspend fun allSessionsImmediate(): List<StudySession> {
        return studySessionDao.getAllSessionsImmediate()
    }

    suspend fun allCompletionsImmediate(): List<SyllabusCompletion> {
        return syllabusCompletionDao.getAllCompletionsImmediate()
    }

    suspend fun insertSessions(sessions: List<StudySession>) {
        studySessionDao.insertSessions(sessions)
        TodayStudyWidget.updateAllWidgets(context)
    }

    suspend fun clearAllSessions() {
        studySessionDao.clearAllSessions()
        TodayStudyWidget.updateAllWidgets(context)
    }

    suspend fun insertSyllabusCompletion(completion: SyllabusCompletion) {
        syllabusCompletionDao.insertCompletion(completion)
    }

    suspend fun insertSyllabusCompletions(completions: List<SyllabusCompletion>) {
        syllabusCompletionDao.insertCompletions(completions)
    }

    suspend fun clearAllSyllabusCompletions() {
        syllabusCompletionDao.clearAllCompletions()
    }
}
