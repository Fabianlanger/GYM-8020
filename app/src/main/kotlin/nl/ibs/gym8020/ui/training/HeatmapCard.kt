package nl.ibs.gym8020.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.deco.BodyFigure
import nl.ibs.gym8020.ui.deco.HatchSwatch
import nl.ibs.gym8020.ui.deco.HeatScale
import nl.ibs.gym8020.ui.deco.bodyBack
import nl.ibs.gym8020.ui.deco.bodyFront
import nl.ibs.gym8020.ui.deco.heatColor
import nl.ibs.gym8020.ui.exercises.muscleName
import nl.ibs.gym8020.ui.theme.Gym

/**
 * Muscle heatmap (v0.4): front and back as separate figures, colored by magnitude (dashboard).
 * The summary has its own card with comparisons and skipped muscles ([MuscleMapCard], #67).
 */
@Composable
fun HeatmapCard(
    total: Map<Muscle, Double>,
    unit: WeightUnit,
    title: String = stringResource(R.string.heat_title),
    totalFormat: (Double) -> String = { WeightFormat.format(it, unit) + " " + unit.name.lowercase() },
) {
    var selected by remember { mutableStateOf<Muscle?>(null) }
    DecoCard(corners = true, padding = 14.dp) {
        Eyebrow(title, color = Gym.Gold)
        BodyFigures(total, HeatScale.SEQUENTIAL, selected, onSelect = { selected = it })
        // Selected muscle, or the legend.
        val sel = selected
        if (sel != null) {
            val v = total[sel]
            val text = if (v == null) stringResource(R.string.heat_no_data) else totalFormat(v)
            Text("${muscleName(sel)}  ·  $text", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text(stringResource(R.string.heat_tap), style = MaterialTheme.typography.bodySmall, color = Gym.Muted, modifier = Modifier.padding(top = 8.dp))
        }
        HeatLegend(HeatScale.SEQUENTIAL)
    }
}

/** Front and back figure side by side. */
@Composable
internal fun BodyFigures(
    values: Map<Muscle, Double>,
    scale: HeatScale,
    selected: Muscle?,
    onSelect: (Muscle?) -> Unit,
    hatched: Set<Muscle> = emptySet(),
) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(R.string.heat_front to bodyFront, R.string.heat_back to bodyBack).forEach { (label, shapes) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                BodyFigure(shapes, values, scale, selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth(), hatched = hatched)
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
            }
        }
    }
}

/** Legend of a heat [scale]; with [skipped] also the striped "skipped" state (#67). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HeatLegend(scale: HeatScale, skipped: Boolean = false) {
    if (scale == HeatScale.SEQUENTIAL) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.heat_less), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
            Box(
                Modifier.weight(1f).height(8.dp)
                    .background(Brush.horizontalGradient(listOf(heatColor(0.01, scale, 1.0), heatColor(1.0, scale, 1.0))), RoundedCornerShape(4.dp)),
            )
            Text(stringResource(R.string.heat_more), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
            if (skipped) SkippedEntry()
        }
    } else {
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(-0.1 to R.string.heat_worse, 0.0 to R.string.heat_same, 0.1 to R.string.heat_better, null to R.string.heat_no_data).forEach { (v, label) ->
                LegendEntry(stringResource(label)) {
                    Box(Modifier.size(10.dp).background(heatColor(v, scale, 1.0), RoundedCornerShape(2.dp)))
                }
            }
            if (skipped) SkippedEntry()
        }
    }
}

@Composable
private fun SkippedEntry() {
    LegendEntry(stringResource(R.string.heat_skipped)) {
        HatchSwatch(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun LegendEntry(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        swatch()
        Text(label, style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
    }
}
