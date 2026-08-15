package com.gffh.mobile.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Approximates gffh-web's theme.ts h1/h2/h3 overrides (Bebas Neue, uppercase,
 * tight leading) using the system font at [FontWeight.Black] plus wide
 * letter-spacing - bundling a condensed display TTF is out of scope for this
 * pass. Compose has no automatic uppercase-on-render, so a screen that wants
 * the display look must call `.uppercase()` on the string itself.
 *
 * Wired into [GffhTheme] as the Material3 `Typography`, so `MaterialTheme.
 * typography.headlineLarge/Medium/Small` carry this treatment app-wide - the
 * same role theme.ts's h1/h2/h3 play on web.
 */
val displayHeadline = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    letterSpacing = 0.8.sp,
    lineHeight = 36.sp,
)

val displayHeadlineMedium = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 28.sp,
    letterSpacing = 0.7.sp,
    lineHeight = 30.sp,
)

val displayHeadlineSmall = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 22.sp,
    letterSpacing = 0.6.sp,
    lineHeight = 24.sp,
)

/** Small uppercase "eyebrow" label style, as used above a [HeroBand] title. */
val eyebrowLabel = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    letterSpacing = 1.2.sp,
)

private val base = Typography()

val GffhTypography = base.copy(
    headlineLarge = base.headlineLarge.merge(displayHeadline),
    headlineMedium = base.headlineMedium.merge(displayHeadlineMedium),
    headlineSmall = base.headlineSmall.merge(displayHeadlineSmall),
)
