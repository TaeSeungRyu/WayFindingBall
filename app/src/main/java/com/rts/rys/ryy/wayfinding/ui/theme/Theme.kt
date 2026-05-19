package com.rts.rys.ryy.wayfinding.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MazeColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepNight,
    secondary = NeonPink,
    onSecondary = SoftWhite,
    tertiary = NeonYellow,
    onTertiary = DeepNight,
    background = DeepNight,
    onBackground = SoftWhite,
    surface = MidNight,
    onSurface = SoftWhite,
)

@Composable
fun ChildrenWayfindingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MazeColorScheme,
        typography = Typography,
        content = content
    )
}
