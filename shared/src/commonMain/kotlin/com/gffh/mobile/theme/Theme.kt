package com.gffh.mobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette - the exact hex values from gffh-web's src/theme/theme.ts
// `brand` object (and its dark-mode getTheme('dark') extension), so the two
// platforms are genuinely the same brand rather than two shades of green/navy
// that happened to start from the same design brief.
private val Void = Color(0xFF14213D)
private val VoidLight = Color(0xFF22406B)
private val Pitch = Color(0xFF229A46)
private val PitchDeep = Color(0xFF167A38)
private val Lime = Color(0xFF6FC78A)
private val Mist = Color(0xFFEEF1F5)
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF101B2E)
private val Muted = Color(0xFF5C6A85)
private val Border = Color(0xFFE0E4EC)
private val Amber = Color(0xFFA85E10)
private val AmberBg = Color(0xFFFBEEDA)
private val Coral = Color(0xFFAE392D)
private val CoralBg = Color(0xFFFAE9E6)

// Dark-mode ground - on-brand navy tones (not Material's default grey-black),
// matching gffh-web's darkGround values exactly. surfaceVariant/error/tertiary
// container tones don't have a web equivalent to mirror (web only needed to
// fix background/paper/text/divider - see theme.ts's comment on why), so
// they're a self-consistent extension in the same navy/amber/coral families.
private val DarkBackground = Color(0xFF0B1626)
private val DarkPaper = Color(0xFF141F36)
private val DarkSurfaceVariant = Color(0xFF1B2740)
private val DarkInk = Color(0xFFF1F4F9)
private val DarkMuted = Color(0xFF8A97B2)
private val DarkBorder = Color(0xFF28334D)
private val DarkVoidLight = Color(0xFF2E4570)
private val DarkAmberBg = Color(0xFF3A2C14)
private val DarkAmberText = Color(0xFFE3A458)
private val DarkCoralBg = Color(0xFF3A1E1A)
private val DarkCoralText = Color(0xFFE58579)

val LightColors = lightColorScheme(
    primary = Pitch,
    onPrimary = Color.White,
    primaryContainer = Lime,
    onPrimaryContainer = PitchDeep,
    secondary = Void,
    error = Coral,
    errorContainer = CoralBg,
    onErrorContainer = Coral,
    tertiary = Amber,
    tertiaryContainer = AmberBg,
    onTertiaryContainer = Amber,
    background = Mist,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Muted,
    outline = Border,
    outlineVariant = Border,
)

val DarkColors = darkColorScheme(
    // Primary stays the same green in both modes, same call gffh-web makes:
    // pitch green reads fine on both a white and a dark-navy ground.
    primary = Pitch,
    onPrimary = Color.White,
    primaryContainer = PitchDeep,
    onPrimaryContainer = Lime,
    secondary = DarkVoidLight,
    error = Coral,
    errorContainer = DarkCoralBg,
    onErrorContainer = DarkCoralText,
    tertiary = Amber,
    tertiaryContainer = DarkAmberBg,
    onTertiaryContainer = DarkAmberText,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkPaper,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
)

@Composable
fun GffhTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = GffhTypography, content = content)
}
