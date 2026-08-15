package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GrayText
import com.example.ui.theme.LightText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * High performance isolated leaf clock that updates every second
 * without triggering recomposition of parent components or screens.
 */
@Composable
fun LiveClockText(modifier: Modifier = Modifier) {
    val timeFormat = remember {
        SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
    }
    val dateFormat = remember {
        SimpleDateFormat("d MMMM, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
    }

    var currentTimeString by remember {
        mutableStateOf(timeFormat.format(Calendar.getInstance().time))
    }
    var currentDateString by remember {
        mutableStateOf(dateFormat.format(Calendar.getInstance().time))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000)
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        Text(
            text = currentTimeString,
            color = LightText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = currentDateString,
            color = GrayText,
            fontSize = 12.sp
        )
    }
}

/**
 * Shared, lightweight and battery-efficient Top App Bar
 */
@Composable
fun TopAppBarSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
                Text(
                    text = "MED-PREP",
                    color = LightText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2CD4A0))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "টার্গেট: ঢাকা মেডিকেল",
                        color = Color(0xFF2CD4A0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        LiveClockText()
    }
}
