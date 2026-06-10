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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TopAppBarSection()
        }
        items(viewModel.syllabusSubjects) { subject ->
            ExpandableSubjectCard(subject, completedChapters, recentSessions, viewModel, onStartChapter)
        }
        item {
            Spacer(modifier = Modifier.height(80.dp)) // padding for bottom nav
        }
    }
}

@Composable
fun ExpandableSubjectCard(
    subject: SyllabusSubject,
    completedChapters: Set<String>,
    recentSessions: List<com.example.data.StudySession>,
    viewModel: MainViewModel,
    onStartChapter: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = when {
        subject.subject.contains("Botany") -> Icons.Rounded.Eco
        subject.subject.contains("Zoology") -> Icons.Rounded.Pets
        subject.subject.contains("Chemistry") -> Icons.Rounded.Science
        subject.subject.contains("Physics") -> Icons.Rounded.Speed
        subject.subject.contains("English") -> Icons.Rounded.MenuBook
        subject.subject.contains("General Knowledge") -> Icons.Rounded.Public
        else -> Icons.Rounded.HistoryEdu
    }

    val iconTint = when {
        subject.subject.contains("Botany") -> Color(0xFF10B981)
        subject.subject.contains("Zoology") -> Color(0xFF3B82F6)
        subject.subject.contains("Chemistry") -> Color(0xFFF59E0B)
        subject.subject.contains("Physics") -> Color(0xFF8B5CF6)
        subject.subject.contains("English") -> Color(0xFFEC4899)
        subject.subject.contains("General") -> Color(0xFF06B6D4)
        else -> PrimaryTeal
    }

    // Filter sessions belonging to this subject
    val subjectSessions = recentSessions.filter {
        getSubjectEnglish(it.subject).equals(getSubjectEnglish(subject.subject), ignoreCase = true)
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
                            val chapterSessions = subjectSessions.filter {
                                it.chapter.trim().equals(chapter.name.trim(), ignoreCase = true)
                            }
                            ChapterItemRow(
                                subjectName = subject.subject,
                                chapter = chapter,
                                isChecked = isChecked,
                                sessions = chapterSessions,
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
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = BorderColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chapter Stats Card
                Text("চ্যাপ্টার স্টাডি অ্যানালাইসিস", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                val totalMin = sessions.sumOf { it.durationMinutes }
                val totalHrs = totalMin.toFloat() / 60f
                val formattedHrs = if (totalHrs % 1f == 0f) totalHrs.toInt().toString() else String.format("%.1f", totalHrs)
                
                val pomodoroCount = sessions.count { it.sessionType.contains("Pomodoro") || it.sessionType.contains("পোমোডোরো") }
                val deepCount = sessions.count { it.sessionType.contains("Deep") || it.sessionType.contains("ডিপ") }
                val normalCount = sessions.count { it.sessionType.contains("Normal") || it.sessionType.contains("সাধারণ") }
                val totalCount = sessions.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("মোট পড়াশোনা: $formattedHrs ঘণ্টা", color = Color(0xFF2CD4A0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("মোট সেশন (সম্পূর্ণ): ${totalCount}টি", color = LightText, fontSize = 12.sp)
                    }
                    Column {
                        Text("পোমোডোরো সেশন: ${pomodoroCount}টি", color = GrayText, fontSize = 11.sp)
                        Text("ডিপ ওয়ার্ক সেশন: ${deepCount}টি", color = GrayText, fontSize = 11.sp)
                        Text("সাধারণ সেশন: ${normalCount}টি", color = GrayText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
