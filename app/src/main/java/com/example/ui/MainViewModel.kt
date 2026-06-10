package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.StudyRepository
import com.example.data.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SubjectItem(val name: String, val bngName: String, val totalChapters: Int)
data class SyllabusProgress(val subjectBng: String, val completed: Int, val total: Int)

class MainViewModel(
    private val repository: StudyRepository,
    val backupManager: com.example.data.FirebaseBackupManager
) : ViewModel() {

    // Pre-populated data matching screenshots for the core UI
    val subjects = listOf(
        SubjectItem("Botany", "উদ্ভিদবিজ্ঞান", 9),
        SubjectItem("Zoology", "প্রাণিবিজ্ঞান", 9),
        SubjectItem("Chemistry 1st Paper", "রসায়ন ১ম পত্র", 4),
        SubjectItem("Chemistry 2nd Paper", "রসায়ন ২য় পত্র", 4),
        SubjectItem("Physics 1st Paper", "পদার্থবিজ্ঞান ১ম পত্র", 9),
        SubjectItem("Physics 2nd Paper", "পদার্থবিজ্ঞান ২য় পত্র", 8),
        SubjectItem("English", "ইংরেজি", 5),
        SubjectItem("General Knowledge & Ethics", "সাধারণ জ্ঞান ও এথিক্স", 4)
    )

    val syllabusSubjects = com.example.data.SyllabusData.subjects

    val completedSyllabusChapters: StateFlow<Set<String>> = repository.allSyllabusCompletions
        .map { list -> list.filter { it.isCompleted }.map { it.chapterKey }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val coreProgress: StateFlow<List<SyllabusProgress>> = repository.allSyllabusCompletions
        .map { completions ->
            val bioCompleted = completions.count { 
                it.isCompleted && (it.subjectName.contains("Botany") || it.subjectName.contains("Zoology")) 
            }
            val chemCompleted = completions.count { 
                it.isCompleted && it.subjectName.contains("Chemistry") 
            }
            val physCompleted = completions.count { 
                it.isCompleted && it.subjectName.contains("Physics") 
            }

            listOf(
                SyllabusProgress("জীববিজ্ঞান", bioCompleted, 24),
                SyllabusProgress("রসায়ন", chemCompleted, 10),
                SyllabusProgress("পদার্থবিজ্ঞান", physCompleted, 21)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                SyllabusProgress("জীববিজ্ঞান", 0, 24),
                SyllabusProgress("রসায়ন", 0, 10),
                SyllabusProgress("পদার্থবিজ্ঞান", 0, 21)
            )
        )

    fun toggleChapterCompletion(subjectName: String, chapterName: String, isCompleted: Boolean) {
        val key = "${subjectName}_${chapterName}"
        viewModelScope.launch {
            repository.insertSyllabusCompletion(
                com.example.data.SyllabusCompletion(
                    chapterKey = key,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    isCompleted = isCompleted
                )
            )
            if (backupManager.autoBackupEnabled && backupManager.isUserSignedIn) {
                backupManager.performBackup()
            }
        }
    }

    // For simplicity, we hardcode today's studies to match screenshot if DB is empty,
    // otherwise we would calculate from DB.
    private val _dbSessions = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // A clean flow that returns DB data directly for the UI
    val recentSessions: StateFlow<List<StudySession>> = _dbSessions.map { dbList ->
        dbList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic, reactive Compose state for today's study hours
    var todayStudyHours by mutableStateOf(0f)
        private set

    // Dynamic, reactive Compose state for weekly study hours
    var weeklyStudyHours by mutableStateOf(0f)
        private set

    // Dynamic, reactive Compose state for average daily study hours
    var averageDailyStudyHours by mutableStateOf(0f)
        private set

    val targetHours = 8.0f

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                val calNow = Calendar.getInstance()
                
                // 1. Today's studies
                val todaySessions = sessions.filter { s ->
                    val calS = Calendar.getInstance().apply { timeInMillis = s.dateMillis }
                    calS.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calS.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                }
                val totalMinutes = todaySessions.sumOf { s -> s.durationMinutes }
                val hours = totalMinutes.toFloat() / 60f
                todayStudyHours = Math.round(hours * 10f) / 10f

                // 2. Weekly studies starting on Saturday
                val calWeek = Calendar.getInstance()
                calWeek.set(Calendar.HOUR_OF_DAY, 0)
                calWeek.set(Calendar.MINUTE, 0)
                calWeek.set(Calendar.SECOND, 0)
                calWeek.set(Calendar.MILLISECOND, 0)
                
                // Roll back to Saturday safely with a strict iteration bound to avoid infinite loops on regional/non-Gregorian calendars
                var iterations = 0
                while (calWeek.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && iterations < 7) {
                    calWeek.add(Calendar.DAY_OF_YEAR, -1)
                    iterations++
                }
                val satMillis = calWeek.timeInMillis

                val weeklySessions = sessions.filter { s ->
                    s.dateMillis >= satMillis
                }
                val weeklyMinutes = weeklySessions.sumOf { s -> s.durationMinutes }
                val wHours = weeklyMinutes.toFloat() / 60f
                weeklyStudyHours = Math.round(wHours * 10f) / 10f

                // 3. Average daily study hours (days elapsed from Saturday of this week)
                val dayOfWeek = calNow.get(Calendar.DAY_OF_WEEK)
                val elapsedDays = maxOf(1, (dayOfWeek - Calendar.SATURDAY + 7) % 7 + 1)
                val avgHours = wHours / elapsedDays.toFloat()
                averageDailyStudyHours = Math.round(avgHours * 10f) / 10f
            }
        }
    }
    
    // --- Timer State ---
    var timerSubject by mutableStateOf("")
    var timerChapter by mutableStateOf("")
    var timerSessionType by mutableStateOf("")
    
    var isTimerRunning by mutableStateOf(false)
    var accumulatedTimeSeconds by mutableStateOf(0L)
    var timerLastStartTime by mutableStateOf(0L) // Real-world time

    fun startResumeTimer(subject: String, chapter: String, type: String) {
        if (timerSubject != subject || timerChapter != chapter) {
            // Reset if different
            accumulatedTimeSeconds = 0L
            timerSubject = subject
            timerChapter = chapter
            timerSessionType = type
        }
        if (!isTimerRunning) {
            timerLastStartTime = System.currentTimeMillis()
            isTimerRunning = true
        }
    }

    fun pauseTimer() {
        if (isTimerRunning) {
            val now = System.currentTimeMillis()
            accumulatedTimeSeconds += (now - timerLastStartTime) / 1000
            isTimerRunning = false
        }
    }

    fun getElapsedSeconds(): Long {
        if (isTimerRunning) {
            val now = System.currentTimeMillis()
            return accumulatedTimeSeconds + (now - timerLastStartTime) / 1000
        }
        return accumulatedTimeSeconds
    }
    
    fun stopAndSaveTimer() {
        pauseTimer()
        val minutes = maxOf(1, (accumulatedTimeSeconds / 60).toInt())
        addSession(StudySession(
            subject = if (timerSubject.isEmpty()) "সাধারণ" else timerSubject,
            chapter = if (timerChapter.isEmpty()) "সাধারণ সেশন" else timerChapter,
            durationMinutes = minutes,
            sessionType = timerSessionType
        ))
        // Reset
        accumulatedTimeSeconds = 0L
        isTimerRunning = false
        timerSubject = ""
        timerChapter = ""
    }
    
    fun addSession(session: StudySession) {
        viewModelScope.launch {
            repository.insertSession(session)
            if (backupManager.autoBackupEnabled && backupManager.isUserSignedIn) {
                backupManager.performBackup()
            }
        }
    }

    fun triggerBackup(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.performBackup()
            onResult(res)
        }
    }

    fun triggerRestore(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.performRestore()
            onResult(res)
        }
    }

    suspend fun exportBackupDataJson(): String {
        val root = org.json.JSONObject()
        
        // 1. Sessions
        val sessionsArray = org.json.JSONArray()
        repository.allSessionsImmediate().forEach { s ->
            val obj = org.json.JSONObject().apply {
                put("subject", s.subject)
                put("chapter", s.chapter)
                put("durationMinutes", s.durationMinutes)
                put("sessionType", s.sessionType)
                put("dateMillis", s.dateMillis)
            }
            sessionsArray.put(obj)
        }
        root.put("study_sessions", sessionsArray)
        
        // 2. Syllabus completions
        val completionsArray = org.json.JSONArray()
        repository.allCompletionsImmediate().forEach { c ->
            val obj = org.json.JSONObject().apply {
                put("chapterKey", c.chapterKey)
                put("subjectName", c.subjectName)
                put("chapterName", c.chapterName)
                put("isCompleted", c.isCompleted)
                put("lastUpdated", c.lastUpdated)
            }
            completionsArray.put(obj)
        }
        root.put("syllabus_completions", completionsArray)
        
        return root.toString(2)
    }

    suspend fun restoreBackupDataJson(jsonStr: String): Result<String> {
        return try {
            val root = org.json.JSONObject(jsonStr)
            
            // Validate structure
            if (!root.has("study_sessions") && !root.has("syllabus_completions")) {
                return Result.failure(Exception("ভুল বা অসম্পূর্ণ ফাইল ফরম্যাট!"))
            }
            
            val sessionsToRestore = mutableListOf<StudySession>()
            if (root.has("study_sessions")) {
                val sArray = root.getJSONArray("study_sessions")
                for (i in 0 until sArray.length()) {
                    val sObj = sArray.getJSONObject(i)
                    sessionsToRestore.add(
                        StudySession(
                            id = 0,
                            subject = sObj.optString("subject", "সাধারণ"),
                            chapter = sObj.optString("chapter", "সাধারণ সেশন"),
                            durationMinutes = sObj.optInt("durationMinutes", 0),
                            sessionType = sObj.optString("sessionType", "পাঠ সেশন"),
                            dateMillis = sObj.optLong("dateMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
            
            val completionsToRestore = mutableListOf<com.example.data.SyllabusCompletion>()
            if (root.has("syllabus_completions")) {
                val cArray = root.getJSONArray("syllabus_completions")
                for (i in 0 until cArray.length()) {
                    val cObj = cArray.getJSONObject(i)
                    completionsToRestore.add(
                        com.example.data.SyllabusCompletion(
                            chapterKey = cObj.optString("chapterKey", ""),
                            subjectName = cObj.optString("subjectName", ""),
                            chapterName = cObj.optString("chapterName", ""),
                            isCompleted = cObj.optBoolean("isCompleted", false),
                            lastUpdated = cObj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
            }
            
            // Clear and insert
            repository.clearAllSessions()
            if (sessionsToRestore.isNotEmpty()) {
                repository.insertSessions(sessionsToRestore)
            }
            
            repository.clearAllSyllabusCompletions()
            if (completionsToRestore.isNotEmpty()) {
                repository.insertSyllabusCompletions(completionsToRestore)
            }
            
            Result.success("ম্যানুয়াল রিস্টোর সফল! ${sessionsToRestore.size} টি সেশন ও ${completionsToRestore.size} টি সিলেবাস অগ্রগতি পুনরুদ্ধার করা হয়েছে।")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MainViewModelFactory(
    private val repository: StudyRepository,
    private val backupManager: com.example.data.FirebaseBackupManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, backupManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
