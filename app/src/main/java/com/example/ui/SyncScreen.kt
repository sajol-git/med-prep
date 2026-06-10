package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FirebaseBackupManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemScreen(viewModel: MainViewModel) {
    val backupManager = viewModel.backupManager
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe State directly from shared preferences and properties
    var isUserSignedIn by remember { mutableStateOf(backupManager.isUserSignedIn) }
    var userName by remember { mutableStateOf(backupManager.googleUserName) }
    var userEmail by remember { mutableStateOf(backupManager.googleUserEmail) }
    
    var autoBackupEnabled by remember { mutableStateOf(backupManager.autoBackupEnabled) }

    // UI Toggles
    var showOAuthDialog by remember { mutableStateOf(false) }

    // Loading & Status states
    var isSyncLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    var manualStatusMessage by remember { mutableStateOf<String?>(null) }
    var isManualStatusError by remember { mutableStateOf(false) }
    var isManualSyncLoading by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        isManualSyncLoading = true
                        val jsonStr = viewModel.exportBackupDataJson()
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(jsonStr.toByteArray(Charsets.UTF_8))
                        }
                        manualStatusMessage = "ব্যাকআপ ফাইলটি ডিভাইসের মেমোরিতে সফলভাবে ডাউনলোড করা হয়েছে!"
                        isManualStatusError = false
                    } catch (e: Exception) {
                        manualStatusMessage = "ডাউনলোড ব্যর্থ হয়েছে: ${e.localizedMessage}"
                        isManualStatusError = true
                    } finally {
                        isManualSyncLoading = false
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        isManualSyncLoading = true
                        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes().toString(Charsets.UTF_8)
                        } ?: ""
                        
                        val res = viewModel.restoreBackupDataJson(content)
                        res.fold(
                            onSuccess = { msg ->
                                manualStatusMessage = msg
                                isManualStatusError = false
                            },
                            onFailure = { err ->
                                manualStatusMessage = "রিস্টোর ব্যর্থ: ${err.localizedMessage}"
                                isManualStatusError = true
                            }
                        )
                    } catch (e: Exception) {
                        manualStatusMessage = "ফাইল পড়তে ব্যর্থ হয়েছে: ${e.localizedMessage}"
                        isManualStatusError = true
                    } finally {
                        isManualSyncLoading = false
                    }
                }
            }
        }
    )

    fun refreshStates() {
        isUserSignedIn = backupManager.isUserSignedIn
        userName = backupManager.googleUserName
        userEmail = backupManager.googleUserEmail
        autoBackupEnabled = backupManager.autoBackupEnabled
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // --- HEADER SECTION ---
        item {
            TopAppBarSection()
        }

        // --- SYSTEM SETTINGS PANEL (SOUNDS) ---
        item {
            val prefs = remember { context.getSharedPreferences("medprep_prefs", android.content.Context.MODE_PRIVATE) }
            var soundEffectsEnabled by remember { mutableStateOf(prefs.getBoolean("sound_effects_enabled", true)) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D3748))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2CD4A0).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Sound Settings",
                                tint = Color(0xFF2CD4A0),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "সিস্টেম ও সাউন্ড সেটিংস",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "সাউন্ড এবং অ্যালার্ম মেকানিজম সেটিংস এখান থেকে কন্ট্রোল করুন।",
                                color = GrayText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color(0xFF2D3748))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "সাউন্ড ইফেক্ট অন রাখুন",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "টাইমার চলাকালীন সেকেন্ডের বীট ও টাইমার সফলভাবে সম্পন্ন হওয়ার জোরে অ্যালার্ম সাউন্ড অন বা অফ করুন।",
                                color = GrayText,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = soundEffectsEnabled,
                            onCheckedChange = { checked ->
                                prefs.edit().putBoolean("sound_effects_enabled", checked).apply()
                                soundEffectsEnabled = checked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2CD4A0),
                                checkedTrackColor = Color(0xFF2CD4A0).copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // --- STATUS BANNERS & NOTIFICATIONS ---
        if (statusMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStatusError) Color(0xFF7F1D1D) else Color(0xFF0F5132)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isStatusError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = "Status",
                            tint = if (isStatusError) Color(0xFFFECACA) else Color(0xFFA7F3D0),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusMessage ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1.0f)
                        )
                        IconButton(
                            onClick = { statusMessage = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // --- CORE AUTH PANEL ---
        item {
            if (!isUserSignedIn) {
                // Not Signed In Layout
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D3748))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Cloud,
                                    contentDescription = "Cloud",
                                    tint = Color(0xFFEA4335),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "নিরাপদ ক্লাউড সংযোগ প্রয়োজন",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "আপনার স্টাডি সেশন এবং সিলেবাস অগ্রগতি নিরাপদে ব্যাকআপ করতে নিচে গুগল অ্যাকাউন্ট দিয়ে লগইন করুন।",
                                    color = GrayText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Premium Official Google Sign-In Button with Colored Google G Logo
                        Button(
                            onClick = {
                                statusMessage = null
                                showOAuthDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_signin_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F2937)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_google),
                                contentDescription = "Google Logo",
                                tint = Color.Unspecified, // Keep original segment colors
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন",
                                color = Color(0xFF1F2937),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Signed In Layout
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2D3748))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF3B82F6), Color(0xFF2CD4A0))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = userName ?: "গুগল অ্যাকাউন্ট ইউজার",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2CD4A0))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "গুগল ক্লাউড সিঙ্ক সক্রিয়",
                                        color = Color(0xFF2CD4A0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = userEmail ?: "medprep.user@gmail.com",
                            color = GrayText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 62.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = Color(0xFF2D3748))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Automatic backup on saving timers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = "স্বয়ংক্রিয় ব্যাকআপ অন রাখুন",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "পড়াশোনার সেশন টাইমার সেভ করার পর ব্যাকগ্রাউন্ডে ডেটা অটোমেটিক ব্যাকআপ হতে থাকবে।",
                                    color = GrayText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { checked ->
                                    backupManager.autoBackupEnabled = checked
                                    refreshStates()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF2CD4A0),
                                    checkedTrackColor = Color(0xFF2CD4A0).copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Manual Backups
                        if (isSyncLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color(0xFF2CD4A0), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("ক্লাউডের সাথে ডেটা ব্যাকআপ ও সিঙ্ক হচ্ছে...", color = GrayText, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // BACKUP BUTTON
                                Button(
                                    onClick = {
                                        isSyncLoading = true
                                        statusMessage = null
                                        viewModel.triggerBackup { result ->
                                            isSyncLoading = false
                                            result.fold(
                                                onSuccess = { msg ->
                                                    statusMessage = msg
                                                    isStatusError = false
                                                },
                                                onFailure = { err ->
                                                    statusMessage = "ব্যাকআপ ব্যর্থ: ${err.localizedMessage}"
                                                    isStatusError = true
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1.0f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2CD4A0)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Backup", modifier = Modifier.size(16.dp), tint = Color(0xFF0C1017))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ক্লাউডে ব্যাকআপ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0C1017))
                                }

                                // RESTORE BUTTON
                                Button(
                                    onClick = {
                                        isSyncLoading = true
                                        statusMessage = null
                                        viewModel.triggerRestore { result ->
                                            isSyncLoading = false
                                            result.fold(
                                                onSuccess = { msg ->
                                                    statusMessage = msg
                                                    isStatusError = false
                                                },
                                                onFailure = { err ->
                                                    statusMessage = "রিস্টোর ব্যর্থ: ${err.localizedMessage}"
                                                    isStatusError = true
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1.0f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ক্লাউড রিস্টোর", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign Out Button
                        OutlinedButton(
                            onClick = {
                                backupManager.signOut()
                                refreshStates()
                                statusMessage = "গুগল অ্যাকাউন্ট সাইন-আউট সফল হয়েছে!"
                                isStatusError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গুগল অ্যাকাউন্ট ডিকানেক্ট করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- MANUAL LOCAL BACKUP & RESTORE PANEL ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D3748))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Manual Backup",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "ম্যানুয়াল ফাইল ব্যাকআপ ও রিস্টোর",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ডিভাইসের লোকাল ফাইল হিসেবে ব্যাকআপ ডাউনলোড করুন এবং পরবর্তীতে রিস্টোর করতে তা সিলেক্ট করে আপলোড করুন।",
                                color = GrayText,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (manualStatusMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isManualStatusError) Color(0xFF7F1D1D) else Color(0xFF0F5132)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isManualStatusError) Icons.Default.Error else Icons.Default.CheckCircle,
                                    contentDescription = "Status",
                                    tint = if (isManualStatusError) Color(0xFFFECACA) else Color(0xFFA7F3D0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = manualStatusMessage ?: "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1.0f)
                                )
                                IconButton(
                                    onClick = { manualStatusMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isManualSyncLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("প্রসেসিং হচ্ছে...", color = GrayText, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Local Export Button
                            Button(
                                onClick = {
                                    manualStatusMessage = null
                                    exportLauncher.launch("medprep_local_backup.json")
                                },
                                modifier = Modifier.weight(1.0f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Save file", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ব্যাকআপ ডাউনলোড", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Local Import Button
                            Button(
                                onClick = {
                                    manualStatusMessage = null
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                                modifier = Modifier.weight(1.0f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = "Upload file", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফাইল আপলোড", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- GOOGLE OAUTH DIALOG POPUP ---
    if (showOAuthDialog) {
        val authUrl = backupManager.generateAuthorizationUrl()
        Dialog(
            onDismissRequest = { showOAuthDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Safe Lock",
                                tint = Color(0xFF2CD4A0),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "নিরাপদ গুগল সংযোগ...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showOAuthDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close dialog", tint = Color.White)
                        }
                    }

                    // Native WebView OAuth client rendering
                    Box(modifier = Modifier.weight(1.0f)) {
                        OAuthWebView(
                            url = authUrl,
                            onCodeReceived = { code ->
                                showOAuthDialog = false
                                isSyncLoading = true
                                statusMessage = "গুগল ক্লাউড কানেক্ট করা হচ্ছে..."
                                scope.launch {
                                    val res = backupManager.handleOAuthRedirectCode(code)
                                    isSyncLoading = false
                                    res.fold(
                                        onSuccess = {
                                            refreshStates()
                                            statusMessage = "গুগল অ্যাকাউন্টের সাথে ড্রাইভ ব্যাকআপ সফলভাবে সংযুক্ত হয়েছে!"
                                            isStatusError = false
                                        },
                                        onFailure = { err ->
                                            statusMessage = "গুগল ক্লাউড কানেক্ট ব্যর্থ হয়েছে: ${err.localizedMessage}"
                                            isStatusError = true
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

object BoxDefaults {
    val BorderStroke = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D3748))
    fun outlinedBorder(color: Color): androidx.compose.foundation.BorderStroke {
        return androidx.compose.foundation.BorderStroke(1.dp, color)
    }
}

@Composable
fun OAuthWebView(url: String, onCodeReceived: (String) -> Unit) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        if (requestUrl.startsWith("http://localhost") || requestUrl.contains("__/auth/handler")) {
                            val uri = android.net.Uri.parse(requestUrl)
                            val code = uri.getQueryParameter("code")
                            if (code != null) {
                                onCodeReceived(code)
                                return true
                            }
                        }
                        return false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        @Suppress("DEPRECATION")
                        super.onPageStarted(view, url, favicon)
                        url?.let {
                            if (it.startsWith("http://localhost") || it.contains("__/auth/handler")) {
                                val uri = android.net.Uri.parse(it)
                                val code = uri.getQueryParameter("code")
                                if (code != null) {
                                    onCodeReceived(code)
                                }
                             }
                        }
                    }
                }
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                loadUrl(url)
            }
        },
        update = { _ ->
            // Optionally update
        },
        modifier = Modifier.fillMaxSize()
    )
}
