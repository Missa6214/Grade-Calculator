package com.gradecalculator.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryPurple     = Color(0xFF6C63FF)
val PrimaryLight      = Color(0xFF9D97FF)
val BackgroundDark    = Color(0xFF0F1117)
val SurfaceDark       = Color(0xFF1A1D27)
val SurfaceMedium     = Color(0xFF242838)
val OnSurface         = Color(0xFFEEEEF5)
val TextSecondary     = Color(0xFF8B8FA8)
val BorderColor       = Color(0xFF2E3250)

val GradeA            = Color(0xFF00D4AA)
val GradeB            = Color(0xFF4FC3F7)
val GradeC            = Color(0xFFFFB347)
val GradeD            = Color(0xFFFFB3B3)
val GradeF            = Color(0xFFFF6B6B)

private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryPurple,
    onPrimary        = Color.White,
    background       = BackgroundDark,
    onBackground     = OnSurface,
    surface          = SurfaceDark,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceMedium,
    onSurfaceVariant = TextSecondary,
    secondary        = GradeA,
    onSecondary      = Color.Black,
    error            = GradeF,
    onError          = Color.White,
    outline          = BorderColor
)

fun gradeColor(grade: String): Color = when {
    grade.startsWith("A") -> GradeA
    grade.startsWith("B") -> GradeB
    grade.startsWith("C") -> GradeC
    grade.startsWith("D") -> GradeD
    else                  -> GradeF
}

@Composable
fun GradeCalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content
    )
}