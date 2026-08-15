package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.example.data.StudySession
import com.example.ui.components.TopAppBarSection
import com.example.ui.theme.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

private fun getSubjectEnglish(subj: String): String {
    val s = subj.trim().lowercase()
    return when {
        s.contains("botany") || s.contains("উদ্ভিদবিজ্ঞান") -> "Botany"
        s.contains("zoology") || s.contains("প্রাণিবিজ্ঞান") -> "Zoology"
        s.contains("chemistry 1st") || s.contains("রসায়ন ১ম") || s.contains("রসায়ন ১ম") -> "Chemistry 1st Paper"
        s.contains("chemistry 2nd") || s.contains("রসায়ন ২য়") || s.contains("রসায়ন ২য়") -> "Chemistry 2nd Paper"
        s.contains("chemistry") || s.contains("রসায়ন") || s.contains("রসায়ন") || s.contains("কেমিস্ট্রি") -> "Chemistry"
        s.contains("physics 1st") || s.contains("পদার্থবিজ্ঞান ১ম") -> "Physics 1st Paper"
        s.contains("physics 2nd") || s.contains("পদার্থবিজ্ঞান ২য়") || s.contains("পদার্থবিজ্ঞান ২য়") -> "Physics 2nd Paper"
        s.contains("physics") || s.contains("পদার্থবিজ্ঞান") || s.contains("ফিজিক্স") -> "Physics"
        s.contains("english") || s.contains("ইংরেজি") -> "English"
        s.contains("general knowledge") || s.contains("সাধারণ জ্ঞান") || s.contains("জিকে") || s.contains("gk") -> "General Knowledge"
        s.contains("সাধারণ") || s.contains("general") || s.contains("normal") -> "General"
        s.contains("onnanno") || s.contains("অন্যান্য") || s.contains("others") -> "Others"
        else -> subj
    }
}

private fun getSessionTypeEnglish(stype: String): String {
    return when (stype.trim()) {
        "পোমোডোরো (২৫ মি)", "Pomodoro", "পোমোডোরো" -> "Pomodoro"
        "সাধারণ সেশন", "Normal Session", "সাধারণ" -> "Normal Session"
        "ডিপ ওয়ার্ক", "Deep Work" -> "Deep Work"
        else -> stype
    }
}

private fun toBanglaDigitsInt(num: Int): String {
    return num.toString()
}

private fun formatBanglaDate(timeMillis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timeMillis }
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val monthIndex = cal.get(java.util.Calendar.MONTH)
    val year = cal.get(java.util.Calendar.YEAR)
    
    val englishMonths = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val monthName = englishMonths.getOrElse(monthIndex) { "May" }
    
    return "$day $monthName, $year"
}

@Composable
fun AnalysisScreen(viewModel: MainViewModel) {
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("দৈনিক") }
    var historyTab by remember { mutableStateOf("সাধারণ") }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val filteredSessions = remember(recentSessions, selectedTab) {
        val calNow = java.util.Calendar.getInstance()
        when (selectedTab) {
            "দৈনিক" -> {
                recentSessions.filter { session ->
                    val calSession = java.util.Calendar.getInstance().apply { timeInMillis = session.dateMillis }
                    calNow.get(java.util.Calendar.YEAR) == calSession.get(java.util.Calendar.YEAR) &&
                    calNow.get(java.util.Calendar.DAY_OF_YEAR) == calSession.get(java.util.Calendar.DAY_OF_YEAR)
                }
            }
            "সাপ্তাহিক" -> {
                val calStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    var iterations = 0
                    while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY && iterations < 7) {
                        add(java.util.Calendar.DAY_OF_YEAR, -1)
                        iterations++
                    }
                }
                val calEnd = calStart.clone() as java.util.Calendar
                calEnd.add(java.util.Calendar.DAY_OF_YEAR, 6)
                calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                calEnd.set(java.util.Calendar.MINUTE, 59)
                calEnd.set(java.util.Calendar.SECOND, 59)
                calEnd.set(java.util.Calendar.MILLISECOND, 999)
                recentSessions.filter { session ->
                    session.dateMillis in calStart.timeInMillis..calEnd.timeInMillis
                }
            }
            "মাসিক" -> {
                recentSessions.filter { session ->
                    val calSession = java.util.Calendar.getInstance().apply { timeInMillis = session.dateMillis }
                    calNow.get(java.util.Calendar.YEAR) == calSession.get(java.util.Calendar.YEAR) &&
                    calNow.get(java.util.Calendar.MONTH) == calSession.get(java.util.Calendar.MONTH)
                }
            }
            else -> recentSessions
        }
    }

    val groupedBySubject = remember(filteredSessions) { filteredSessions.groupBy { it.subject } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        item(key = "header") {
            TopAppBarSection()
        }

        if (isTablet) {
            item(key = "tablet_dashboard") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Charts and Performance
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("স্টাডি ট্র্যাকিং লগ", color = LightText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(4.dp)
                            ) {
                                TabButton("দৈনিক", selectedTab == "দৈনিক") { selectedTab = "দৈনিক" }
                                TabButton("সাপ্তাহিক", selectedTab == "সাপ্তাহিক") { selectedTab = "সাপ্তাহিক" }
                                TabButton("মাসিক", selectedTab == "মাসিক") { selectedTab = "মাসিক" }
                            }
                        }

                        BarChartCard(selectedTab, recentSessions, filteredSessions)
                        ExamScoreProgressChartCard(viewModel)
                    }

                    // Right Column: Session History
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("স্টাডি সেশন হিস্ট্রি", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(4.dp)
                            ) {
                                TabButton("সাধারণ", historyTab == "সাধারণ") { historyTab = "সাধারণ" }
                                TabButton("বিষয়ভিত্তিক", historyTab == "বিষয়ভিত্তিক") { historyTab = "বিষয়ভিত্তিক" }
                            }
                        }

                        if (filteredSessions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = "Empty",
                                        tint = GrayText,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "কোনো স্টাডি সেশন রেকর্ড পাওয়া যায়নি",
                                        color = GrayText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            if (historyTab == "সাধারণ") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredSessions.forEach { session ->
                                        SessionHistoryCard(session)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    groupedBySubject.forEach { (subject, sessionsInSubject) ->
                                        SubjectHistoryGroup(subject, sessionsInSubject)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "tracking_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("স্টাডি ট্র্যাকিং লগ", color = LightText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(4.dp)
                    ) {
                        TabButton("দৈনিক", selectedTab == "দৈনিক") { selectedTab = "দৈনিক" }
                        TabButton("সাপ্তাহিক", selectedTab == "সাপ্তাহিক") { selectedTab = "সাপ্তাহিক" }
                        TabButton("মাসিক", selectedTab == "মাসিক") { selectedTab = "মাসিক" }
                    }
                }
            }
            item(key = "bar_chart") {
                BarChartCard(selectedTab, recentSessions, filteredSessions)
            }
            item(key = "exam_chart") {
                ExamScoreProgressChartCard(viewModel)
            }
            item(key = "history_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("স্টাডি সেশন হিস্ট্রি", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(4.dp)
                    ) {
                        TabButton("সাধারণ", historyTab == "সাধারণ") { historyTab = "সাধারণ" }
                        TabButton("বিষয়ভিত্তিক", historyTab == "বিষয়ভিত্তিক") { historyTab = "বিষয়ভিত্তিক" }
                    }
                }
            }

            if (filteredSessions.isEmpty()) {
                item(key = "empty_history") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = "Empty",
                                tint = GrayText,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "কোনো স্টাডি সেশন রেকর্ড পাওয়া যায়নি",
                                color = GrayText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                if (historyTab == "সাধারণ") {
                    items(filteredSessions, key = { it.id }) { session ->
                        SessionHistoryCard(session)
                    }
                } else {
                    groupedBySubject.forEach { (subject, sessionsInSubject) ->
                        item(key = "subj_$subject") {
                            SubjectHistoryGroup(subject, sessionsInSubject)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectHistoryGroup(subject: String, sessionsInSubject: List<StudySession>) {
    val totalSubjectMin = remember(sessionsInSubject) { sessionsInSubject.sumOf { it.durationMinutes } }
    val totalSubjectHrs = totalSubjectMin.toFloat() / 60f
    val formattedSubjectHrs = if (totalSubjectHrs % 1f == 0f) totalSubjectHrs.toInt().toString() else String.format(java.util.Locale.US, "%.1f", totalSubjectHrs)
    
    val toBngDigits = { numStr: String ->
        numStr.map { c ->
            if (c.isDigit()) (c - '0' + '০'.code).toChar() else c
        }.joinToString("")
    }
    val bngSubjectHrs = toBngDigits(formattedSubjectHrs)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.2f))
            .border(1.dp, Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getSubjectEnglish(subject),
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "মোট $bngSubjectHrs ঘণ্টা",
                    color = Color(0xFF2CD4A0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Divider(color = Color(0xFF1E293B).copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sessionsInSubject.forEach { session ->
                    SessionHistoryCard(session)
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0F172A) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (isSelected) Color(0xFF60A5FA) else GrayText, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun BarChartCard(selectedTab: String, sessions: List<StudySession>, filteredSessions: List<StudySession>) {
    val data = remember(sessions, selectedTab) {
        val cal = java.util.Calendar.getInstance()
        when (selectedTab) {
            "দৈনিক" -> {
                val result = mutableListOf<Triple<String, Float, Float>>()
                for (i in 6 downTo 0) {
                    val dayCal = java.util.Calendar.getInstance()
                    dayCal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                    
                    val prevDayCal = java.util.Calendar.getInstance()
                    prevDayCal.add(java.util.Calendar.DAY_OF_YEAR, -(i + 7))
                    
                    val dayOfWeek = dayCal.get(java.util.Calendar.DAY_OF_WEEK)
                    val label = if (i == 0) "আজ" else when (dayOfWeek) {
                        java.util.Calendar.SATURDAY -> "শনি"
                        java.util.Calendar.SUNDAY -> "রবি"
                        java.util.Calendar.MONDAY -> "সোম"
                        java.util.Calendar.TUESDAY -> "মঙ্গল"
                        java.util.Calendar.WEDNESDAY -> "বুধ"
                        java.util.Calendar.THURSDAY -> "বৃহঃ"
                        java.util.Calendar.FRIDAY -> "শুক্র"
                        else -> "আজ"
                    }
                    
                    val daySessions = sessions.filter { s ->
                        val sCal = java.util.Calendar.getInstance().apply { timeInMillis = s.dateMillis }
                        sCal.get(java.util.Calendar.YEAR) == dayCal.get(java.util.Calendar.YEAR) &&
                        sCal.get(java.util.Calendar.DAY_OF_YEAR) == dayCal.get(java.util.Calendar.DAY_OF_YEAR)
                    }

                    val prevDaySessions = sessions.filter { s ->
                        val sCal = java.util.Calendar.getInstance().apply { timeInMillis = s.dateMillis }
                        sCal.get(java.util.Calendar.YEAR) == prevDayCal.get(java.util.Calendar.YEAR) &&
                        sCal.get(java.util.Calendar.DAY_OF_YEAR) == prevDayCal.get(java.util.Calendar.DAY_OF_YEAR)
                    }

                    val totalHours = daySessions.sumOf { it.durationMinutes }.toFloat() / 60f
                    val prevTotalHours = prevDaySessions.sumOf { it.durationMinutes }.toFloat() / 60f
                    val roundedHours = Math.round(totalHours * 10f) / 10f
                    val roundedPrevHours = Math.round(prevTotalHours * 10f) / 10f
                    result.add(Triple(label, roundedHours, roundedPrevHours))
                }
                result
            }
            "সাপ্তাহিক" -> {
                val result = mutableListOf<Triple<String, Float, Float>>()
                for (i in 3 downTo 0) {
                    val calStart = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                        var iterations = 0
                        while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY && iterations < 7) {
                            add(java.util.Calendar.DAY_OF_YEAR, -1)
                            iterations++
                        }
                        add(java.util.Calendar.DAY_OF_YEAR, -(i * 7))
                    }
                    val calEnd = calStart.clone() as java.util.Calendar
                    calEnd.add(java.util.Calendar.DAY_OF_YEAR, 6)
                    calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calEnd.set(java.util.Calendar.MINUTE, 59)
                    calEnd.set(java.util.Calendar.SECOND, 59)
                    calEnd.set(java.util.Calendar.MILLISECOND, 999)

                    val prevCalStart = calStart.clone() as java.util.Calendar
                    prevCalStart.add(java.util.Calendar.DAY_OF_YEAR, -7)
                    
                    val prevCalEnd = calEnd.clone() as java.util.Calendar
                    prevCalEnd.add(java.util.Calendar.DAY_OF_YEAR, -7)
                    
                    val weekSessions = sessions.filter { s ->
                        s.dateMillis in calStart.timeInMillis..calEnd.timeInMillis
                    }
                    val prevWeekSessions = sessions.filter { s ->
                        s.dateMillis in prevCalStart.timeInMillis..prevCalEnd.timeInMillis
                    }

                    val totalHours = weekSessions.sumOf { it.durationMinutes }.toFloat() / 60f
                    val prevTotalHours = prevWeekSessions.sumOf { it.durationMinutes }.toFloat() / 60f

                    val roundedHours = Math.round(totalHours * 10f) / 10f
                    val roundedPrevHours = Math.round(prevTotalHours * 10f) / 10f
                    result.add(Triple("সপ্তাহ ${4 - i}", roundedHours, roundedPrevHours))
                }
                result
            }
            "মাসিক" -> {
                val result = mutableListOf<Triple<String, Float, Float>>()
                val monthNames = listOf("জানু", "ফেব", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
                for (i in 3 downTo 0) {
                    val mCal = java.util.Calendar.getInstance()
                    mCal.add(java.util.Calendar.MONTH, -i)
                    
                    val prevMCal = java.util.Calendar.getInstance()
                    prevMCal.add(java.util.Calendar.MONTH, -(i + 1))

                    val monthIndex = mCal.get(java.util.Calendar.MONTH)
                    val label = monthNames.getOrElse(monthIndex) { "মে" }
                    
                    val monthSessions = sessions.filter { s ->
                        val sCal = java.util.Calendar.getInstance().apply { timeInMillis = s.dateMillis }
                        sCal.get(java.util.Calendar.YEAR) == mCal.get(java.util.Calendar.YEAR) &&
                        sCal.get(java.util.Calendar.MONTH) == mCal.get(java.util.Calendar.MONTH)
                    }
                    
                    val prevMonthSessions = sessions.filter { s ->
                        val sCal = java.util.Calendar.getInstance().apply { timeInMillis = s.dateMillis }
                        sCal.get(java.util.Calendar.YEAR) == prevMCal.get(java.util.Calendar.YEAR) &&
                        sCal.get(java.util.Calendar.MONTH) == prevMCal.get(java.util.Calendar.MONTH)
                    }

                    val totalHours = monthSessions.sumOf { it.durationMinutes }.toFloat() / 60f
                    val prevTotalHours = prevMonthSessions.sumOf { it.durationMinutes }.toFloat() / 60f

                    val roundedHours = Math.round(totalHours * 10f) / 10f
                    val roundedPrevHours = Math.round(prevTotalHours * 10f) / 10f
                    result.add(Triple(label, roundedHours, roundedPrevHours))
                }
                result
            }
            else -> emptyList()
        }
    }

    val totalHoursSum = remember(filteredSessions) {
        val rawSum = filteredSessions.sumOf { it.durationMinutes }.toDouble() / 60.0
        (Math.round(rawSum * 10.0) / 10.0).toFloat()
    }

    // Calculate previous period sum
    val previousPeriodSum = remember(sessions, selectedTab) {
        val calNow = java.util.Calendar.getInstance()
        when (selectedTab) {
            "দৈনিক" -> {
                val prevDay = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }
                val prevSessions = sessions.filter { session ->
                    val calSession = java.util.Calendar.getInstance().apply { timeInMillis = session.dateMillis }
                    prevDay.get(java.util.Calendar.YEAR) == calSession.get(java.util.Calendar.YEAR) &&
                    prevDay.get(java.util.Calendar.DAY_OF_YEAR) == calSession.get(java.util.Calendar.DAY_OF_YEAR)
                }
                (Math.round((prevSessions.sumOf { it.durationMinutes }.toDouble() / 60.0) * 10.0) / 10.0).toFloat()
            }
            "সাপ্তাহিক" -> {
                val calLimitStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    var iterations = 0
                    while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY && iterations < 7) {
                        add(java.util.Calendar.DAY_OF_YEAR, -1)
                        iterations++
                    }
                    add(java.util.Calendar.DAY_OF_YEAR, -7)
                }
                val calLimitEnd = calLimitStart.clone() as java.util.Calendar
                calLimitEnd.add(java.util.Calendar.DAY_OF_YEAR, 6)
                calLimitEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                calLimitEnd.set(java.util.Calendar.MINUTE, 59)
                calLimitEnd.set(java.util.Calendar.SECOND, 59)
                calLimitEnd.set(java.util.Calendar.MILLISECOND, 999)
                val prevSessions = sessions.filter { session ->
                    session.dateMillis in calLimitStart.timeInMillis..calLimitEnd.timeInMillis
                }
                (Math.round((prevSessions.sumOf { it.durationMinutes }.toDouble() / 60.0) * 10.0) / 10.0).toFloat()
            }
            "মাসিক" -> {
                val prevMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
                val prevSessions = sessions.filter { session ->
                    val calSession = java.util.Calendar.getInstance().apply { timeInMillis = session.dateMillis }
                    prevMonth.get(java.util.Calendar.YEAR) == calSession.get(java.util.Calendar.YEAR) &&
                    prevMonth.get(java.util.Calendar.MONTH) == calSession.get(java.util.Calendar.MONTH)
                }
                (Math.round((prevSessions.sumOf { it.durationMinutes }.toDouble() / 60.0) * 10.0) / 10.0).toFloat()
            }
            else -> 0f
        }
    }

    val percentageChange = remember(totalHoursSum, previousPeriodSum) {
        if (previousPeriodSum == 0f && totalHoursSum > 0f) {
            100f // 100% improvement if previous was 0 and current is positive
        } else if (previousPeriodSum == 0f && totalHoursSum == 0f) {
            0f
        } else {
            ((totalHoursSum - previousPeriodSum) / previousPeriodSum) * 100f
        }
    }

    val toBanglaDigits = { num: Float ->
        if (num % 1f == 0f) num.toInt().toString() else String.format(java.util.Locale.US, "%.1f", num)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(if (selectedTab == "দৈনিক") "দৈনিক অর্জন" else if (selectedTab == "সাপ্তাহিক") "সাপ্তাহিক অর্জন" else "মাসিক অর্জন", color = GrayText, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (percentageChange != 0f) {
                        val isPositive = percentageChange > 0
                        val color = if (isPositive) Color(0xFF2CD4A0) else Color(0xFFF87171)
                        val sign = if (isPositive) "+" else ""
                        val textStr = "$sign${toBanglaDigits(percentageChange)}% ${if (isPositive) "বেশি" else "কম"}"
                        Text(textStr, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("পরিবর্তন নেই", color = GrayText, fontSize = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("মোট ${toBanglaDigits(totalHoursSum)} ঘণ্টা", color = Color(0xFF2CD4A0), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("আগে ${toBanglaDigits(previousPeriodSum)} ঘণ্টা", color = GrayText, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxVal = remember(data) {
                    val maxCurrent = data.maxOfOrNull { it.second } ?: 1.0f
                    val maxPrev = data.maxOfOrNull { it.third } ?: 1.0f
                    val maxTotal = maxOf(maxCurrent, maxPrev)
                    if (maxTotal == 0.0f) 1.0f else maxTotal
                }
                val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF2CD4A0), Color(0xFF3B82F6))
                )
                
                data.forEach { (labelCircle, hours, prevHours) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(toBanglaDigits(hours), color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1E293B)) // Track color
                        ) {
                            // Previous Data Shadow (Behind)
                            if (prevHours > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(prevHours / maxVal)
                                        .align(Alignment.BottomCenter)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.15f))
                                )
                            }
                            // Current Data (Front)
                            if (hours > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(hours / maxVal)
                                        .align(Alignment.BottomCenter)
                                        .clip(RoundedCornerShape(50))
                                        .background(brush = brush)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(labelCircle, color = GrayText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(session: StudySession) {
    val subjectEng = getSubjectEnglish(session.subject)
    val isCore = subjectEng.startsWith("Botany") || 
                 subjectEng.startsWith("Zoology") || 
                 subjectEng.startsWith("Chemistry") || 
                 subjectEng.startsWith("Physics")
    
    val tagBg = Color(0xFF1E2B45)
    val tagColor = Color(0xFF60A5FA)
    
    val valueColor = Color(0xFF2CD4A0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.chapter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (isCore) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(tagBg).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("কোর সাবজেক্ট", color = tagColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("$subjectEng • ${formatBanglaDate(session.dateMillis)}", color = GrayText, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${toBanglaDigitsInt(session.durationMinutes)} মি.", color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(getSessionTypeEnglish(session.sessionType), color = GrayText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ExamScoreProgressChartCard(viewModel: MainViewModel) {
    val examResultsMap by viewModel.chapterExamResults.collectAsStateWithLifecycle()
    
    // Fallback/demo exam results if user hasn't added any yet
    val demoExamResults = remember {
        listOf(
            com.example.data.ChapterExamResult(id = -1, chapterKey = "Botany_কোষ ও এর গঠন", subjectName = "Botany (উদ্ভিদবিজ্ঞান)", chapterName = "কোষ ও এর গঠন", percentage = 55.0, timestamp = System.currentTimeMillis() - 7 * 86400000),
            com.example.data.ChapterExamResult(id = -2, chapterKey = "Zoology_প্রাণীর পরিচিতি", subjectName = "Zoology (প্রাণিবিজ্ঞান)", chapterName = "প্রাণীর পরিচিতি", percentage = 68.0, timestamp = System.currentTimeMillis() - 5 * 86400000),
            com.example.data.ChapterExamResult(id = -3, chapterKey = "Chemistry 1st Paper_গুণগত রসায়ন", subjectName = "Chemistry 1st Paper (রসায়ন ১ম পত্র)", chapterName = "গুণগত রসায়ন", percentage = 72.0, timestamp = System.currentTimeMillis() - 4 * 86400000),
            com.example.data.ChapterExamResult(id = -4, chapterKey = "Physics 1st Paper_ভেক্টর", subjectName = "Physics 1st Paper (পদার্থবিজ্ঞান ১ম পত্র)", chapterName = "ভেক্টর", percentage = 60.0, timestamp = System.currentTimeMillis() - 3 * 86400000),
            com.example.data.ChapterExamResult(id = -5, chapterKey = "Botany_কোষ বিভাজন", subjectName = "Botany (উদ্ভিদবিজ্ঞান)", chapterName = "কোষ বিভাজন", percentage = 85.0, timestamp = System.currentTimeMillis() - 2 * 86400000),
            com.example.data.ChapterExamResult(id = -6, chapterKey = "Chemistry 2nd Paper_জৈব রসায়ন", subjectName = "Chemistry 2nd Paper (রসায়ন ২য় পত্র)", chapterName = "জৈব রসায়ন", percentage = 92.0, timestamp = System.currentTimeMillis() - 1 * 86400000)
        )
    }

    val realExamResults = remember(examResultsMap) {
        examResultsMap.values.flatten().sortedBy { it.timestamp }
    }
    
    val isDemoMode = realExamResults.isEmpty()
    val displayList = if (isDemoMode) demoExamResults else realExamResults

    var selectedFilter by remember { mutableStateOf("সব বিষয়") }
    val subjectFilterOptions = listOf(
        "সব বিষয়",
        "উদ্ভিদবিজ্ঞান",
        "প্রাণিবিজ্ঞান",
        "রসায়ন ১ম",
        "রসায়ন ২য়",
        "পদার্থ ১ম",
        "পদার্থ ২য়",
        "ইংরেজি",
        "জিকে"
    )

    val filteredDisplayList = remember(displayList, selectedFilter) {
        if (selectedFilter == "সব বিষয়") {
            displayList
        } else {
            displayList.filter { result ->
                val sub = result.subjectName.lowercase()
                when (selectedFilter) {
                    "উদ্ভিদবিজ্ঞান" -> sub.contains("botany") || sub.contains("উদ্ভিদবিজ্ঞান")
                    "প্রাণিবিজ্ঞান" -> sub.contains("zoology") || sub.contains("প্রাণিবিজ্ঞান")
                    "রসায়ন ১ম" -> sub.contains("chemistry 1st") || sub.contains("রসায়ন ১ম") || sub.contains("রসায়ন ১ম")
                    "রসায়ন ২য়" -> sub.contains("chemistry 2nd") || sub.contains("রসায়ন ২য়") || sub.contains("রসায়ন ২য়")
                    "পদার্থ ১ম" -> sub.contains("physics 1st") || sub.contains("পদার্থবিজ্ঞান ১ম")
                    "পদার্থ ২য়" -> sub.contains("physics 2nd") || sub.contains("পদার্থবিজ্ঞান ২য়") || sub.contains("পদার্থবিজ্ঞান ২য়")
                    "ইংরেজি" -> sub.contains("english") || sub.contains("ইংরেজি")
                    "জিকে" -> sub.contains("general knowledge") || sub.contains("সাধারণ জ্ঞান") || sub.contains("gk")
                    else -> true
                }
            }
        }
    }

    var selectedPointIndex by remember(filteredDisplayList) {
        mutableStateOf(if (filteredDisplayList.isNotEmpty()) filteredDisplayList.lastIndex else -1)
    }
    
    val activeIndex = if (selectedPointIndex in filteredDisplayList.indices) selectedPointIndex else if (filteredDisplayList.isNotEmpty()) filteredDisplayList.lastIndex else -1

    var showAddForm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Trend Icon",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "টেস্ট স্কোর ট্রেন্ড বিশ্লেষণ",
                            color = LightText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = HindSiliguri
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "অধ্যায়ভিত্তিক প্রাকটিস টেস্টের প্রোগ্রেস ও পারফরম্যান্স বিশ্লেষণ",
                        color = GrayText,
                        fontSize = 11.sp,
                        fontFamily = HindSiliguri
                    )
                }

                if (isDemoMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ডেমো চার্ট",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontally Scrollable Subject Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectFilterOptions.forEach { filterItem ->
                    val isSelected = selectedFilter == filterItem
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFF1E293B).copy(alpha = 0.4f))
                            .border(width = 1.dp, color = if (isSelected) Color(0xFF3B82F6) else Color.Transparent, shape = RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = filterItem }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filterItem,
                            color = if (isSelected) Color(0xFF60A5FA) else GrayText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = HindSiliguri
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredDisplayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এই বিষয়ে কোনো প্রাকটিস টেস্টের রেকর্ড নেই",
                        color = GrayText,
                        fontSize = 12.sp,
                        fontFamily = HindSiliguri
                    )
                }
            } else {
                // Interactive Recharts-style progress line chart
                RechartsLineChart(
                    data = filteredDisplayList,
                    selectedPointIndex = activeIndex,
                    onPointSelected = { index -> selectedPointIndex = index },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail card of hovered/selected point
                if (activeIndex in filteredDisplayList.indices) {
                    val item = filteredDisplayList[activeIndex]
                    
                    val scoreColor = if (item.percentage >= 80.0) Color(0xFF10B981)
                    else if (item.percentage >= 60.0) Color(0xFF3B82F6)
                    else if (item.percentage >= 40.0) Color(0xFFF59E0B)
                    else Color(0xFFEF4444)
                    
                    val scoreBg = scoreColor.copy(alpha = 0.12f)
                    
                    val statusText = if (item.percentage >= 80.0) "অসাধারণ প্রিপারেশন! 🌟"
                    else if (item.percentage >= 60.0) (if (item.percentage >= 70.0) "ভালো পজিশন — আরও ভালো সম্ভব 👍" else "সন্তোষজনক প্রোগ্রেস 👍")
                    else if (item.percentage >= 40.0) "গড়পড়তা স্কোর — রিভিশন দিন 🎯"
                    else "উন্নতি প্রয়োজন — চ্যাপ্টারটি আবার পড়ুন ⚠️"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.chapterName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = HindSiliguri
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${getSubjectEnglish(item.subjectName)} • ${formatBanglaDate(item.timestamp)}",
                                        color = GrayText,
                                        fontSize = 11.sp,
                                        fontFamily = HindSiliguri
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scoreBg)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    val pctStr = if (item.percentage % 1.0 == 0.0) "${item.percentage.toInt()}" else String.format("%.1f", item.percentage)
                                    Text(
                                        text = "$pctStr%",
                                        color = scoreColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        fontFamily = HindSiliguri
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = BorderColor.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(scoreColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    color = scoreColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    fontFamily = HindSiliguri
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inline entry button & collapsible form
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.3f))
                    .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddForm = !showAddForm }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "নতুন প্রাকটিস টেস্ট স্কোর যুক্ত করুন",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguri
                        )
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Expand Accordion",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showAddForm) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Divider(color = BorderColor.copy(alpha = 0.2f))
                            
                            // 1. Choose Subject
                            var addSelectedSubject by remember { mutableStateOf(viewModel.syllabusSubjects.first()) }
                            var addSelectedChapter by remember { mutableStateOf(viewModel.syllabusSubjects.first().chapters.first()) }
                            var addScoreInputValue by remember { mutableStateOf("") }
                            var addFormError by remember { mutableStateOf("") }

                            Text(
                                text = "বিষয় নির্বাচন করুন:",
                                color = LightText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguri
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                viewModel.syllabusSubjects.forEach { subj ->
                                    val isSelectedObj = addSelectedSubject.subject == subj.subject
                                    val cleanSubjText = subj.subject.substringBefore(" (")
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelectedObj) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF0F172A))
                                            .border(1.dp, if (isSelectedObj) Color(0xFF3B82F6) else BorderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable { 
                                                addSelectedSubject = subj
                                                addSelectedChapter = subj.chapters.first()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = cleanSubjText,
                                            color = if (isSelectedObj) Color(0xFF60A5FA) else GrayText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelectedObj) FontWeight.Bold else FontWeight.Medium,
                                            fontFamily = HindSiliguri
                                        )
                                    }
                                }
                            }

                            // 2. Choose Chapter
                            Text(
                                text = "অধ্যায় নির্বাচন করুন:",
                                color = LightText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguri
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                addSelectedSubject.chapters.forEach { chap ->
                                    val isSelectedChap = addSelectedChapter.name == chap.name
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelectedChap) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0F172A))
                                            .border(1.dp, if (isSelectedChap) Color(0xFF10B981) else BorderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable { addSelectedChapter = chap }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = chap.name,
                                            color = if (isSelectedChap) Color(0xFF2CD4A0) else GrayText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelectedChap) FontWeight.Bold else FontWeight.Medium,
                                            fontFamily = HindSiliguri
                                        )
                                    }
                                }
                            }

                            // 3. Input score percentage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "%",
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (addScoreInputValue.isEmpty()) {
                                                Text(
                                                    text = "প্রাপ্ত মার্ক বলুন (যেমন: ৮০)",
                                                    color = GrayText.copy(alpha = 0.6f),
                                                    fontSize = 11.sp,
                                                    fontFamily = HindSiliguri
                                                )
                                            }
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = addScoreInputValue,
                                                onValueChange = { valStr ->
                                                    if (valStr.isEmpty() || valStr.toDoubleOrNull() != null) {
                                                        addScoreInputValue = valStr
                                                        addFormError = ""
                                                    }
                                                },
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontFamily = HindSiliguri,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                ),
                                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3B82F6)),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        val score = addScoreInputValue.toDoubleOrNull()
                                        if (score != null && score in 0.0..100.0) {
                                            viewModel.addChapterExamResult(
                                                subjectName = addSelectedSubject.subject,
                                                chapterName = addSelectedChapter.name,
                                                percentage = score
                                            )
                                            addScoreInputValue = ""
                                            addFormError = ""
                                            showAddForm = false
                                            selectedFilter = "সব বিষয়"
                                        } else {
                                            addFormError = "০ থেকে ১০০ এর মধ্যে একটি নম্বর দিন"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3B82F6)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = "যোগ করুন",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = HindSiliguri
                                    )
                                }
                            }

                            if (addFormError.isNotEmpty()) {
                                Text(
                                    text = addFormError,
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = HindSiliguri,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RechartsLineChart(
    data: List<com.example.data.ChapterExamResult>,
    selectedPointIndex: Int,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val gridColor = Color(0xFF1E293B)
    val lineColor = Color(0xFF3B82F6) // Recharts-style beautiful vibrant blue
    val areaStartColor = Color(0xFF3B82F6).copy(alpha = 0.22f)
    val areaEndColor = Color.Transparent

    // Pre-allocated Paint objects to prevent GC churn on low-spec tablets
    val yAxisPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 24f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
    }
    val activeNumberPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
    }
    val inactiveNumberPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 20f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    val handleTouch: (Offset) -> Unit = { touchOffset ->
                        val width = size.width.toFloat()
                        val paddingEnd = 24f
                        val paddingStart = 64f
                        val chartWidth = width - paddingStart - paddingEnd
                        val pointsCount = data.size
                        if (pointsCount > 0 && chartWidth > 0) {
                            val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth
                            var closestIndex = 0
                            var minDistance = Float.MAX_VALUE
                            for (i in 0 until pointsCount) {
                                val cx = paddingStart + i * stepX
                                val distance = kotlin.math.abs(touchOffset.x - cx)
                                if (distance < minDistance) {
                                    minDistance = distance
                                    closestIndex = i
                                }
                            }
                            if (minDistance < stepX / 2f || minDistance < 50f) {
                                onPointSelected(closestIndex)
                            }
                        }
                    }

                    detectTapGestures(onTap = handleTouch)
                }
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            val paddingEnd = 24f
                            val paddingStart = 64f
                            val chartWidth = width - paddingStart - paddingEnd
                            val pointsCount = data.size
                            if (pointsCount > 0 && chartWidth > 0) {
                                val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth
                                var closestIndex = 0
                                var minDistance = Float.MAX_VALUE
                                for (i in 0 until pointsCount) {
                                    val cx = paddingStart + i * stepX
                                    val distance = kotlin.math.abs(offset.x - cx)
                                    if (distance < minDistance) {
                                        minDistance = distance
                                        closestIndex = i
                                    }
                                }
                                if (minDistance < stepX / 2f || minDistance < 50f) {
                                    onPointSelected(closestIndex)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            val width = size.width.toFloat()
                            val paddingEnd = 24f
                            val paddingStart = 64f
                            val chartWidth = width - paddingStart - paddingEnd
                            val pointsCount = data.size
                            if (pointsCount > 0 && chartWidth > 0) {
                                val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth
                                var closestIndex = 0
                                var minDistance = Float.MAX_VALUE
                                for (i in 0 until pointsCount) {
                                    val cx = paddingStart + i * stepX
                                    val distance = kotlin.math.abs(change.position.x - cx)
                                    if (distance < minDistance) {
                                        minDistance = distance
                                        closestIndex = i
                                    }
                                }
                                if (minDistance < stepX / 2f || minDistance < 50f) {
                                    onPointSelected(closestIndex)
                                }
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            
            val paddingTop = 16f
            val paddingBottom = 24f
            val paddingStart = 64f
            val paddingEnd = 24f
            
            val chartWidth = width - paddingStart - paddingEnd
            val chartHeight = height - paddingTop - paddingBottom
            
            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas
            
            // 1. Draw grid horizontal lines (0%, 25%, 50%, 75%, 100%)
            val gridSteps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            gridSteps.forEach { step ->
                val gy = paddingTop + chartHeight * (1f - step)
                
                // Grid horizontal line
                drawLine(
                    color = gridColor,
                    start = Offset(paddingStart, gy),
                    end = Offset(width - paddingEnd, gy),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                
                // Native canvas to draw percentage labels
                drawContext.canvas.nativeCanvas.apply {
                    val labelBng = when (step) {
                        0f -> "০%"
                        0.25f -> "২৫%"
                        0.5f -> "৫০%"
                        0.75f -> "৭৫%"
                        1f -> "১০০%"
                        else -> "${(step * 100).toInt()}%"
                    }
                    drawText(
                        labelBng,
                        12f,
                        gy + 8f,
                        yAxisPaint
                    )
                }
            }
            
            // X Axis Line
            drawLine(
                color = BorderColor,
                start = Offset(paddingStart, paddingTop + chartHeight),
                end = Offset(width - paddingEnd, paddingTop + chartHeight),
                strokeWidth = 2f
            )
            
            // Points calculations
            val pointsCount = data.size
            val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth
            val coordinates = data.mapIndexed { index, item ->
                val cx = paddingStart + index * stepX
                val pct = item.percentage.toFloat().coerceIn(0f, 100f)
                val cy = paddingTop + chartHeight * (1f - (pct / 100f))
                Offset(cx, cy)
            }
            
            // 2. Draw Area Gradient under Line
            if (pointsCount > 1) {
                val areaPath = Path().apply {
                    moveTo(coordinates[0].x, paddingTop + chartHeight)
                    lineTo(coordinates[0].x, coordinates[0].y)
                    for (i in 1 until pointsCount) {
                        lineTo(coordinates[i].x, coordinates[i].y)
                    }
                    lineTo(coordinates[pointsCount - 1].x, paddingTop + chartHeight)
                    close()
                }
                
                drawPath(
                    path = areaPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(areaStartColor, areaEndColor),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )
            }
            
            // 3. Draw Trend Path Stroke Line
            val strokePath = Path().apply {
                if (pointsCount > 0) {
                    moveTo(coordinates[0].x, coordinates[0].y)
                    for (i in 1 until pointsCount) {
                        lineTo(coordinates[i].x, coordinates[i].y)
                    }
                }
            }
            
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // 4. Draw Glow Dots for points
            coordinates.forEachIndexed { index, offset ->
                val ratingPct = data[index].percentage
                
                val pointColor = if (ratingPct >= 80) Color(0xFF10B981)
                else if (ratingPct >= 60) Color(0xFF3B82F6)
                else if (ratingPct >= 40) Color(0xFFF59E0B)
                else Color(0xFFEF4444)
                
                val radius = if (index == selectedPointIndex) 10f else 6f
                val outerHalo = if (index == selectedPointIndex) 20f else 0f
                
                if (outerHalo > 0f) {
                    drawCircle(
                        color = pointColor.copy(alpha = 0.25f),
                        radius = outerHalo,
                        center = offset
                    )
                }
                
                // Point Solid Dot
                drawCircle(
                    color = pointColor,
                    radius = radius,
                    center = offset
                )
                
                // Core center point
                drawCircle(
                    color = Color.White,
                    radius = if (index == selectedPointIndex) 3.5f else 1.5f,
                    center = offset
                )
                
                // Draw sequence indicator number at the bottom for navigation reference
                drawContext.canvas.nativeCanvas.apply {
                    val paint = if (index == selectedPointIndex) activeNumberPaint else inactiveNumberPaint
                    val indicatorStr = when (index + 1) {
                        1 -> "১"
                        2 -> "২"
                        3 -> "৩"
                        4 -> "৪"
                        5 -> "৫"
                        6 -> "৬"
                        7 -> "৭"
                        8 -> "৮"
                        9 -> "৯"
                        else -> "${index + 1}"
                    }
                    drawText(
                        indicatorStr,
                        offset.x - 6f,
                        paddingTop + chartHeight + 20f,
                        paint
                    )
                }
            }
        }
    }
}
