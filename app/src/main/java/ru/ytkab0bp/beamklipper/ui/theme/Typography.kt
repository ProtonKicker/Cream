package ru.ytkab0bp.beamklipper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.beamklipper.R

val RobotoMedium = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.roboto_medium, FontWeight.Medium)
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.04.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.06.sp
    )
)
