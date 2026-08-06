package ru.ytkab0bp.beamklipper.ui.theme

import androidx.compose.ui.graphics.Color

val Paper = Color(0xFFFFF8E6)
val PaperAlt = Color(0xFFF2ECD6)
val Ink = Color(0xFF171512)
val InkMuted = Color(0x8C171512)
val InkDim = Color(0x5C171512)
val Accent = Color(0xFFE6402F)
val InkOnAccent = Color(0xFFFFFFFF)
val Ok = Color(0xFF2F9E44)
val InkOnOk = Color(0xFFFFFFFF)
val Warn = Color(0xFFD99A1B)
val InkOnWarn = Color(0xFF171512)
val StripeLine = Color(0x1A171512)

fun cardColor(index: Int): Color {
    val idx = Math.floorMod(Math.abs(index), 4)
    return when (idx) {
        0 -> Paper
        1 -> PaperAlt
        2 -> Paper
        else -> PaperAlt
    }
}
