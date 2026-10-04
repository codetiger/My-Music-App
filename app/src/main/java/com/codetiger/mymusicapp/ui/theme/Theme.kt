package com.codetiger.mymusicapp.ui.theme

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// Mapping from design-system/README.md "In Compose". Outlines are transparent and
// nothing is tinted, so every shape stays flat.
private val MusicColorScheme = lightColorScheme(
    primary = MusicColors.Accent,
    onPrimary = MusicColors.OnAccent,
    primaryContainer = MusicColors.Fill,
    onPrimaryContainer = MusicColors.Ink,
    secondary = MusicColors.Accent,
    onSecondary = MusicColors.OnAccent,
    secondaryContainer = MusicColors.Fill,
    onSecondaryContainer = MusicColors.Ink,
    tertiary = MusicColors.Accent,
    onTertiary = MusicColors.OnAccent,
    tertiaryContainer = MusicColors.Fill,
    onTertiaryContainer = MusicColors.Ink,
    background = MusicColors.Surface,
    onBackground = MusicColors.Ink,
    surface = MusicColors.Surface,
    onSurface = MusicColors.Ink,
    surfaceVariant = MusicColors.Fill,
    onSurfaceVariant = MusicColors.Ink,
    surfaceTint = Color.Transparent,
    surfaceBright = MusicColors.Surface,
    surfaceDim = MusicColors.Fill,
    surfaceContainerLowest = MusicColors.Surface,
    surfaceContainerLow = MusicColors.Fill,
    surfaceContainer = MusicColors.Fill,
    surfaceContainerHigh = MusicColors.Fill,
    surfaceContainerHighest = MusicColors.Fill,
    inverseSurface = MusicColors.Ink,
    inverseOnSurface = MusicColors.Surface,
    inversePrimary = MusicColors.Fill,
    error = MusicColors.Ink,
    onError = MusicColors.Surface,
    errorContainer = MusicColors.Fill,
    onErrorContainer = MusicColors.Ink,
    outline = Color.Transparent,
    outlineVariant = Color.Transparent,
    scrim = MusicColors.Scrim,
)

private val MusicShapes = Shapes(
    extraSmall = Radius.Sm,
    small = Radius.Sm,
    medium = Radius.Md,
    large = Radius.Lg,
    extraLarge = Radius.Lg,
)

@Composable
fun MyMusicTheme(
    textSize: TextSize = TextSize.Normal,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = effectiveFontScale(density.fontScale, textSize),
    )
    MaterialTheme(
        colorScheme = MusicColorScheme,
        typography = MusicTypography,
        shapes = MusicShapes,
    ) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalMinimumInteractiveComponentSize provides Size.Target,
            content = content,
        )
    }
}
