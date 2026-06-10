package com.example.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.TimerService
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(
    viewModel: MainViewModel,
    subject: String,
    chapter: String,
    sessionType: String,
    onStop: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isImmersive by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val decodedSubject = android.net.Uri.decode(subject ?: "")
    val decodedChapter = android.net.Uri.decode(chapter ?: "")
    val decodedType = android.net.Uri.decode(sessionType ?: "")

    val isPomodoro = decodedType.contains("Pomodoro") || decodedType.contains("পোমোডোরো")
    val isDeepWork = decodedType.contains("Deep") || decodedType.contains("ডিপ")
    val countdownTarget = if (isPomodoro) 25 * 60L else if (isDeepWork) 90 * 60L else 0L

    var showCompletionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val window = (context as? android.app.Activity)?.window

    // Read Foreground Service state flow variables
    val isServiceRunning by TimerService.isServiceRunning.collectAsState()
    val isTimerRunningGlobal by TimerService.isTimerRunning.collectAsState()
    val currentSecondsGlobal by TimerService.currentSeconds.collectAsState()
    val showCompletionEvent by TimerService.showCompletionDialogEvent.collectAsState()

    val currentDisplaySeconds = if (isServiceRunning) {
        if (countdownTarget > 0 && currentSecondsGlobal == 0L && isTimerRunningGlobal) {
            countdownTarget
        } else {
            currentSecondsGlobal
        }
    } else {
        if (countdownTarget > 0) countdownTarget else 0L
    }

    // Request dynamic POST_NOTIFICATIONS permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Start Foreground Timer Service on launch
    LaunchedEffect(decodedSubject, decodedChapter, decodedType) {
        if (!TimerService.isServiceRunning.value) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_SUBJECT, decodedSubject)
                putExtra(TimerService.EXTRA_CHAPTER, decodedChapter)
                putExtra(TimerService.EXTRA_TYPE, decodedType)
                putExtra(TimerService.EXTRA_COUNTDOWN_TARGET, countdownTarget)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    // Handle completion dialog event from background service
    LaunchedEffect(showCompletionEvent) {
        if (showCompletionEvent) {
            showCompletionDialog = true
            TimerService.showCompletionDialogEvent.value = false
            
            // Play a pleasant sound effect when the session completes
            try {
                val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Exit to Analysis screen when study session finishes or stops
    var serviceStartedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(isServiceRunning) {
        if (isServiceRunning) {
            serviceStartedOnce = true
        } else if (serviceStartedOnce) {
            onStop()
        }
    }

    // Hide status bar and navigation indicators during immersive modes
    DisposableEffect(Unit) {
        onDispose {
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Immersive screen timeout control
    LaunchedEffect(isTimerRunningGlobal, lastInteractionTime) {
        if (isTimerRunningGlobal) {
            isImmersive = false
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            delay(3000)
            isImmersive = true
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            isImmersive = false
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val onInteract = {
        lastInteractionTime = System.currentTimeMillis()
        isImmersive = false
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(interactionSource = interactionSource, indication = null) { onInteract() },
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimerDetails(decodedSubject, decodedChapter, decodedType, isImmersive)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.weight(1f), 
                        contentAlignment = Alignment.Center
                    ) {
                        TimerControls(
                            isRunning = isTimerRunningGlobal,
                            onPlayPause = {
                                val act = if (isTimerRunningGlobal) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
                                val intent = Intent(context, TimerService::class.java).apply { action = act }
                                try {
                                    context.startService(intent)
                                } catch (e: Throwable) { e.printStackTrace() }
                            },
                            onStop = {
                                val intent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP }
                                try {
                                    context.startService(intent)
                                } catch (e: Throwable) { e.printStackTrace() }
                            },
                            isImmersive = isImmersive
                        )
                    }
                }
                TimerTime(currentDisplaySeconds, isImmersive)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TimerDetails(decodedSubject, decodedChapter, decodedType, isImmersive)
                Spacer(modifier = Modifier.height(48.dp))
                TimerTime(currentDisplaySeconds, isImmersive)
                Spacer(modifier = Modifier.height(64.dp))
                TimerControls(
                    isRunning = isTimerRunningGlobal,
                    onPlayPause = {
                        val act = if (isTimerRunningGlobal) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
                        val intent = Intent(context, TimerService::class.java).apply { action = act }
                        try {
                            context.startService(intent)
                        } catch (e: Throwable) { e.printStackTrace() }
                    },
                    onStop = {
                        val intent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP }
                        try {
                            context.startService(intent)
                        } catch (e: Throwable) { e.printStackTrace() }
                    },
                    isImmersive = isImmersive
                )
            }
        }
        
        if (showCompletionDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCompletionDialog = false
                    onStop()
                },
                title = { Text("অসাধারণ কাজ!", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("আপনার \"$decodedType\" সফলভাবে সম্পন্ন হয়েছে।", color = GrayText) },
                confirmButton = {
                    TextButton(onClick = { 
                        showCompletionDialog = false
                        onStop()
                    }) {
                        Text("ঠিক আছে", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardBackground,
                titleContentColor = Color.White,
                textContentColor = GrayText
            )
        }
    }
}

@Composable
fun TimerDetails(subject: String, chapter: String, type: String, isImmersive: Boolean) {
    AnimatedVisibility(visible = !isImmersive) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (chapter.isNotEmpty()) chapter else "Normal Session",
                color = LightText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (subject.isNotEmpty()) subject else "General",
                color = GrayText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .background(CardBackground, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = type,
                    color = PrimaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TimerTime(elapsedSeconds: Long, isImmersive: Boolean, modifier: Modifier = Modifier) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val fontSize = if (isImmersive) 110.sp else 64.sp
    val fontColor = if (isImmersive) PrimaryTeal else OrangeAccent
    
    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        color = fontColor,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Composable
fun TimerControls(
    isRunning: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    isImmersive: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !isImmersive, modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.Center, 
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FloatingActionButton(
                onClick = onPlayPause,
                containerColor = CardBackground,
                contentColor = PrimaryTeal,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Pause/Resume")
            }

            Spacer(modifier = Modifier.width(24.dp))

            FloatingActionButton(
                onClick = onStop,
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }
        }
    }
}
