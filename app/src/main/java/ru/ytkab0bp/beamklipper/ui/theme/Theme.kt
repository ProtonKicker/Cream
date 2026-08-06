package ru.ytkab0bp.beamklipper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrutalColorScheme,
        typography = AppTypography,
        content = content
    )
}
