package nl.ibs.gym8020.ui.body

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.theme.Gym
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date as the app shows it elsewhere (log, session edit). */
internal val measurementDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Time of day, 24 hours, as on the session edit screen. */
internal val measurementTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun Long.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

private fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * The moment of a measurement, small under the weight field (#94): [measuredAt] null is "now" (taken when you save).
 * Tap the date or the time to change it; the seconds of the moment stay, so only what you pick changes.
 */
@Composable
fun MeasuredAtLine(measuredAt: Long?, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shown = (measuredAt ?: System.currentTimeMillis()).toLocalDateTime()
    val small = MaterialTheme.typography.bodySmall
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (measuredAt == null) Text(stringResource(R.string.body_at_now), style = small, color = Gym.Muted)
        Text(
            shown.format(measurementDate),
            style = small, color = Gym.Yellow, textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onChange(shown.withYear(y).withMonth(m + 1).withDayOfMonth(d).toEpochMillis()) },
                    shown.year, shown.monthValue - 1, shown.dayOfMonth,
                ).show()
            }.padding(vertical = 6.dp),
        )
        Text(
            shown.format(measurementTime),
            style = small, color = Gym.Yellow, textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                TimePickerDialog(context, { _, h, min -> onChange(shown.withHour(h).withMinute(min).toEpochMillis()) }, shown.hour, shown.minute, true).show()
            }.padding(vertical = 6.dp),
        )
    }
}

/**
 * Enter or change one body weight measurement (#94): the weight, with its moment small underneath ([initialAt] null
 * is "now"). With [onDelete] the measurement can also be deleted.
 */
@Composable
fun MeasurementDialog(
    title: String,
    initialKg: Double?,
    initialAt: Long?,
    unit: WeightUnit,
    onSave: (kg: Double, measuredAt: Long) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var value by remember { mutableStateOf(initialKg) }
    var at by remember { mutableStateOf(initialAt) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NumberStepper(
                    value = value, onValueChange = { value = it }, step = 0.1, min = 0.0, startFrom = 75.0,
                    formatter = { WeightFormat.format(it, unit) }, parser = unit::toKg,
                    suffix = unit.name.lowercase(), textSize = 26.sp,
                )
                MeasuredAtLine(at, onChange = { at = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { value?.takeIf { it > 0 }?.let { onSave(it, at ?: System.currentTimeMillis()) } },
                enabled = (value ?: 0.0) > 0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = Gym.Negative) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
