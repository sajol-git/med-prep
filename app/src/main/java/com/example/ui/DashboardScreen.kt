package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.example.ui.theme.*
import kotlin.math.roundToInt

private fun toBanglaDigits(num: Any): String {
    return num.toString()
}

private fun toBanglaDigitsFloat(num: Float): String {
    return if (num % 1f == 0f) num.toInt().toString() else String.format(java.util.Locale.US, "%.1f", num)
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val coreProgress by viewModel.coreProgress.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TopAppBarSection()
        }
        item {
            DailyProgressCard(viewModel.todayStudyHours, viewModel.targetHours)
        }
        item {
            WeeklyStatsCard(viewModel.weeklyStudyHours, viewModel.averageDailyStudyHours)
        }
        item {
            MedicalCountdownCard()
        }
        item {
            SyllabusProgressCard(coreProgress)
        }
    }
}

@Composable
fun MedicalCountdownCard() {
    val startDate = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.JUNE, 1, 0, 0, 0) }
    val endDate = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.OCTOBER, 31, 23, 59, 59) }
    val now = java.util.Calendar.getInstance()
    
    val totalDays = ((endDate.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
    
    val daysPassed = if (now.before(startDate)) 0L else if (now.after(endDate)) totalDays else ((now.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24))
    val daysRemaining = totalDays - daysPassed

    val progressRaw = if (totalDays > 0) (daysPassed.toFloat() / totalDays.toFloat()) else 0f
    val progress = if (progressRaw.isNaN()) 0f else progressRaw.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A).copy(alpha = 0.3f), Color(0xFF172554).copy(alpha = 0.3f))))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Countdown", tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("মেডিকেল প্রিপারেশন কাউন্টডাউন", color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("1 June - 31 October, 2026", color = GrayText, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF3B82F6),
                trackColor = Color(0xFF2A303C),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("অতিবাহিত দিন", color = GrayText, fontSize = 11.sp)
                    Text("${toBanglaDigits(daysPassed)} দিন", color = LightText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("অবশিষ্ট দিন", color = GrayText, fontSize = 11.sp)
                    Text("${toBanglaDigits(daysRemaining)} দিন", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TopAppBarSection() {
    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Dhaka") } }
    val dateFormat = remember { java.text.SimpleDateFormat("d MMMM, yyyy", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Dhaka") } }

    var currentTimeString by remember { mutableStateOf(timeFormat.format(java.util.Calendar.getInstance().time)) }
    var currentDateString by remember { mutableStateOf(dateFormat.format(java.util.Calendar.getInstance().time)) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Calendar.getInstance().time
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAFAFB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = com.example.R.drawable.ic_custom_logo),
                    contentDescription = "MED-PREP Brand Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("MED-PREP", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2CD4A0)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("টার্গেট: ঢাকা মেডিকেল", color = Color(0xFF2CD4A0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(currentTimeString, color = LightText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(currentDateString, color = GrayText, fontSize = 12.sp)
        }
    }
}

@Composable
fun DailyProgressCard(hours: Float, target: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF112240), Color(0xFF0F172A))))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2CD4A0).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Daily Study Time", color = Color(0xFF2CD4A0), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("আজকের লড়াকু দিন!", color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("৮ ঘণ্টার টার্গেটে এগিয়ে যান। আপনার\nমনোযোগই এনে দেবে সাফল্য!", color = GrayText, fontSize = 13.sp, lineHeight = 18.sp)
            }
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 12.dp).size(96.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF2A303C),
                    strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = { if (target > 0f) { val p = hours / target; if (p.isNaN()) 0f else p.coerceIn(0f, 1f) } else 0f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF2CD4A0),
                    strokeCap = StrokeCap.Round,
                    strokeWidth = 8.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(toBanglaDigitsFloat(hours), color = LightText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("ঘণ্টা", color = GrayText, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SyllabusProgressCard(progressList: List<SyllabusProgress>) {
    val totalCompleted = progressList.sumOf { it.completed }
    val totalChapters = progressList.sumOf { it.total }
            val percentageRaw = if (totalChapters > 0) (totalCompleted.toFloat() / totalChapters.toFloat() * 100f) else 0f
            val percentage = if (percentageRaw.isNaN()) 0f else percentageRaw.coerceIn(0f, 100f)
            val rawPercentageStr = String.format(java.util.Locale.US, "%.1f", percentage)
            val percentageStr = "${toBanglaDigits(rawPercentageStr)}%"
            val remaining = maxOf(0, totalChapters - totalCompleted)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0F766E).copy(alpha = 0.2f), Color(0xFF042F2E).copy(alpha = 0.3f))))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = "School", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("কোর সিলেবাস অগ্রগতি", color = LightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E2B45)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(percentageStr, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("শুধু জীব, রসায়ন ও পদার্থ হিসাবের আওতায় (জিকে ও ইংরেজি মুক্ত)", color = GrayText, fontSize = 11.sp)
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Custom Gradient Progress Bar
            val brush = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2CD4A0)))
            val progRaw = if (totalChapters > 0) (totalCompleted.toFloat() / totalChapters.toFloat()) else 0f
            val progressFraction = if (progRaw.isNaN()) 0f else progRaw.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A303C))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(brush)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("কমপ্লিট: ${toBanglaDigits(totalCompleted)}টি অধ্যায়", color = GrayText, fontSize = 11.sp)
                Text("অবশিষ্ট: ${toBanglaDigits(remaining)}টি অধ্যায়", color = GrayText, fontSize = 11.sp)
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val colors = listOf(Color(0xFF2CD4A0), Color(0xFF60A5FA), Color(0xFF60A5FA))
                progressList.forEachIndexed { index, p ->
                    val color = colors[index % colors.size]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.subjectBng, color = GrayText, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            // Format according to image: "০৫ / ২৪"
                            val completedFormatted = toBanglaDigits(p.completed.toString().padStart(2, '0'))
                            val totalFormatted = toBanglaDigits(p.total.toString().padStart(2, '0'))
                            
                            Text("$completedFormatted / $totalFormatted", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionCard() {
     Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF182A2E), CardBackground)))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
         Row(verticalAlignment = Alignment.Top) {
             Icon(Icons.Default.Lightbulb, contentDescription = "Tip", tint = Color(0xFF2CD4A0), modifier = Modifier.size(24.dp))
             Spacer(modifier = Modifier.width(16.dp))
             Column {
                 Text("আজকের মাস্টার সাজেশন", color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                 Spacer(modifier = Modifier.height(8.dp))
                 Text("বোটানির 'কোষ ও এর গঠন' এবং কেমিস্ট্রির 'জৈব রসায়ন' থেকে বিগত ৫ বছরে ৭টির বেশি কোশ্চেন এসেছে। এগুলো আজকে ভালো করে রিভিশন দিন!", color = GrayText, fontSize = 14.sp, lineHeight = 22.sp)
             }
         }
     }
}

@Composable
fun WeeklyStatsCard(weeklyHours: Float, averageHours: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E293B).copy(alpha = 0.4f), Color(0xFF0F172A).copy(alpha = 0.4f))))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2CD4A0).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "সাপ্তাহিক রুটিন",
                        tint = Color(0xFF2CD4A0),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "চলতি সপ্তাহের অর্জন (শনিবার থেকে শুরু)",
                        color = LightText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "সাপ্তাহিক গড় ও মোট পড়াশোনার বিশ্লেষণ",
                        color = GrayText,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column: Total Weekly Hours
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("মোট সাপ্তাহিক সময়", color = GrayText, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = toBanglaDigitsFloat(weeklyHours),
                                color = Color(0xFF2CD4A0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ঘণ্টা", color = GrayText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                }

                // Right Column: Daily Average Hours
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("দৈনিক গড় সময়", color = GrayText, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = toBanglaDigitsFloat(averageHours),
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ঘণ্টা / দিন", color = GrayText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                }
            }
        }
    }
}
