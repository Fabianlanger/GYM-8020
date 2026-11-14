package nl.ibs.gym8020.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WarmupMeasure
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.theme.Gym

/**
 * "Standaard-warming-up" in Oefening bewerken (#93, variant A): the value, in kg, as a plus-or-minus weight or in reps
 * to match the exercise ([measure]), and a short note. [value] is kg (signed) or reps; null = none.
 */
@Composable
internal fun DefaultWarmupBlock(
    measure: WarmupMeasure,
    value: Double?,
    note: String,
    unit: WeightUnit,
    calc: WeightCalculator?,
    onValue: (Double?) -> Unit,
    onNote: (String) -> Unit,
) {
    val signed = measure == WarmupMeasure.SIGNED_WEIGHT
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(stringResource(R.string.warmup_default_title))
        Text(stringResource(R.string.warmup_default_help), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        Text(
            stringResource(
                when (measure) {
                    WarmupMeasure.WEIGHT -> R.string.tr_weight
                    WarmupMeasure.SIGNED_WEIGHT -> R.string.tr_extra_short
                    WarmupMeasure.REPS -> R.string.tr_reps
                },
            ),
            color = Gym.Muted,
        )
        if (measure == WarmupMeasure.REPS) {
            NumberStepper(
                value, onValue, step = 1.0, decimals = false, min = 0.0,
                formatter = { it.toInt().toString() }, buttonSize = 44.dp, textSize = 22.sp,
                minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
            )
        } else {
            NumberStepper(
                value, onValue, step = 2.5,
                allowNegative = signed,
                min = if (signed) null else 0.0,
                next = calc?.let { c -> { v: Double -> c.up(v) } },
                previous = calc?.let { c -> { v: Double -> c.down(v) } },
                formatter = { WeightFormat.format(it, unit, signed) },
                parser = unit::toKg,
                suffix = unit.name.lowercase(),
                buttonSize = 44.dp, textSize = 22.sp,
                minusLabel = stringResource(R.string.action_less), plusLabel = stringResource(R.string.action_more),
            )
        }
        OutlinedTextField(
            note, onNote,
            label = { Text(stringResource(R.string.warmup_default_note)) },
            placeholder = { Text(stringResource(R.string.warmup_default_note_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
