package com.gffh.mobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette, matching gffh-web's theme.ts: green primary (#2E9E52, with
// #34B85B/#1E8E42 light/dark variants) and navy (#14213D) secondary.
private val GreenMain = Color(0xFF2E9E52)
private val GreenLight = Color(0xFF34B85B)
private val GreenDark = Color(0xFF1E8E42)
private val Navy = Color(0xFF14213D)
private val Amber = Color(0xFFB26A00)
private val Red = Color(0xFFBA1A1A)

val LightColors = lightColorScheme(
    primary = GreenMain,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6EFC4), // pale tint of GreenMain
    onPrimaryContainer = GreenDark,
    secondary = Navy,
    error = Red,
    tertiary = Amber
)

val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenDark,
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFB6EFC4), // pale tint of GreenMain
    secondary = Color(0xFFB3C0D9), // navy-derived tone, lightened for contrast on a dark surface
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
