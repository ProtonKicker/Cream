package ru.ytkab0bp.beamklipper.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

private val LightColors = lightColorScheme(
    primary = CreamPrimary,
    onPrimary = CreamOnPrimary,
    primaryContainer = SurfaceCreamContainer,
    onPrimaryContainer = TextColorOnCream,
    secondary = CreamPrimaryLight,
    onSecondary = CreamOnPrimary,
    secondaryContainer = CreamPrimaryLight,
    onSecondaryContainer = TextColorOnCream,
    tertiary = CreamPrimaryDark,
    onTertiary = CreamOnPrimary,
    tertiaryContainer = SnippetSurface,
    onTertiaryContainer = TextColorOnCream,
    surface = WindowBackground,
    onSurface = TextColorOnCream,
    surfaceVariant = SurfaceCream,
    onSurfaceVariant = TextColorOnCreamSecondary,
    surfaceContainerLowest = WindowBackground,
    surfaceContainerLow = SurfaceCream,
    surfaceContainer = SnippetSurface,
    surfaceContainerHigh = SurfaceCreamContainer,
    surfaceContainerHighest = CreamSurfaceHighest,
    outline = DividerColor,
    outlineVariant = DividerColor,
)

private val DarkColors = darkColorScheme(
    primary = CreamPrimaryNight,
    onPrimary = CreamOnPrimaryNight,
    primaryContainer = SurfaceCreamContainerNight,
    onPrimaryContainer = TextColorOnCreamNight,
    secondary = CreamPrimaryLightNight,
    onSecondary = CreamOnPrimaryNight,
    secondaryContainer = CreamPrimaryDarkNight,
    onSecondaryContainer = TextColorOnCreamNight,
    tertiary = CreamPrimaryDarkNight,
    onTertiary = CreamOnPrimaryNight,
    tertiaryContainer = SnippetSurfaceNight,
    onTertiaryContainer = TextColorOnCreamNight,
    surface = WindowBackgroundNight,
    onSurface = TextColorOnCreamNight,
    surfaceVariant = SurfaceCreamNight,
    onSurfaceVariant = TextColorOnCreamSecondaryNight,
    surfaceContainerLowest = WindowBackgroundNight,
    surfaceContainerLow = SurfaceCreamNight,
    surfaceContainer = SnippetSurfaceNight,
    surfaceContainerHigh = SurfaceCreamContainerNight,
    surfaceContainerHighest = CreamSurfaceHighestNight,
    outline = DividerColorNight,
    outlineVariant = DividerColorNight,
)

fun AppColorScheme(darkTheme: Boolean) = if (darkTheme) DarkColors else LightColors
