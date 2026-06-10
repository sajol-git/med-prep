package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MedPrepColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    secondary = AccentTeal,
    background = DarkBackground,
    surface = CardBackground,
    surfaceVariant = BottomNavBackground,
    onPrimary = Color.White,
    onSecondary = OrangeAccent,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = GrayText,
    outline = BorderColor,
    outlineVariant = BorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MedPrepColorScheme,
        typography = Typography,
        content = content
    )
}
