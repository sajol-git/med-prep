package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SessionScreen(
    viewModel: MainViewModel,
    subject: String = "",
    chapter: String = "",
    onNavigateToTimer: (String) -> Unit = {},
    onClose: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground), // Mocking the background as a dark dim background
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top close button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GrayText,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onClose() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hourglass Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF33251A)), // Dark brownish-orange bg
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Session",
                        tint = OrangeAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titles
                Text(
                    text = if (chapter.isNotEmpty()) chapter else "Normal Session",
                    color = LightText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "সেশনের ধরণ নির্বাচন করুন",
                    color = GrayText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Options
                SessionOptionCard(
                    icon = Icons.Default.Timer,
                    iconTint = ProgressGradientStart, // Blueish
                    title = "সাধারণ সেশন",
                    description = "স্বাভাবিকভাবে স্টপ না করা পর্যন্ত সময় কাউন্ট হবে",
                    onClick = { onNavigateToTimer("সাধারণ সেশন") }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                SessionOptionCard(
                    icon = Icons.Default.Work,
                    iconTint = ProgressGradientEnd, // Greenish
                    title = "পোমোডোরো (২৫ মি)",
                    description = "২৫ মিনিট পড়াশোনা + ৫ মিনিট বিশ্রামের সাইকেল",
                    onClick = { onNavigateToTimer("পোমোডোরো (২৫ মি)") }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                SessionOptionCard(
                    icon = Icons.Default.Lock,
                    iconTint = OrangeAccent, // Yellowish
                    title = "ডিপ ওয়ার্ক",
                    description = "নিবিড় মনোযোগ দিয়ে একটানা সময় ট্র্যাকিং",
                    onClick = { onNavigateToTimer("ডিপ ওয়ার্ক") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SessionOptionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                color = LightText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = GrayText,
                fontSize = 12.sp
            )
        }
    }
}
