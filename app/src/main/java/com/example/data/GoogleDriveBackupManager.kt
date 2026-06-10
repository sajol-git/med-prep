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

class GoogleDriveBackupManager(
    private val context: Context,
    private val studySessionDao: StudySessionDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("google_backup_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()

    companion object {
        private const val TAG = "GoogleDriveBackup"
        
        // Default placeholders for testing / real OAuth Client setup
        const val DEFAULT_CLIENT_ID = "YOUR_CLIENT_ID_GOES_HERE.apps.googleusercontent.com"
        const val DEFAULT_CLIENT_SECRET = "YOUR_CLIENT_SECRET_GOES_HERE"
        const val REDIRECT_URI = "http://localhost"
    }

    // --- Authentication States ---
    var useSandbox: Boolean
        get() = prefs.getBoolean("use_sandbox", true)
        set(value) = prefs.edit().putBoolean("use_sandbox", value).apply()

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean("auto_backup_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_backup_enabled", value).apply()

    var isUserSignedIn: Boolean
        get() = prefs.getBoolean("is_signed_in", false)
        private set(value) = prefs.edit().putBoolean("is_signed_in", value).apply()

    var googleUserName: String?
        get() = prefs.getString("user_name", null)
        private set(value) = prefs.edit().putString("user_name", value).apply()

    var googleUserEmail: String?
        get() = prefs.getString("user_email", null)
        private set(value) = prefs.edit().putString("user_email", value).apply()

    var googleUserPhoto: String?
        get() = prefs.getString("user_photo", null)
        private set(value) = prefs.edit().putString("user_photo", value).apply()

    var oauthClientId: String
        get() = prefs.getString("oauth_client_id", DEFAULT_CLIENT_ID) ?: DEFAULT_CLIENT_ID
        set(value) = prefs.edit().putString("oauth_client_id", value).apply()

    var oauthClientSecret: String
        get() = prefs.getString("oauth_client_secret", DEFAULT_CLIENT_SECRET) ?: DEFAULT_CLIENT_SECRET
        set(value) = prefs.edit().putString("oauth_client_secret", value).apply()

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        private set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        private set(value) = prefs.edit().putString("refresh_token", value).apply()

    var tokenExpiryMillis: Long
        get() = prefs.getLong("token_expiry", 0L)
        private set(value) = prefs.edit().putLong("token_expiry", value).apply()

    // --- Sandbox Backup File ---
    // For Sandbox (out-of-the-box working option)
    private var sandboxBackupData: String?
        get() = prefs.getString("sandbox_backup_json", null)
        set(value) = prefs.edit().putString("sandbox_backup_json", value).apply()

    // --- SignIn/SignOut Actions ---
    fun signInAsSandbox() {
        useSandbox = true
        isUserSignedIn = true
        googleUserName = "মেডিক্যাসপিয়ান (ডেমো)"
        googleUserEmail = "medprep.sandbox@gmail.com"
        googleUserPhoto = null
    }

    fun signOut() {
        isUserSignedIn = false
        googleUserName = null
        googleUserEmail = null
        googleUserPhoto = null
        accessToken = null
        refreshToken = null
        tokenExpiryMillis = 0L
    }

    fun generateAuthorizationUrl(): String {
        val scope = URLEncoder.encode("https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email", "UTF-8")
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
                    return@withContext Result.failure(Exception("Token exchanges failed: Code ${response.code}, Response: $body"))
                }

                val json = JSONObject(response.body!!.string())
                val access = json.getString("access_token")
                val refresh = json.optString("refresh_token", "")
                val expiresSec = json.optLong("expires_in", 3600L)

                accessToken = access
                if (refresh.isNotEmpty()) {
                    refreshToken = refresh
                }
                tokenExpiryMillis = System.currentTimeMillis() + (expiresSec * 1000)
                useSandbox = false
                isUserSignedIn = true

                // Retrieve Profile Info
                fetchUserProfileInfo(access)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "OAuth token exchange exception", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchUserProfileInfo(token: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer $token")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    googleUserName = json.optString("name", "গুগল ইউজার")
                    googleUserEmail = json.optString("email", "medprep.user@gmail.com")
                    googleUserPhoto = json.optString("picture", "")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
        }
    }

    private suspend fun ensureFreshToken(): String? {
        if (useSandbox) return "sandbox_token"

        val currentAccess = accessToken ?: return null
        if (System.currentTimeMillis() < tokenExpiryMillis - 60000L) {
            return currentAccess
        }

        val refresh = refreshToken ?: return null
        return refreshAccessToken(refresh)
    }

    private suspend fun refreshAccessToken(refresh: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("client_id", oauthClientId)
                .add("client_secret", oauthClientSecret)
                .add("refresh_token", refresh)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    val newAccess = json.getString("access_token")
                    val expiresSec = json.optLong("expires_in", 3600L)

                    accessToken = newAccess
                    tokenExpiryMillis = System.currentTimeMillis() + (expiresSec * 1000)
                    newAccess
                } else {
                    Log.e(TAG, "Failed to refresh token: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception refreshing token", e)
            null
        }
    }

    // --- High Level Backup & Restore Sync Actions ---

    suspend fun performBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Gather all study sessions
            val sessionsJsonArray = JSONArray()
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

            val backupPayload = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("sessions", sessionsJsonArray)
                put("autoBackup", autoBackupEnabled)
            }.toString(2)

            // 2. Perform upload
            if (useSandbox) {
                sandboxBackupData = backupPayload
                val count = sessionsJsonArray.length()
                return@withContext Result.success("স্যান্ডবক্স ক্লাউড ব্যাকআপ সফল! মোট সেশন ব্যাকআপ করা হয়েছে: $count")
            }

            val token = ensureFreshToken() ?: return@withContext Result.failure(Exception("Google Account requires authentic credentials or sign-in expired."))

            // A. Search if filename "medprep_backup.json" exists in appDataFolder
            val existingFileId = findBackupFileId(token)

            // B. Upload file
            if (existingFileId != null) {
                updateBackupFile(token, existingFileId, backupPayload)
            } else {
                createBackupFile(token, backupPayload)
            }

            val count = sessionsJsonArray.length()
            Result.success("গুগল ড্রাইভ ক্লাউড ব্যাকআপ সফল! মোট সেশন: $count")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing backup", e)
            Result.failure(e)
        }
    }

    suspend fun performRestore(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupPayloadStr = if (useSandbox) {
                sandboxBackupData ?: return@withContext Result.failure(Exception("স্যান্ডবক্সে কোনো পূর্ববর্তী ক্লাউড ব্যাকআপ খুঁজে পাওয়া যায়নি।"))
            } else {
                val token = ensureFreshToken() ?: return@withContext Result.failure(Exception("Google Account authentication required."))
                val fileId = findBackupFileId(token) ?: return@withContext Result.failure(Exception("গুগল ড্রাইভে কোনো পূর্ববর্তী ক্লাউড ব্যাকআপ খুঁজে পাওয়া যায়নি।"))
                downloadBackupFileContent(token, fileId)
            }

            // Parse backup payload
            val json = JSONObject(backupPayloadStr)
            val version = json.optInt("version", 1)
            val sessionsArray = json.getJSONArray("sessions")

            val listToRestore = mutableListOf<StudySession>()
            for (i in 0 until sessionsArray.length()) {
                val sObj = sessionsArray.getJSONObject(i)
                listToRestore.add(
                    StudySession(
                        id = 0, // Let autogenerate id on database local insert to avoid primary key conflicts
                        subject = sObj.getString("subject"),
                        chapter = sObj.getString("chapter"),
                        durationMinutes = sObj.getInt("durationMinutes"),
                        sessionType = sObj.getString("sessionType"),
                        dateMillis = sObj.getLong("dateMillis")
                    )
                )
            }

            // Restore on Database
            studySessionDao.clearAllSessions()
            studySessionDao.insertSessions(listToRestore)

            Result.success("রিস্টোর সফল! ${listToRestore.size} টি সেশন পুনরুদ্ধার করা হয়েছে।")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing restore", e)
            Result.failure(e)
        }
    }

    // --- Google Drive API Interfacing Methods ---

    private suspend fun findBackupFileId(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files" +
                    "?q=" + URLEncoder.encode("name = 'medprep_backup.json' and 'appDataFolder' in parents", "UTF-8") +
                    "&spaces=appDataFolder" +
                    "&fields=files(id)"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val files = json.getJSONArray("files")
                    if (files.length() > 0) {
                        return@withContext files.getJSONObject(0).getString("id")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup file", e)
        }
        null
    }

    private suspend fun createBackupFile(token: String, payload: String) = withContext(Dispatchers.IO) {
        val boundary = "BackupBoundary"
        val requestBodyStr = buildMultipartBody(payload, boundary)

        val requestBody = requestBodyStr.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IOException("Create file failed: Code ${response.code}, Response: $errorBody")
            }
        }
    }

    private suspend fun updateBackupFile(token: String, fileId: String, payload: String) = withContext(Dispatchers.IO) {
        val boundary = "BackupBoundary"
        val requestBodyStr = buildMultipartBody(payload, boundary)

        val requestBody = requestBodyStr.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart")
            .header("Authorization", "Bearer $token")
            .patch(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IOException("Update file failed: Code ${response.code}, Response: $errorBody")
            }
        }
    }

    private suspend fun downloadBackupFileContent(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string() ?: throw IOException("Empty payload returned from Google Drive")
            } else {
                throw IOException("Download failed with response code ${response.code}")
            }
        }
    }

    private fun buildMultipartBody(payload: String, boundary: String): String {
        val metadata = JSONObject().apply {
            put("name", "medprep_backup.json")
            put("parents", JSONArray().put("appDataFolder"))
        }.toString()

        return "--$boundary\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                "$metadata\r\n" +
                "--$boundary\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                "$payload\r\n" +
                "--$boundary--"
    }
}
