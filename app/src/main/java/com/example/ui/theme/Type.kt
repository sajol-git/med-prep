package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

val HindSiliguri = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_medium, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = HindSiliguri),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = HindSiliguri),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = HindSiliguri),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = HindSiliguri),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = HindSiliguri),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = HindSiliguri),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = HindSiliguri),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = HindSiliguri),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = HindSiliguri),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = HindSiliguri),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = HindSiliguri),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = HindSiliguri),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = HindSiliguri),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = HindSiliguri),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = HindSiliguri)
)
