package com.example.receiver

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MedPrepApplication
import com.example.data.StudySession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "com.example.medprep.TIMER_CHANNEL"
        const val NOTIFICATION_ID = 101

        const val ACTION_START = "com.example.medprep.ACTION_START"
        const val ACTION_PAUSE = "com.example.medprep.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.medprep.ACTION_RESUME"
        const val ACTION_STOP = "com.example.medprep.ACTION_STOP"

        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_CHAPTER = "extra_chapter"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_COUNTDOWN_TARGET = "extra_countdown_target"

        // Live States accessible from UI
        val isServiceRunning = MutableStateFlow(false)
        val isTimerRunning = MutableStateFlow(false)
        val currentSeconds = MutableStateFlow(0L)
        val timerSubjectState = MutableStateFlow("")
        val timerChapterState = MutableStateFlow("")
        val timerSessionTypeState = MutableStateFlow("")
        val countdownTargetState = MutableStateFlow(0L)
        val showCompletionDialogEvent = MutableStateFlow(false)
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var tickerJob: Job? = null

    private var accumulatedTimeSeconds = 0L
    private var timerLastStartTime = 0L
    private var isCurrentlyRunning = false

    private var subject = ""
    private var chapter = ""
    private var sessionType = ""
    private var countdownTarget = 0L

    private var toneGen: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (inner: Throwable) {
                    inner.printStackTrace()
                    // Fallback to standard startForeground without specified type
                    startForeground(NOTIFICATION_ID, buildNotification())
                }
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_START -> {
                val newSubject = intent.getStringExtra(EXTRA_SUBJECT) ?: ""
                val newChapter = intent.getStringExtra(EXTRA_CHAPTER) ?: ""
                val newType = intent.getStringExtra(EXTRA_TYPE) ?: ""
                val target = intent.getLongExtra(EXTRA_COUNTDOWN_TARGET, 0L)

                subject = newSubject
                chapter = newChapter
                sessionType = newType
                countdownTarget = target

                accumulatedTimeSeconds = 0L
                timerLastStartTime = System.currentTimeMillis()
                isCurrentlyRunning = true

                timerSubjectState.value = subject
                timerChapterState.value = chapter
                timerSessionTypeState.value = sessionType
                countdownTargetState.value = countdownTarget
                
                // Immediately initialize currentSeconds to starting value to avoid initial 00:00 flicker
                currentSeconds.value = if (countdownTarget > 0) countdownTarget else 0L
                
                isServiceRunning.value = true
                isTimerRunning.value = true

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        try {
                            startForeground(
                                NOTIFICATION_ID,
                                buildNotification(),
                                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            )
                        } catch (inner: Throwable) {
                            inner.printStackTrace()
                            // Fallback to standard startForeground without specified type
                            startForeground(NOTIFICATION_ID, buildNotification())
                        }
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                startTicker()
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_STOP -> {
                stopAndSaveTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && isCurrentlyRunning) {
                val elapsed = getElapsedSeconds()
                val isSoundEnabled = getSharedPreferences("medprep_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("sound_effects_enabled", true)
                if (countdownTarget > 0) {
                    val remaining = countdownTarget - elapsed
                    if (remaining <= 0) {
                        currentSeconds.value = 0L
                        if (isSoundEnabled) {
                            try {
                                toneGen?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000)
                            } catch (e: Exception) {}
                            
                            try {
                                val alertUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, alertUri)
                                ringtone?.play()
                            } catch (e: Exception) {}
                        }
                        
                        showCompletionDialogEvent.value = true
                        stopAndSaveTimer()
                        break
                    } else {
                        currentSeconds.value = remaining
                    }
                } else {
                    currentSeconds.value = elapsed
                }

                if (isSoundEnabled) {
                    try {
                        toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                    } catch (e: Exception) {}
                }

                updateNotification()
                delay(1000)
            }
        }
    }

    private fun getElapsedSeconds(): Long {
        if (isCurrentlyRunning) {
            val now = System.currentTimeMillis()
            return accumulatedTimeSeconds + (now - timerLastStartTime) / 1000
        }
        return accumulatedTimeSeconds
    }

    private fun pauseTimer() {
        if (isCurrentlyRunning) {
            val now = System.currentTimeMillis()
            accumulatedTimeSeconds += (now - timerLastStartTime) / 1000
            isCurrentlyRunning = false
            tickerJob?.cancel()
            isTimerRunning.value = false
            isServiceRunning.value = true

            val remainingOrElapsed = if (countdownTarget > 0) {
                val r = countdownTarget - accumulatedTimeSeconds
                if (r < 0) 0L else r
            } else {
                accumulatedTimeSeconds
            }
            currentSeconds.value = remainingOrElapsed
            updateNotification()
        }
    }

    private fun resumeTimer() {
        if (!isCurrentlyRunning) {
            timerLastStartTime = System.currentTimeMillis()
            isCurrentlyRunning = true
            isTimerRunning.value = true
            isServiceRunning.value = true
            startTicker()
        }
    }

    private fun stopAndSaveTimer() {
        if (isCurrentlyRunning) {
            val now = System.currentTimeMillis()
            accumulatedTimeSeconds += (now - timerLastStartTime) / 1000
        }
        isCurrentlyRunning = false
        tickerJob?.cancel()

        val finalDurationSec = accumulatedTimeSeconds
        val minutes = maxOf(1, (finalDurationSec / 60).toInt())

        serviceScope.launch(Dispatchers.IO) {
            val app = application as? MedPrepApplication
            if (app != null) {
                val session = StudySession(
                    subject = if (subject.isEmpty()) "সাধারণ" else subject,
                    chapter = if (chapter.isEmpty()) "সাধারণ সেশন" else chapter,
                    durationMinutes = minutes,
                    sessionType = sessionType
                )
                app.repository.insertSession(session)
                if (app.backupManager.autoBackupEnabled && app.backupManager.isUserSignedIn) {
                    app.backupManager.performBackup()
                }
            }
        }

        accumulatedTimeSeconds = 0L
        isServiceRunning.value = false
        isTimerRunning.value = false
        currentSeconds.value = 0L
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "পড়াশোনা টাইমার",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "চলতি পড়াশোনা সেশন এবং টাইমারের তথ্য প্রদর্শন করে।"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = "SHOW_TIMER"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeIntent = if (isCurrentlyRunning) {
            Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        } else {
            Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
        }
        val pauseOrResumePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseOrResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeBtnText = if (isCurrentlyRunning) "বিরতি" else "চালু করুন"
        val pauseOrResumeIcon = if (isCurrentlyRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val elapsed = getElapsedSeconds()
        val displaySec = if (countdownTarget > 0) {
            val r = countdownTarget - elapsed
            if (r < 0) 0L else r
        } else {
            elapsed
        }
        val timeStr = String.format("%02d:%02d", displaySec / 60, displaySec % 60)
        val subtext = if (countdownTarget > 0) "$sessionType চলছে" else "সাধারণ পড়াশোনা সেশন চলছে"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (chapter.isNotEmpty()) chapter else "পড়াশোনা সেশন")
            .setContentText("$subject • $timeStr")
            .setSubText(subtext)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, com.example.R.mipmap.ic_launcher))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseOrResumeIcon, pauseOrResumeBtnText, pauseOrResumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "শেষ করুন", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceJob.cancel()
        toneGen?.release()
    }
}
