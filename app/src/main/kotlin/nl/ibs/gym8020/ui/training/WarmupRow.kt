package nl.ibs.gym8020.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.DefaultWarmup
import nl.ibs.gym8020.core.logic.WarmupMeasure
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.data.defaultWarmup
import nl.ibs.gym8020.data.warmupMeasure
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.theme.Gym

/*
 * The default warm-up on the training screen (#93): the Warm-up button shows it, tapping fills the warm-up set with
 * it, and "Bewaar … als standaard" under the set when you entered something else. Not tapped = skipped, no warning.
 */

/**
 * The default warm-up as the training uses it: yours, or while it has no value yet the prefill from this training's
 * material (stored on the exercise once you tap Warm-up).
 */
internal fun effectiveDefaultWarmup(item: TrainingItem): DefaultWarmup? {
    val ex = item.exercise
    val measure = ex.warmupMeasure
    val prefill = DefaultWarmup.prefill(measure, ex.loadType, ex.bodyweightFactor, ex.equipment, item.calc, item.warmupAssistKg)
    return DefaultWarmup.effective(ex.defaultWarmup, measure, prefill)
}

/** A default warm-up value as text: "40 kg", "Assistance 40 kg", "Bodyweight only", "+5 kg" or "5 reps". */
@Composable
internal fun warmupValueText(measure: WarmupMeasure, value: Double, unit: WeightUnit): String {
    val unitName = unit.name.lowercase()
    return when (measure) {
        WarmupMeasure.REPS -> stringResource(R.string.warmup_reps_value, Math.round(value).toInt())
        WarmupMeasure.WEIGHT -> WeightFormat.format(value, unit) + " " + unitName
        WarmupMeasure.SIGNED_WEIGHT -> when {
            value < 0 -> stringResource(R.string.ld_assist, WeightFormat.format(-value, unit) + " " + unitName)
            value == 0.0 -> stringResource(R.string.tr_warmup_hint_bodyweight)
            else -> WeightFormat.format(value, unit, signed = true) + " " + unitName
        }
    }
}

/** The Warm-up button before GO: "Warm-up · 40 kg" with the note of your default warm-up under it. */
@Composable
internal fun WarmupButton(item: TrainingItem, unit: WeightUnit, assistPercent: Int, onClick: () -> Unit) {
    val default = effectiveDefaultWarmup(item)
    val value = default?.value
    val text = if (default != null && value != null) warmupValueText(default.measure, value, unit) else warmupHint(item, unit, assistPercent)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LineButton(
            stringResource(R.string.tr_warmup_button, text),
            onClick = onClick, icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.fillMaxWidth(),
        )
        default?.note?.let { Text(it, color = Gym.Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

/** The open warm-up set: the value of your warm-up (weight, plus-or-minus weight) and the reps. */
@Composable
internal fun WarmupSetEditor(
    item: TrainingItem,
    set: SetEntity,
    unit: WeightUnit,
    step: Double,
    onChange: (SetEntity) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val measure = item.exercise.warmupMeasure
    val signed = measure == WarmupMeasure.SIGNED_WEIGHT
    val calc = item.calc
    Column(
        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(20.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoleBadge(set.role)
            Text(
                roleLabel(set.role).uppercase(),
                style = MaterialTheme.typography.labelMedium, color = Gym.Gold, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, stringResource(R.string.tr_set_delete), tint = Gym.Muted, modifier = Modifier.size(18.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (measure != WarmupMeasure.REPS) {
                Column(Modifier.weight(1.15f)) {
                    Eyebrow(stringResource(if (signed) R.string.tr_extra_short else R.string.tr_weight))
                    NumberStepper(
                        value = set.weightKg,
                        onValueChange = { onChange(set.copy(weightKg = it)) },
                        step = step,
                        allowNegative = signed,
                        min = if (signed) null else 0.0,
                        next = calc?.let { c -> { v: Double -> c.up(v) } },
                        previous = calc?.let { c -> { v: Double -> c.down(v) } },
                        formatter = { WeightFormat.format(it, unit, signed) },
                        parser = unit::toKg,
                        textSize = 22.sp, buttonSize = 42.dp,
                        minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.tr_reps))
                NumberStepper(
                    value = set.reps?.toDouble(),
                    onValueChange = { onChange(set.copy(reps = it?.toInt()?.coerceAtLeast(0))) },
                    step = 1.0,
                    decimals = false,
                    min = 0.0,
                    formatter = { it.toInt().toString() },
                    textSize = 22.sp, buttonSize = 42.dp,
                    minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Gym.Yellow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_done), color = Gym.Yellow)
            }
        }
    }
}

/**
 * Under a warm-up set: the note of your default warm-up, and "Bewaar 50 kg als standaard" when the value in the set
 * differs from your default (#93, variant B). Only a tap makes it the new default.
 */
@Composable
internal fun WarmupSetFooter(item: TrainingItem, set: SetEntity, unit: WeightUnit, onSave: (Double) -> Unit) {
    val measure = item.exercise.warmupMeasure
    val default = item.exercise.defaultWarmup
    val entered = DefaultWarmup.valueOf(measure, set.weightKg, set.reps)
    val note = default?.note
    val offer = entered != null && DefaultWarmup.offersSave(default, measure, entered)
    if (note == null && !offer) return
    Column(Modifier.fillMaxWidth().padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        note?.let { Text(it, color = Gym.Muted, style = MaterialTheme.typography.bodySmall) }
        if (offer && entered != null) {
            Text(
                stringResource(R.string.warmup_save_default, warmupValueText(measure, entered, unit)),
                color = Gym.Yellow, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onSave(entered) }.padding(vertical = 6.dp),
            )
        }
    }
}
