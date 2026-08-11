package com.gffh.mobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand-neutral placeholder palette - no design system was supplied with the
// specification set. Pitch green as the primary, swap for brand colours when
// a visual identity exists.
private val Green = Color(0xFF2E7D46)
private val GreenDark = Color(0xFF8FD9A0)
private val Amber = Color(0xFFB26A00)
private val Red = Color(0xFFBA1A1A)

val LightColors = lightColorScheme(
    primary = Green,
    secondary = Color(0xFF4C6B52),
    error = Red,
    tertiary = Amber
)

val DarkColors = darkColorScheme(
    primary = GreenDark,
    secondary = Color(0xFFB3CCB7),
    error = Color(0xFFFFB4AB),
    tertiary = Color(0xFFFFB870)
)

@Composable
fun GffhTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

/** Score-band colours, per Screen Build Specification's ScoreChip band colouring. */
object ScoreBandColors {
    val high = Color(0xFF2E7D46)
    val mid = Color(0xFFB26A00)
    val low = Color(0xFF757575)
}
