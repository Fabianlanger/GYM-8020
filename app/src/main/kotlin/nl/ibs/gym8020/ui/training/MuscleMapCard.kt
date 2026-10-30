package nl.ibs.gym8020.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.stats.ComparisonFormat
import nl.ibs.gym8020.core.stats.MuscleBreakdown
import nl.ibs.gym8020.core.stats.MuscleMap
import nl.ibs.gym8020.core.stats.Trend
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.deco.HeatScale
import nl.ibs.gym8020.ui.exercises.muscleName
import nl.ibs.gym8020.ui.theme.Gym
import kotlin.math.roundToInt

/**
 * The muscle map in the summary (#67, decision #8 points 2, 3 and 5): "Totaal" is the volume against the
 * previous session of this workout, "Vs. last time" the strength of what you did. Muscles only trained by
 * skipped exercises are striped. Tap a muscle for the exercises that count and the ones you skipped.
 */
@Composable
fun MuscleMapCard(map: MuscleMap, exercises: Map<Long, ExerciseEntity>, unit: WeightUnit) {
    var showStrength by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Muscle?>(null) }
    val skipped = map.strength.filterValues { it.skipped }.keys
    val values: Map<Muscle, Double> =
        if (showStrength) map.strength.mapNotNull { (m, s) -> s.change?.let { m to it } }.toMap()
        else map.volume.mapValues { it.value.now }.filterValues { it > 0 }
    val scale = if (showStrength) HeatScale.DIVERGING else HeatScale.SEQUENTIAL
    DecoCard(corners = true, padding = 14.dp) {
        Eyebrow(stringResource(R.string.heat_title), color = Gym.Gold)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(Gym.Bg, RoundedCornerShape(12.dp)).padding(3.dp)) {
            listOf(false to R.string.heat_total, true to R.string.heat_progress).forEach { (p, label) ->
                val sel = showStrength == p
                Text(
                    stringResource(label),
                    modifier = Modifier.weight(1f).background(if (sel) Gym.Yellow else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { showStrength = p }.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center, color = if (sel) Gym.OnYellow else Gym.Cream, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        BodyFigures(values, scale, selected, onSelect = { selected = it }, hatched = skipped)
        val sel = selected
        if (sel != null) {
            MusclePanel(sel, map, showStrength, exercises, unit)
        } else {
            Text(stringResource(R.string.heat_tap), style = MaterialTheme.typography.bodySmall, color = Gym.Muted, modifier = Modifier.padding(top = 8.dp))
        }
        HeatLegend(scale, skipped = skipped.isNotEmpty())
    }
}

/** The tapped muscle: its number, the exercises that count with theirs, and the skipped ones. Inline, no dialog. */
@Composable
private fun MusclePanel(muscle: Muscle, map: MuscleMap, showStrength: Boolean, exercises: Map<Long, ExerciseEntity>, unit: WeightUnit) {
    val u = unit.name.lowercase()
    val breakdown: MuscleBreakdown? = map.breakdown[muscle]
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).background(Gym.Bg, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Headline: the muscle and its number in this view.
        val strength = map.strength[muscle]
        val volume = map.volume[muscle]
        val strengthChange = strength?.change
        val headline: Triple<String, String?, Trend?> = when {
            breakdown == null -> Triple(stringResource(R.string.heat_not_trained), null, null)
            showStrength && strength?.skipped == true -> Triple(stringResource(R.string.heat_skipped), null, null)
            showStrength && strengthChange == null -> Triple(stringResource(R.string.heat_no_data), null, null)
            showStrength -> Triple((if (strength?.trend == Trend.SAME) "≈ " else "") + ComparisonFormat.percent(strengthChange ?: 0.0), null, strength?.trend)
            volume == null -> Triple(stringResource(R.string.heat_no_data), null, null)
            else -> Triple(
                ComparisonFormat.volume(volume.now, unit) + " " + u,
                volume.change?.let { ch -> changeText(ComparisonFormat.volume(ch.absolute, unit, signed = true) + " " + u, ch.relative, volume.trend) },
                volume.trend,
            )
        }
        val (value, change, trend) = headline
        val name = muscleName(muscle)
        Text(
            buildAnnotatedString {
                append("$name  ·  ")
                if (change == null) {
                    withStyle(SpanStyle(color = if (showStrength) trendColor(trend) else Gym.Cream)) { append(value) }
                } else {
                    append("$value ")
                    withStyle(SpanStyle(color = trendColor(trend))) { append(change) }
                }
            },
            style = MaterialTheme.typography.titleMedium,
        )
        if (breakdown != null) {
            // The exercises that count for this muscle, in session order.
            if (showStrength) {
                breakdown.strength.forEach { p ->
                    val c = p.comparison
                    val text = exerciseChangeText(c, unit) ?: if (c.firstTime) stringResource(R.string.sum_first_time) else "—"
                    PartRow(exercises[p.exerciseId]?.name ?: "", p.percent, text, trendColor(c.trend))
                }
            } else {
                breakdown.volume.forEach { p ->
                    val share = p.share?.let { " · ${(it * 100).roundToInt()}%" } ?: ""
                    PartRow(exercises[p.exerciseId]?.name ?: "", p.percent, ComparisonFormat.volume(p.kg, unit) + " " + u + share, Gym.Cream)
                }
            }
            if (breakdown.skipped.isNotEmpty()) {
                Text(
                    stringResource(R.string.heat_skipped_list, breakdown.skipped.joinToString(", ") { exercises[it]?.name ?: "" }),
                    style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                )
            }
        }
    }
}

/** "Bench press  50%    +2,5 kg · +2,4%": the exercise, its share of this muscle when below 100 %, and its number. */
@Composable
private fun PartRow(name: String, percent: Int, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        if (percent < 100) Text("$percent%", style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}
