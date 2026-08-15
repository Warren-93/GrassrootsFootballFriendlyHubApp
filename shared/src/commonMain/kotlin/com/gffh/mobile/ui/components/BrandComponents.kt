package com.gffh.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gffh.mobile.theme.displayHeadlineMedium
import com.gffh.mobile.theme.displayHeadlineSmall
import com.gffh.mobile.theme.eyebrowLabel

/**
 * Reusable brand-styled composables approximating the PitchMate concept's
 * .a-hero/.a-stat/.crest/ScoreChip patterns - see gffh-web's brand component
 * files under src/components/brand for the visual language these mirror
 * (React, not shared code, but the source of truth for colours/thresholds). Follows
 * [PostcodeLocationField]'s convention of one focused composable per concern
 * in this package.
 */

// Navy gradient stops, matching gffh-web theme.ts's brand.void / brand.voidLight.
val HeroNavy = Color(0xFF14213D)
val HeroNavyLight = Color(0xFF22406B)
val HeroLime = Color(0xFF6FC78A)

/**
 * The same navy gradient [HeroBand] uses, exposed so full-bleed marketing
 * screens (Welcome, Sign in) can paint their whole background with it rather
 * than nesting a HeroBand inside a page.
 */
fun heroGradientBrush(): Brush = Brush.linearGradient(colors = listOf(HeroNavy, HeroNavyLight, HeroNavy))

/**
 * Navy gradient band used at the top of key screens (auth marketing screens,
 * dashboard). Mirrors gffh-web's HeroBand.tsx: an optional uppercase eyebrow
 * in light green, a display-style title, an optional subtitle, both in white.
 */
@Composable
fun HeroBand(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    compact: Boolean = false,
    action: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroNavy, HeroNavyLight, HeroNavy)
                )
            )
            .padding(horizontal = 24.dp, vertical = if (compact) 20.dp else 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                eyebrow?.let {
                    Text(
                        it.uppercase(),
                        style = eyebrowLabel,
                        color = HeroLime,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    title.uppercase(),
                    style = if (compact) displayHeadlineSmall else displayHeadlineMedium,
                    color = Color.White
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            action?.let {
                Spacer(Modifier.width(12.dp))
                it()
            }
        }
    }
}

/**
 * Small metric card: label, a big bold value, an optional sub-line. Mirrors
 * gffh-web's StatTile.tsx, including the coloured left accent border (green
 * by default, amber when [tone] flags something needing attention).
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    tone: StatTileTone = StatTileTone.Default,
) {
    val accent = when (tone) {
        StatTileTone.Default -> MaterialTheme.colorScheme.primary
        StatTileTone.Amber -> MaterialTheme.colorScheme.tertiary
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(64.dp)
                    .background(accent)
            )
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                sub?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

enum class StatTileTone { Default, Amber }

/** Lays StatTiles out in a row with even spacing, wrapping the way StatTileRow does on web. */
@Composable
fun StatTileRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

// Score-band colours matching gffh-web's MatchScoreChip.tsx exactly (>=85 green, >=70 amber, else neutral).
private val ScoreHighBg = Color(0xFFE4F4E4)
private val ScoreHighText = Color(0xFF167A38)
private val ScoreMidBg = Color(0xFFFBEEDA)
private val ScoreMidText = Color(0xFFA85E10)
private val ScoreLowBg = Color(0xFFEEF1F5)
private val ScoreLowText = Color(0xFF5C6A85)

/**
 * "N% match" pill, colour-banded exactly like gffh-web's MatchScoreChip.tsx:
 * >=85 green, >=70 amber, below that neutral grey.
 */
@Composable
fun MatchScoreChip(score: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val (bg, fg) = when {
        score >= 85 -> ScoreHighBg to ScoreHighText
        score >= 70 -> ScoreMidBg to ScoreMidText
        else -> ScoreLowBg to ScoreLowText
    }
    AssistChip(
        onClick = onClick ?: {},
        label = { Text("$score% match", fontWeight = FontWeight.Bold) },
        colors = AssistChipDefaults.assistChipColors(containerColor = bg, labelColor = fg),
        border = null,
        modifier = modifier
    )
}

/**
 * A team's badge: a light rounded-square with its initials, distinct from a
 * circular user avatar so teams and people never look alike in a list.
 * Mirrors gffh-web's CrestAvatar.tsx.
 */
@Composable
fun CrestAvatar(name: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.5f))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(size / 3.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            crestInitials(name),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun crestInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
