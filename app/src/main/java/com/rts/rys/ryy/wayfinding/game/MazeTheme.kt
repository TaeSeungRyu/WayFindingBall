package com.rts.rys.ryy.wayfinding.game

import androidx.compose.ui.graphics.Color

/**
 * Per-level visual theme: wall/floor/sky palette + decoration colors.
 * Goal star and ball intentionally remain consistent across themes (brand identity).
 */
data class MazeTheme(
    val wallTop: Color,
    val wallMid: Color,
    val wallDeep: Color,
    val wallAccent: Color,
    val floorTile: Color,
    val floorTileAlt: Color,
    val floorDetailDot: Color,
    val floorDetailSplotch: Color,
    val skyTop: Color,
    val skyBottom: Color,
    val rockTop: Color,
    val rockBottom: Color,
    val flowerColors: List<Color>,
    val flowerCenter: Color,
    val cloudOpacity: Float = 1f,
    val isDark: Boolean = false
)

private val GrassTheme = MazeTheme(
    wallTop = Color(0xFFB8E07D),
    wallMid = Color(0xFF7CC150),
    wallDeep = Color(0xFF4F8B33),
    wallAccent = Color(0xFFCCEC91),
    floorTile = Color(0xFFFDF1D8),
    floorTileAlt = Color(0xFFF6E6BD),
    floorDetailDot = Color(0xFFA8C49B),
    floorDetailSplotch = Color(0xFFE8D9B5),
    skyTop = Color(0xFFBEE6FF),
    skyBottom = Color(0xFF7CC1E0),
    rockTop = Color(0xFFC4CED4),
    rockBottom = Color(0xFF8A98A2),
    flowerColors = listOf(Color(0xFFFFC1CC), Color(0xFFFFD89C), Color(0xFFCDB6F5)),
    flowerCenter = Color(0xFFFFE066),
    cloudOpacity = 1f
)

private val IceTheme = MazeTheme(
    wallTop = Color(0xFFD4ECF6),
    wallMid = Color(0xFF7FBCD3),
    wallDeep = Color(0xFF3C7894),
    wallAccent = Color(0xFFEFFAFF),
    floorTile = Color(0xFFEAF5FA),
    floorTileAlt = Color(0xFFD7E8F2),
    floorDetailDot = Color(0xFFA8C8DC),
    floorDetailSplotch = Color(0xFFC9DFEC),
    skyTop = Color(0xFFE2F4FC),
    skyBottom = Color(0xFFA8D2E4),
    rockTop = Color(0xFFB6CBD7),
    rockBottom = Color(0xFF6A8597),
    flowerColors = listOf(Color(0xFFD8EAF6), Color(0xFFB7D8E8), Color(0xFFE5EEF7)),
    flowerCenter = Color(0xFFFFFFFF),
    cloudOpacity = 1f
)

private val SandTheme = MazeTheme(
    wallTop = Color(0xFFEED39A),
    wallMid = Color(0xFFC9A26A),
    wallDeep = Color(0xFF876236),
    wallAccent = Color(0xFFF7E6BC),
    floorTile = Color(0xFFF8E7C2),
    floorTileAlt = Color(0xFFE9D29C),
    floorDetailDot = Color(0xFFB89060),
    floorDetailSplotch = Color(0xFFE2C188),
    skyTop = Color(0xFFFFDBA8),
    skyBottom = Color(0xFFFFA866),
    rockTop = Color(0xFFD3B894),
    rockBottom = Color(0xFF9C7B4C),
    flowerColors = listOf(Color(0xFFFFCC66), Color(0xFFFF9466), Color(0xFFD86B40)),
    flowerCenter = Color(0xFFFFE066),
    cloudOpacity = 0.55f
)

private val NightTheme = MazeTheme(
    wallTop = Color(0xFF6A5C92),
    wallMid = Color(0xFF3D3268),
    wallDeep = Color(0xFF1C1844),
    wallAccent = Color(0xFFB8AFE2),
    floorTile = Color(0xFF221B49),
    floorTileAlt = Color(0xFF1A1538),
    floorDetailDot = Color(0xFFE0DBF4),
    floorDetailSplotch = Color(0xFF2E2658),
    skyTop = Color(0xFF1A1A3E),
    skyBottom = Color(0xFF06061A),
    rockTop = Color(0xFF5D5470),
    rockBottom = Color(0xFF302955),
    flowerColors = listOf(Color(0xFFE7E0F8), Color(0xFFFFD6E8), Color(0xFFFFE7A8)),
    flowerCenter = Color(0xFFFFE066),
    cloudOpacity = 0f,
    isDark = true
)

private val MagicTheme = MazeTheme(
    wallTop = Color(0xFFE5B6FF),
    wallMid = Color(0xFFA465D9),
    wallDeep = Color(0xFF5C2B8E),
    wallAccent = Color(0xFFF6D8FF),
    floorTile = Color(0xFFFBEAFF),
    floorTileAlt = Color(0xFFF0D6F8),
    floorDetailDot = Color(0xFFC393E1),
    floorDetailSplotch = Color(0xFFE3C9F0),
    skyTop = Color(0xFFFFD1F3),
    skyBottom = Color(0xFFA886E2),
    rockTop = Color(0xFFC8B6D6),
    rockBottom = Color(0xFF867594),
    flowerColors = listOf(Color(0xFFFFC1CC), Color(0xFFFFD89C), Color(0xFFFFE066)),
    flowerCenter = Color(0xFFFFE066),
    cloudOpacity = 0.7f
)

private val OceanTheme = MazeTheme(
    wallTop = Color(0xFF7AD7F0),
    wallMid = Color(0xFF2196C2),
    wallDeep = Color(0xFF0F4F73),
    wallAccent = Color(0xFFCFEFFA),
    floorTile = Color(0xFFE2F4FB),
    floorTileAlt = Color(0xFFC9E4F0),
    floorDetailDot = Color(0xFF6BAEC8),
    floorDetailSplotch = Color(0xFFB6D7E6),
    skyTop = Color(0xFFB7E8FA),
    skyBottom = Color(0xFF3478A8),
    rockTop = Color(0xFFADC5D2),
    rockBottom = Color(0xFF5C7785),
    flowerColors = listOf(Color(0xFFFFB8C4), Color(0xFFFFE08A), Color(0xFFA8E9C9)),
    flowerCenter = Color(0xFFFFE066),
    cloudOpacity = 0.85f
)

private val ShadowTheme = MazeTheme(
    wallTop = Color(0xFF4A3A6D),
    wallMid = Color(0xFF241A40),
    wallDeep = Color(0xFF0A0620),
    wallAccent = Color(0xFF7A6BA1),
    floorTile = Color(0xFF1B1430),
    floorTileAlt = Color(0xFF120C24),
    floorDetailDot = Color(0xFF5E5078),
    floorDetailSplotch = Color(0xFF38305A),
    skyTop = Color(0xFF12082E),
    skyBottom = Color(0xFF050010),
    rockTop = Color(0xFF463B65),
    rockBottom = Color(0xFF1E1840),
    flowerColors = listOf(Color(0xFFE7E0F8), Color(0xFFFFD6E8), Color(0xFFFFE7A8)),
    flowerCenter = Color(0xFFFFE066),
    cloudOpacity = 0f,
    isDark = true
)

fun themeForLevel(level: Int): MazeTheme = when (level) {
    1 -> GrassTheme
    2 -> IceTheme
    3 -> SandTheme
    4 -> NightTheme
    5 -> MagicTheme
    6 -> OceanTheme
    else -> ShadowTheme
}
