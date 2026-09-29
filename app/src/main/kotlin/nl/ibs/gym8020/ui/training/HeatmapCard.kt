package nl.ibs.gym8020.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.deco.BodyFigure
import nl.ibs.gym8020.ui.deco.HeatScale
import nl.ibs.gym8020.ui.deco.bodyBack
import nl.ibs.gym8020.ui.deco.bodyFront
import nl.ibs.gym8020.ui.deco.heatColor
import nl.ibs.gym8020.ui.exercises.muscleName
import nl.ibs.gym8020.ui.theme.Gym

/**
 * Muscle heatmap (v0.4): front and back as separate figures. Two views when [progress] is given:
 * total volume (magnitude) and change vs. the previous time (better / same / worse).
 */
@Composable
fun HeatmapCard(
    total: Map<Muscle, Double>,
    progress: Map<Muscle, Double>?,
    unit: WeightUnit,
    title: String = stringResource(R.string.heat_title),
    totalFormat: (Double) -> String = { WeightFormat.format(it, unit) + " " + unit.name.lowercase() },
) {
    var showProgress by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Muscle?>(null) }
    val values = if (showProgress && progress != null) progress else total
    val scale = if (showProgress && progress != null) HeatScale.DIVERGING else HeatScale.SEQUENTIAL
    DecoCard(corners = true, padding = 14.dp) {
        Eyebrow(title, color = Gym.Gold)
        if (progress != null) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(Gym.Bg, RoundedCornerShape(12.dp)).padding(3.dp)) {
                listOf(false to R.string.heat_total, true to R.string.heat_progress).forEach { (p, label) ->
                    val sel = showProgress == p
                    Text(
                        stringResource(label),
                        modifier = Modifier.weight(1f).background(if (sel) Gym.Yellow else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { showProgress = p }.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center, color = if (sel) Gym.OnYellow else Gym.Cream, style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(R.string.heat_front to bodyFront, R.string.heat_back to bodyBack).forEach { (label, shapes) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BodyFigure(shapes, values, scale, selected, onSelect = { selected = it }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
                }
            }
        }
        // Selected muscle, or the legend.
        val sel = selected
        if (sel != null) {
            val v = values[sel]
            val text = when {
                v == null -> stringResource(R.string.heat_no_data)
                scale == HeatScale.DIVERGING -> (if (v >= 0) "+" else "−") + "${"%.1f".format(kotlin.math.abs(v) * 100).replace('.', ',')} %"
                else -> totalFormat(v)
            }
            Text("${muscleName(sel)}  ·  $text", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text(stringResource(R.string.heat_tap), style = MaterialTheme.typography.bodySmall, color = Gym.Muted, modifier = Modifier.padding(top = 8.dp))
        }
        Legend(scale)
    }
}

@Composable
private fun Legend(scale: HeatScale) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (scale == HeatScale.SEQUENTIAL) {
            Text(stringResource(R.string.heat_less), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
            Box(
                Modifier.weight(1f).height(8.dp)
                    .background(Brush.horizontalGradient(listOf(heatColor(0.01, scale, 1.0), heatColor(1.0, scale, 1.0))), RoundedCornerShape(4.dp)),
            )
            Text(stringResource(R.string.heat_more), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
        } else {
            listOf(-0.1 to R.string.heat_worse, 0.0 to R.string.heat_same, 0.1 to R.string.heat_better, null to R.string.heat_no_data).forEach { (v, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).background(heatColor(v, scale, 1.0), RoundedCornerShape(2.dp)))
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
                }
            }
        }
    }
}
