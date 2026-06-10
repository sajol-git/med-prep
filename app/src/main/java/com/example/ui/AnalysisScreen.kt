package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import com.example.data.StudySession
import com.example.ui.theme.*

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TopAppBarSection()
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
        item {
            BarChartCard(selectedTab, recentSessions, filteredSessions)
        }
        item {
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
            item {
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
                items(filteredSessions) { session ->
                    SessionHistoryCard(session)
                }
            } else {
                val groupedBySubject = filteredSessions.groupBy { it.subject }
                groupedBySubject.forEach { (subject, sessionsInSubject) ->
                    item {
                        val totalSubjectMin = sessionsInSubject.sumOf { it.durationMinutes }
                        val totalSubjectHrs = totalSubjectMin.toFloat() / 60f
                        val formattedSubjectHrs = if (totalSubjectHrs % 1f == 0f) totalSubjectHrs.toInt().toString() else String.format("%.1f", totalSubjectHrs)
                        
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
