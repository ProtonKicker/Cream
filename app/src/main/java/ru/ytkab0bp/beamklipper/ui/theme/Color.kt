package ru.ytkab0bp.beamklipper.ui.theme

import androidx.compose.ui.graphics.Color

val CreamPrimary = Color(0xFFF2DA9C)
val CreamPrimaryDark = Color(0xFFDCC07A)
val CreamPrimaryLight = Color(0xFFFAECC2)
val CreamOnPrimary = Color(0xFF3B2C05)

val SurfaceCream = Color(0xFFFFF1CE)
val SurfaceCreamContainer = Color(0xFFF6DEA3)
val CardOutlineColor = Color(0x00000000)
val NavigationBarColor = Color(0xFFFFF8E6)
val WindowBackground = Color(0xFFFFF8E6)
val DividerColor = Color(0x0D000000)
val SnippetSurface = Color(0xFFFAE6B0)
val SnippetOutline = Color(0x14F2DA9C)

val TextColorOnCream = Color(0xFF4A3910)
val TextColorOnCreamSecondary = Color(0xFF7C6C47)
val CreamSurfaceHighest = Color(0xFFFAECC2)

val CreamPrimaryNight = Color(0xFFE2C47E)
val CreamPrimaryDarkNight = Color(0xFFC9A85A)
val CreamPrimaryLightNight = Color(0xFFF0D79A)
val CreamOnPrimaryNight = Color(0xFF2F2103)

val SurfaceCreamNight = Color(0xFF1C160D)
val SurfaceCreamContainerNight = Color(0xFF2D2415)
val NavigationBarColorNight = Color(0xFF15110A)
val WindowBackgroundNight = Color(0xFF15110A)
val DividerColorNight = Color(0x1FE2C47E)
val SnippetSurfaceNight = Color(0xFF231C10)
val SnippetOutlineNight = Color(0x2BE2C47E)

val TextColorOnCreamNight = Color(0xFFEAD9AC)
val TextColorOnCreamSecondaryNight = Color(0xFFB7A774)
val CreamSurfaceHighestNight = Color(0xFF372C18)

val CardColorsLight = listOf(
    Color(0xFFF2DA9C), Color(0xFFE8CB88), Color(0xFFEED69A), Color(0xFFD4BA70),
    Color(0xFFF0DDA8), Color(0xFFE0C682), Color(0xFFF2DCA5), Color(0xFFDAC47C),
    Color(0xFFE8D092), Color(0xFFF0DBA0)
)

val CardColorsDark = listOf(
    Color(0xFFE2C47E), Color(0xFFD6B56A), Color(0xFFEBD294), Color(0xFFC8A555),
    Color(0xFFEED79A), Color(0xFFDBBA72), Color(0xFFF0D99C), Color(0xFFC39F51),
    Color(0xFFE6CD89), Color(0xFFEBD395)
)

val InstanceCardLight = Color(0xFFF7EFD9)
val InstanceCardDark = Color(0xFF2D2211)
val OnInstanceCardLight = Color(0xFF4A3418)
val OnInstanceCardDark = Color(0xFFEAD9AC)
val InstanceCardIconTintLight = Color(0xFF4A3418)
val InstanceCardIconTintDark = Color(0xFFEAD9AC)

val WebCardLightFluidd = Color(0xFFE6C778)
val WebCardLightMainsail = Color(0xFFE5B567)
val OnWebCardLight = Color(0xFF3A2510)

val WebCardDarkFluidd = Color(0xFFE2C47E)
val WebCardDarkMainsail = Color(0xFFE0B770)
val OnWebCardDark = Color(0xFF2B1E08)

fun cardColor(index: Int, darkTheme: Boolean): Color {
    val idx = Math.floorMod(Math.abs(index), 10)
    return if (darkTheme) CardColorsDark[idx] else CardColorsLight[idx]
}
