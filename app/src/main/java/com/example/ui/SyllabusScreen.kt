package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.components.TopAppBarSection
import com.example.ui.theme.*
import com.example.data.SyllabusSubject
import com.example.data.SyllabusChapter

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
        else -> subj
    }
}

@Composable
fun SyllabusScreen(viewModel: MainViewModel, onStartChapter: (String, String) -> Unit = {_,_ ->}) {
    val completedChapters by viewModel.completedSyllabusChapters.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val examResultsMap by viewModel.chapterExamResults.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Pre-group sessions to make lookups O(1) instead of nested O(N*M) list filters
    val chapterSessionMap = remember(recentSessions) {
        recentSessions.groupBy {
            getSubjectEnglish(it.subject).lowercase() to it.chapter.trim().lowercase()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        item(key = "header") {
            TopAppBarSection()
        }

        if (isTablet) {
            val subjects = viewModel.syllabusSubjects
            val half = (subjects.size + 1) / 2
            val col1 = subjects.take(half)
            val col2 = subjects.drop(half)

            item(key = "tablet_subjects") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        col1.forEach { subject ->
                            ExpandableSubjectCard(
                                subject = subject,
                                completedChapters = completedChapters,
                                chapterSessionMap = chapterSessionMap,
                                examResultsMap = examResultsMap,
                                viewModel = viewModel,
                                onStartChapter = onStartChapter
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        col2.forEach { subject ->
                            ExpandableSubjectCard(
                                subject = subject,
                                completedChapters = completedChapters,
                                chapterSessionMap = chapterSessionMap,
                                examResultsMap = examResultsMap,
                                viewModel = viewModel,
                                onStartChapter = onStartChapter
                            )
                        }
                    }
                }
            }
        } else {
            items(viewModel.syllabusSubjects, key = { it.subject }) { subject ->
                ExpandableSubjectCard(
                    subject = subject,
                    completedChapters = completedChapters,
                    chapterSessionMap = chapterSessionMap,
                    examResultsMap = examResultsMap,
                    viewModel = viewModel,
                    onStartChapter = onStartChapter
                )
            }
        }
    }
}

@Composable
fun ExpandableSubjectCard(
    subject: SyllabusSubject,
    completedChapters: Set<String>,
    chapterSessionMap: Map<Pair<String, String>, List<com.example.data.StudySession>>,
    examResultsMap: Map<String, List<com.example.data.ChapterExamResult>>,
    viewModel: MainViewModel,
    onStartChapter: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = remember(subject.subject) {
        when {
            subject.subject.contains("Botany") -> Icons.Rounded.Eco
            subject.subject.contains("Zoology") -> Icons.Rounded.Pets
            subject.subject.contains("Chemistry") -> Icons.Rounded.Science
            subject.subject.contains("Physics") -> Icons.Rounded.Speed
            subject.subject.contains("English") -> Icons.Rounded.MenuBook
            subject.subject.contains("General Knowledge") -> Icons.Rounded.Public
            else -> Icons.Rounded.HistoryEdu
        }
    }

    val iconTint = remember(subject.subject) {
        when {
            subject.subject.contains("Botany") -> Color(0xFF10B981)
            subject.subject.contains("Zoology") -> Color(0xFF3B82F6)
            subject.subject.contains("Chemistry") -> Color(0xFFF59E0B)
            subject.subject.contains("Physics") -> Color(0xFF8B5CF6)
            subject.subject.contains("English") -> Color(0xFFEC4899)
            subject.subject.contains("General") -> Color(0xFF06B6D4)
            else -> PrimaryTeal
        }
    }

    val subjectEnglishKey = remember(subject.subject) {
        getSubjectEnglish(subject.subject).lowercase()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = "Icon", tint = iconTint, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(subject.subject, color = LightText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("মোট অধ্যায়: ${subject.totalChapters} টি", color = GrayText, fontSize = 12.sp)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = GrayText
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(color = BorderColor, thickness = 1.dp)
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        subject.chapters.forEach { chapter ->
                            val key = "${subject.subject}_${chapter.name}"
                            val isChecked = completedChapters.contains(key)
                            val chapterKey = subjectEnglishKey to chapter.name.trim().lowercase()
                            val chapterSessions = chapterSessionMap[chapterKey] ?: emptyList()
                            val examResults = examResultsMap[key] ?: emptyList()
                            ChapterItemRow(
                                subjectName = subject.subject,
                                chapter = chapter,
                                isChecked = isChecked,
                                sessions = chapterSessions,
                                examResults = examResults,
                                onAddExamResult = { score ->
                                    viewModel.addChapterExamResult(subject.subject, chapter.name, score)
                                },
                                onDeleteExamResult = { id ->
                                    viewModel.deleteChapterExamResult(id)
                                },
                                onToggle = { checked ->
                                    viewModel.toggleChapterCompletion(subject.subject, chapter.name, checked)
                                },
                                onStartChapter = onStartChapter
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterItemRow(
    subjectName: String,
    chapter: SyllabusChapter,
    isChecked: Boolean,
    sessions: List<com.example.data.StudySession>,
    examResults: List<com.example.data.ChapterExamResult>,
    onAddExamResult: (Double) -> Unit,
    onDeleteExamResult: (Int) -> Unit,
    onToggle: (Boolean) -> Unit,
    onStartChapter: (String, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isChecked) ProgressGradientEnd else Color.Transparent)
                        .border(
                            width = if (isChecked) 0.dp else 1.5.dp,
                            color = if (isChecked) Color.Transparent else GrayText,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onToggle(!isChecked) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Checked",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(chapter.name, color = LightText, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Desc",
                            tint = GrayText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..5) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Star",
                                tint = if (i <= chapter.rating) OrangeAccent else GrayText.copy(alpha = 0.3f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryTeal)
                    .clickable { onStartChapter(subjectName, chapter.name) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("স্টার্ট", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Description
                Text(chapter.description, color = GrayText, fontSize = 12.sp, lineHeight = 18.sp)
                
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = BorderColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))
                
                // Chapter Stats Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "Stats Icon",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "চ্যাপ্টার স্টাডি অ্যানালাইসিস", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                val totalMin = sessions.sumOf { it.durationMinutes }
                val totalHrs = totalMin.toFloat() / 60f
                val formattedHrs = if (totalHrs % 1f == 0f) totalHrs.toInt().toString() else String.format("%.1f", totalHrs)
                
                val pomodoroCount = sessions.count { it.sessionType.contains("Pomodoro") || it.sessionType.contains("পোমোডোরো") }
                val deepCount = sessions.count { it.sessionType.contains("Deep") || it.sessionType.contains("ডিপ") }
                val normalCount = sessions.count { it.sessionType.contains("Normal") || it.sessionType.contains("সাধারণ") }
                val totalCount = sessions.size
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.4f))
                        .border(1.dp, BorderColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("মোট পড়াশোনা: $formattedHrs ঘণ্টা", color = Color(0xFF2CD4A0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("মোট সেশন (সম্পূর্ণ): ${totalCount}টি", color = LightText, fontSize = 11.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("পোমোডোরো সেশন: ${pomodoroCount}টি", color = GrayText, fontSize = 10.sp)
                        Text("ডিপ ওয়ার্ক সেশন: ${deepCount}টি", color = GrayText, fontSize = 10.sp)
                        Text("সাধারণ সেশন: ${normalCount}টি", color = GrayText, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = BorderColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(18.dp))

                // Chapter Exam Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Exam Tracker",
                        tint = OrangeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "চ্যাপ্টার এক্সাম ট্র্যাকার (১০০ মার্ক)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (examResults.isNotEmpty()) {
                    val avgScore = examResults.map { it.percentage }.average()
                    val maxScore = examResults.map { it.percentage }.maxOrNull() ?: 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("গড় স্কোর", color = GrayText, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format("%.1f%%", avgScore),
                                    color = if (avgScore >= 80.0) Color(0xFF2CD4A0) else if (avgScore >= 60.0) Color(0xFF3B82F6) else if (avgScore >= 40.0) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("সর্বোচ্চ স্কোর", color = GrayText, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format("%.1f%%", maxScore),
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        examResults.forEachIndexed { index, result ->
                            val examNumNative = when (index + 1) {
                                1 -> "১ম পরীক্ষা"
                                2 -> "২য় পরীক্ষা"
                                3 -> "৩য় পরীক্ষা"
                                4 -> "৪র্থ পরীক্ষা"
                                5 -> "৫ম পরীক্ষা"
                                else -> "${index + 1}তম পরীক্ষা"
                            }

                            val scoreColor = if (result.percentage >= 80.0) {
                                Color(0xFF10B981)
                            } else if (result.percentage >= 60.0) {
                                Color(0xFF3B82F6)
                            } else if (result.percentage >= 40.0) {
                                Color(0xFFF59E0B)
                            } else {
                                Color(0xFFEF4444)
                            }

                            val badgeBg = scoreColor.copy(alpha = 0.15f)

                            val ratingText = if (result.percentage >= 80.0) {
                                "অসাধারণ! 🌟"
                            } else if (result.percentage >= 60.0) {
                                "উত্তম 👍"
                            } else if (result.percentage >= 40.0) {
                                "চলতি 🎯"
                            } else {
                                "কমতি ⚠️"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF111827).copy(alpha = 0.8f))
                                    .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(scoreColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = scoreColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = examNumNative,
                                                    color = LightText,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = ratingText,
                                                    color = scoreColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                val percentStripped = if (result.percentage % 1.0 == 0.0) "${result.percentage.toInt()}" else String.format("%.1f", result.percentage)
                                                Text(
                                                    text = "$percentStripped%",
                                                    color = scoreColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            androidx.compose.material3.IconButton(
                                                onClick = { onDeleteExamResult(result.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete result",
                                                    tint = Color(0xFFEF4444).copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    LinearProgressIndicator(
                                        progress = { (result.percentage / 100.0).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = scoreColor,
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        "কোনো পরীক্ষার ফলাফল এখনও যুক্ত করা হয়নি।",
                        color = GrayText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Add Mark Section
                var scoreInput by remember { mutableStateOf("") }
                var scoreError by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Compact Input Field Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = if (scoreError) Color(0xFFEF4444) else BorderColor.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Percent icon",
                                tint = OrangeAccent.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Box(modifier = Modifier.weight(1f)) {
                                if (scoreInput.isEmpty()) {
                                    Text(
                                        text = "পরীক্ষার মার্ক (%) উদা: ৮৫",
                                        color = GrayText.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontFamily = com.example.ui.theme.HindSiliguri
                                    )
                                }
                                
                                androidx.compose.foundation.text.BasicTextField(
                                    value = scoreInput,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                            scoreInput = input
                                            scoreError = false
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = com.example.ui.theme.HindSiliguri,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2CD4A0)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sleek unified Button
                    Button(
                        onClick = {
                            val score = scoreInput.toDoubleOrNull()
                            if (score != null && score in 0.0..100.0) {
                                onAddExamResult(score)
                                scoreInput = ""
                                scoreError = false
                            } else {
                                scoreError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2CD4A0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "যোগ করুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0C1017),
                            fontFamily = com.example.ui.theme.HindSiliguri
                        )
                    }
                }
                if (scoreError) {
                    Text(
                        text = "অনুগ্রহ করে ০ থেকে ১০০ এর মধ্যে একটি নম্বর দিন",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontFamily = com.example.ui.theme.HindSiliguri,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}
