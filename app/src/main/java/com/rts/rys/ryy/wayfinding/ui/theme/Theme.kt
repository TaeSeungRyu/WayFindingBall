package com.rts.rys.ryy.wayfinding.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KidsColorScheme = lightColorScheme(
    primary = CoralPink,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = SkyBlue,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = SunYellow,
    onTertiary = InkDark,
    background = CreamBg,
    onBackground = InkDark,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = InkDark,
)

@Composable
fun ChildrenWayfindingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KidsColorScheme,
        typography = Typography,
        content = content
    )
}
