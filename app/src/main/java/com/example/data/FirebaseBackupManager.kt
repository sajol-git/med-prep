package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class FirebaseBackupManager(
    private val context: Context,
    private val studySessionDao: StudySessionDao,
    private val syllabusCompletionDao: SyllabusCompletionDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("firebase_backup_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()

    private var sandboxSyllabusBackupData: String?
        get() = prefs.getString("sandbox_syllabus_backup_json", null)
        set(value) = prefs.edit().putString("sandbox_syllabus_backup_json", value).apply()

    companion object {
        private const val TAG = "FirebaseBackup"
        
        // Use compile-time parsed Client ID from google-services.json, fallback to active Web Client ID
        val DEFAULT_CLIENT_ID: String
            get() = if (com.example.BuildConfig.AUTO_CLIENT_ID.isNotEmpty()) {
                com.example.BuildConfig.AUTO_CLIENT_ID
            } else {
                "671831869873-42flh4g8adsplje9gl23qdkmf040mq31.apps.googleusercontent.com"
            }
        
        const val DEFAULT_CLIENT_SECRET = "GOCSPX-studytrackerbdsecret"
        val REDIRECT_URI: String
            get() = "https://${if (com.example.BuildConfig.AUTO_PROJECT_ID.isNotEmpty()) com.example.BuildConfig.AUTO_PROJECT_ID else "studytracker-bd"}.firebaseapp.com/__/auth/handler"

        // Hardcoded Firebase Config from user's request
        val HARDCODED_API_KEY = if (com.example.BuildConfig.AUTO_API_KEY.isNotEmpty()) {
            com.example.BuildConfig.AUTO_API_KEY
        } else {
            "AIzaSyC9jnuJ0UOygVxG8UdecJOpFtgpnfUAR_g"
        }
        
        val HARDCODED_DATABASE_URL = if (com.example.BuildConfig.AUTO_DB_URL.isNotEmpty()) {
            com.example.BuildConfig.AUTO_DB_URL
        } else {
            "https://studytracker-bd-default-rtdb.firebaseio.com"
        }
    }

    // --- Firebase & OAuth Configuration ---
    var firebaseApiKey: String
        get() = HARDCODED_API_KEY
        set(value) {}

    var firebaseDatabaseUrl: String
        get() = HARDCODED_DATABASE_URL
        set(value) {}

    var oauthClientId: String
        get() = DEFAULT_CLIENT_ID
        set(value) {}

    var oauthClientSecret: String
        get() = DEFAULT_CLIENT_SECRET
        set(value) {}

    var useSandbox: Boolean
        get() = false
        set(value) {}

    private var sandboxBackupData: String?
        get() = prefs.getString("sandbox_backup_json", null)
        set(value) = prefs.edit().putString("sandbox_backup_json", value).apply()

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean("auto_backup_enabled", true) // Default to auto-backup enabled for high trust
        set(value) = prefs.edit().putBoolean("auto_backup_enabled", value).apply()

    // --- Auth States ---
    var isUserSignedIn: Boolean
        get() = prefs.getBoolean("is_signed_in", false)
        private set(value) = prefs.edit().putBoolean("is_signed_in", value).apply()

    var googleUserName: String?
        get() = prefs.getString("user_name", null)
        private set(value) = prefs.edit().putString("user_name", value).apply()

    var googleUserEmail: String?
        get() = prefs.getString("user_email", null)
        private set(value) = prefs.edit().putString("user_email", value).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        private set(value) = prefs.edit().putString("user_id", value).apply()

    var firebaseIdToken: String?
        get() = prefs.getString("fb_id_token", null)
        private set(value) = prefs.edit().putString("fb_id_token", value).apply()

    var firebaseRefreshToken: String?
        get() = prefs.getString("fb_refresh_token", null)
        private set(value) = prefs.edit().putString("fb_refresh_token", value).apply()

    var tokenExpiryMillis: Long
        get() = prefs.getLong("token_expiry", 0L)
        private set(value) = prefs.edit().putLong("token_expiry", value).apply()

    // --- SignIn/SignOut Actions ---
    fun signInAsSandbox() {
        useSandbox = true
        isUserSignedIn = true
        googleUserName = "মেডিক্যাসপিয়ান (ডেমো)"
        googleUserEmail = "medprep.sandbox@gmail.com"
        userId = "sandbox_user_id"
    }

    suspend fun signInToFirebaseAnonymously(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$firebaseApiKey"
            val jsonPayload = JSONObject().apply {
                put("returnSecureToken", true)
            }.toString()

            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Firebase Guest authentication failed: $responseStr"))
                }

                val json = JSONObject(responseStr)
                val fbIdToken = json.getString("idToken")
                val fbRefreshToken = json.getString("refreshToken")
                val expiresSec = json.optLong("expiresIn", 3600L)
                val fbLocalId = json.getString("localId")

                firebaseIdToken = fbIdToken
                firebaseRefreshToken = fbRefreshToken
                tokenExpiryMillis = System.currentTimeMillis() + (expiresSec * 1000)
                userId = fbLocalId
                googleUserEmail = "guest.${fbLocalId.take(6)}@medprep.com"
                googleUserName = "অতিথি ইউজার (${fbLocalId.take(6)})"
                useSandbox = false
                isUserSignedIn = true

                // Try auto-restoring database backup when signing in
                performRestore()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Anonymous Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInToFirebaseWithEmailPassword(email: String, password: String, isSignUp: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (isSignUp) "signUp" else "signInWithPassword"
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:$endpoint?key=$firebaseApiKey"
            val jsonPayload = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            }.toString()

            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorObj = JSONObject(responseStr).optJSONObject("error")
                    val message = errorObj?.optString("message") ?: "অনাকাঙ্ক্ষিত ত্রুটি সংঘটিত হয়েছে"
                    val bngMessage = when(message) {
                        "EMAIL_EXISTS" -> "এই ইমেইলটি দিয়ে ইতিমধ্যে অ্যাকাউন্ট তৈরি করা আছে!"
                        "INVALID_PASSWORD" -> "ভুল পাসওয়ার্ড! অনুগ্রহ করে সঠিক পাসওয়ার্ড প্রদান করুন।"
                        "EMAIL_NOT_FOUND" -> "এই ইমেইল দিয়ে কোনো অ্যাকাউন্ট খুঁজে পাওয়া যায়নি!"
                        "INVALID_EMAIL" -> "ইমেইলটি সঠিক ফরম্যাটে নয়!"
                        "WEAK_PASSWORD" -> "পাসওয়ার্ড দুর্বল! অন্তত ৬ অক্ষরের পাসওয়ার্ড ব্যবহার করুন।"
                        else -> "ক্লাউড অথেনটিকেশন ব্যর্থ: $message"
                    }
                    return@withContext Result.failure(Exception(bngMessage))
                }

                val json = JSONObject(responseStr)
                val fbIdToken = json.getString("idToken")
                val fbRefreshToken = json.getString("refreshToken")
                val expiresSec = json.optLong("expiresIn", 3600L)
                val fbLocalId = json.getString("localId")
                val userMail = json.optString("email", email)

                firebaseIdToken = fbIdToken
                firebaseRefreshToken = fbRefreshToken
                tokenExpiryMillis = System.currentTimeMillis() + (expiresSec * 1000)
                userId = fbLocalId
                googleUserEmail = userMail
                googleUserName = userMail.substringBefore("@")
                useSandbox = false
                isUserSignedIn = true

                // Try auto-restoring database backup when signing in
                performRestore()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Email Sign-In failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        isUserSignedIn = false
        googleUserName = null
        googleUserEmail = null
        userId = null
        firebaseIdToken = null
        firebaseRefreshToken = null
        tokenExpiryMillis = 0L
    }

    fun generateAuthorizationUrl(): String {
        val scope = URLEncoder.encode("https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email", "UTF-8")
        val redirect = URLEncoder.encode(REDIRECT_URI, "UTF-8")
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$oauthClientId" +
                "&redirect_uri=$redirect" +
                "&response_type=code" +
                "&scope=$scope" +
                "&prompt=consent" +
                "&access_type=offline"
    }

    suspend fun handleOAuthRedirectCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Trade Google Redirection Code for Google Token (including id_token)
            val requestBody = FormBody.Builder()
                .add("code", code)
                .add("client_id", oauthClientId)
                .add("client_secret", oauthClientSecret)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Google Token exchanges failed: $body"))
                }

                val json = JSONObject(response.body!!.string())
                val googleIdToken = json.getString("id_token")

                // 2. Authenticate with Firebase using Google ID Token
                signInToFirebaseWithGoogle(googleIdToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "OAuth token exchange exception", e)
            Result.failure(e)
        }
    }

    private suspend fun signInToFirebaseWithGoogle(googleIdToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$firebaseApiKey"
            val postBodyStr = "id_token=$googleIdToken&providerId=google.com"
            val fbPayload = JSONObject().apply {
                put("postBody", postBodyStr)
                put("requestUri", REDIRECT_URI)
                put("returnIdpCredential", true)
                put("returnSecureToken", true)
            }.toString()

            val requestBody = fbPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Firebase entry failed: $responseStr"))
                }

                val json = JSONObject(responseStr)
                val fbIdToken = json.getString("idToken")
                val fbRefreshToken = json.getString("refreshToken")
                val expiresSec = json.optLong("expiresIn", 3600L)
                val fbLocalId = json.getString("localId")
                val email = json.optString("email", "medprep.user@gmail.com")
                val displayName = json.optString("displayName", "গুগল ইউজার")

                firebaseIdToken = fbIdToken
                firebaseRefreshToken = fbRefreshToken
                tokenExpiryMillis = System.currentTimeMillis() + (expiresSec * 1000)
                userId = fbLocalId
                googleUserEmail = email
                googleUserName = displayName
                useSandbox = false
                isUserSignedIn = true

                // Try auto-restoring database backup when signing in
                performRestore()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Sign-In failed", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureFreshToken(): String? {
        if (useSandbox) return "sandbox_token"

        val currentToken = firebaseIdToken ?: return null
        if (System.currentTimeMillis() < tokenExpiryMillis - 60000L) {
            return currentToken
        }

        val refresh = firebaseRefreshToken ?: return null
        return refreshFirebaseToken(refresh)
    }

    private suspend fun refreshFirebaseToken(refresh: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://securetoken.googleapis.com/v1/token?key=$firebaseApiKey"
            val payload = "grant_type=refresh_token&refresh_token=$refresh"
            val requestBody = payload.toRequestBody("application/x-www-form-urlencoded".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    val newIdToken = json.getString("id_token")
                    val newRefreshToken = json.getString("refresh_token")
                    val expiresSec = json.optLong("expires_in", 3600L)

                    firebaseIdToken = newIdToken
                    firebaseRefreshToken = newRefreshToken
                    tokenExpiryMillis = System.currentTimeMillis() + (expiresSec * 1000)
                    newIdToken
                } else {
                    Log.e(TAG, "Failed to refresh firebase token: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception refreshing firebase token", e)
            null
        }
    }

    // --- Core Database Sync (Backup and Restore) ---

    suspend fun performBackup(): Result<String> = withContext(Dispatchers.IO) {
        if (!isUserSignedIn) {
            return@withContext Result.failure(Exception("ব্যাকআপ করার পূর্বে অনুগ্রহ করে লগইন সম্পন্ন করুন!"))
        }

        // 1. Gather all study sessions and syllabus completions
        val sessionsJsonArray = JSONArray()
        try {
            studySessionDao.getAllSessionsImmediate().forEach { s ->
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("subject", s.subject)
                    put("chapter", s.chapter)
                    put("durationMinutes", s.durationMinutes)
                    put("sessionType", s.sessionType)
                    put("dateMillis", s.dateMillis)
                }
                sessionsJsonArray.put(sObj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error gathering sessions for backup", e)
        }

        val syllabusJsonArray = JSONArray()
        try {
            syllabusCompletionDao.getAllCompletionsImmediate().forEach { c ->
                val cObj = JSONObject().apply {
                    put("chapterKey", c.chapterKey)
                    put("subjectName", c.subjectName)
                    put("chapterName", c.chapterName)
                    put("isCompleted", c.isCompleted)
                    put("lastUpdated", c.lastUpdated)
                }
                syllabusJsonArray.put(cObj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error gathering syllabus completions for backup", e)
        }

        if (useSandbox) {
            try {
                sandboxBackupData = sessionsJsonArray.toString()
                sandboxSyllabusBackupData = syllabusJsonArray.toString()
                return@withContext Result.success("স্যান্ডবক্স ক্লাউড ব্যাকআপ সফল! মোট ${sessionsJsonArray.length()} টি সেশন এবং ${syllabusJsonArray.length()} টি অধ্যায় অগ্রগতি ব্যাকআপ করা হয়েছে।")
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        val currentUserId = userId ?: return@withContext Result.failure(Exception("ইউজার আইডি পাওয়া যায়নি! পুনরায় সাইন-ইন করুন।"))

        try {
            val token = ensureFreshToken() ?: return@withContext Result.failure(Exception("Firebase token requires fresh login."))
            val cleanedDbUrl = firebaseDatabaseUrl.trim().removeSuffix("/")

            // Back up sessions
            val sessionsUrl = "$cleanedDbUrl/backups/$currentUserId.json?auth=$token"
            val sessionsBody = sessionsJsonArray.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val sessionsRequest = Request.Builder().url(sessionsUrl).put(sessionsBody).build()

            httpClient.newCall(sessionsRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("সেশন ব্যাকআপ আপলোড ব্যর্থ হয়েছে! কোড: ${response.code}, মেসেজ: $responseStr"))
                }
            }

            // Back up syllabus completions
            val syllabusUrl = "$cleanedDbUrl/syllabus_completions/$currentUserId.json?auth=$token"
            val syllabusBody = syllabusJsonArray.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val syllabusRequest = Request.Builder().url(syllabusUrl).put(syllabusBody).build()

            httpClient.newCall(syllabusRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("সিলেবাস অগ্রগতি ব্যাকআপ ব্যর্থ হয়েছে! কোড: ${response.code}, মেসেজ: $responseStr"))
                }
            }

            Result.success("ক্লাউড ব্যাকআপ সম্পূর্ণ সফল! মোট ${sessionsJsonArray.length()} টি সেশন ও ${syllabusJsonArray.length()} টি অধ্যায় অগ্রগতি সংরক্ষিত হয়েছে।")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing backup", e)
            Result.failure(e)
        }
    }

    suspend fun performRestore(): Result<String> = withContext(Dispatchers.IO) {
        if (!isUserSignedIn) {
            return@withContext Result.failure(Exception("রিস্টোর করার পূর্বে অনুগ্রহ করে লগইন সম্পন্ন করুন!"))
        }

        if (useSandbox) {
            try {
                // Restore sessions
                val sessionsPayloadStr = sandboxBackupData ?: "[]"
                val listToRestore = mutableListOf<StudySession>()
                val sessionsArray = JSONArray(sessionsPayloadStr)
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(i)
                    listToRestore.add(
                        StudySession(
                            id = 0,
                            subject = sObj.getString("subject"),
                            chapter = sObj.getString("chapter"),
                            durationMinutes = sObj.getInt("durationMinutes"),
                            sessionType = sObj.optString("sessionType", "পাঠ সেশন"),
                            dateMillis = sObj.getLong("dateMillis")
                        )
                    )
                }
                studySessionDao.clearAllSessions()
                if (listToRestore.isNotEmpty()) {
                    studySessionDao.insertSessions(listToRestore)
                }

                // Restore syllabus completions
                val syllabusPayloadStr = sandboxSyllabusBackupData ?: "[]"
                val completionsToRestore = mutableListOf<SyllabusCompletion>()
                val syllabusArray = JSONArray(syllabusPayloadStr)
                for (i in 0 until syllabusArray.length()) {
                    val cObj = syllabusArray.getJSONObject(i)
                    completionsToRestore.add(
                        SyllabusCompletion(
                            chapterKey = cObj.getString("chapterKey"),
                            subjectName = cObj.getString("subjectName"),
                            chapterName = cObj.getString("chapterName"),
                            isCompleted = cObj.getBoolean("isCompleted"),
                            lastUpdated = cObj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
                syllabusCompletionDao.clearAllCompletions()
                if (completionsToRestore.isNotEmpty()) {
                    syllabusCompletionDao.insertCompletions(completionsToRestore)
                }

                return@withContext Result.success("স্যান্ডবক্স রিস্টোর সফল! ${listToRestore.size} টি সেশন ও ${completionsToRestore.size} টি সিলেবাস অগ্রগতি পুনরুদ্ধার করা হযেছে।")
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        val currentUserId = userId ?: return@withContext Result.failure(Exception("ইউজার আইডি পাওয়া যায়নি! পুনরায় সাইন-ইন করুন।"))

        try {
            val token = ensureFreshToken() ?: return@withContext Result.failure(Exception("Firebase token requires fresh login."))
            val cleanedDbUrl = firebaseDatabaseUrl.trim().removeSuffix("/")

            // 1. Restore sessions
            val sessionsPayloadStr = run {
                val url = "$cleanedDbUrl/backups/$currentUserId.json?auth=$token"
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("সেশন ডেটা পুনরুদ্ধার ব্যর্থ! কোড: ${response.code}"))
                    }
                    if (responseStr.trim() == "null" || responseStr.trim().isEmpty()) {
                        "[]"
                    } else {
                        responseStr
                    }
                }
            }

            val listToRestore = mutableListOf<StudySession>()
            val sessionsArray = JSONArray(sessionsPayloadStr)
            for (i in 0 until sessionsArray.length()) {
                val sObj = sessionsArray.getJSONObject(i)
                listToRestore.add(
                    StudySession(
                        id = 0,
                        subject = sObj.getString("subject"),
                        chapter = sObj.getString("chapter"),
                        durationMinutes = sObj.getInt("durationMinutes"),
                        sessionType = sObj.optString("sessionType", "পাঠ সেশন"),
                        dateMillis = sObj.getLong("dateMillis")
                    )
                )
            }

            studySessionDao.clearAllSessions()
            if (listToRestore.isNotEmpty()) {
                studySessionDao.insertSessions(listToRestore)
            }

            // 2. Restore syllabus completions
            val syllabusPayloadStr = run {
                val url = "$cleanedDbUrl/syllabus_completions/$currentUserId.json?auth=$token"
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        if (responseStr.trim() == "null" || responseStr.trim().isEmpty()) {
                            "[]"
                        } else {
                            responseStr
                        }
                    } else {
                        "[]"
                    }
                }
            }

            val completionsToRestore = mutableListOf<SyllabusCompletion>()
            val syllabusArray = JSONArray(syllabusPayloadStr)
            for (i in 0 until syllabusArray.length()) {
                val cObj = syllabusArray.getJSONObject(i)
                completionsToRestore.add(
                    SyllabusCompletion(
                        chapterKey = cObj.getString("chapterKey"),
                        subjectName = cObj.getString("subjectName"),
                        chapterName = cObj.getString("chapterName"),
                        isCompleted = cObj.getBoolean("isCompleted"),
                        lastUpdated = cObj.optLong("lastUpdated", System.currentTimeMillis())
                    )
                )
            }

            syllabusCompletionDao.clearAllCompletions()
            if (completionsToRestore.isNotEmpty()) {
                syllabusCompletionDao.insertCompletions(completionsToRestore)
            }

            Result.success("পুনরুদ্ধার সফল! ${listToRestore.size} টি সেশন এবং ${completionsToRestore.size} টি সিলেবাস অধ্যায় অগ্রগতি সঠিকভাবে পুনরুদ্ধার হয়েছে।")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing restore", e)
            Result.failure(e)
        }
    }
}
