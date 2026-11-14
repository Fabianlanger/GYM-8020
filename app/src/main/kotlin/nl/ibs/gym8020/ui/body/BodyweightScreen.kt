package nl.ibs.gym8020.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.BodyweightLog
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.stats.Point
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.db.BodyweightEntity
import nl.ibs.gym8020.data.toMeasurement
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.NumberStepper
import nl.ibs.gym8020.ui.dashboard.DayRange
import nl.ibs.gym8020.ui.dashboard.LineChart
import nl.ibs.gym8020.ui.theme.Gym
import java.time.LocalDate

/**
 * Body weight log (D-14), several measurements per day (#94): add one (moment "now" unless you change it), a chart
 * with per day a bar from low to high, a dot for the last and small dots for the others, and per day a line with
 * the last measurement and how many there are; open it to change or delete a single measurement.
 */
@Composable
fun BodyweightScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val list by container.bodyweights.observeAll().collectAsStateWithLifecycle(emptyList())
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    var value by remember { mutableStateOf<Double?>(null) }
    var at by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(setOf<Long>()) }
    var editing by remember { mutableStateOf<BodyweightEntity?>(null) }
    val unit = settings.unit
    // Newest day first; within a day in the order taken.
    val byDay = remember(list) { list.groupBy { it.epochDay }.map { (day, l) -> day to l.sortedWith(compareBy({ it.measuredAt }, { it.id })) } }

    editing?.let { m ->
        MeasurementDialog(
            title = stringResource(R.string.body_edit),
            initialKg = m.kg,
            initialAt = m.measuredAt,
            unit = unit,
            onSave = { kg, measuredAt -> editing = null; scope.launch { container.bodyweights.update(m.id, kg, measuredAt) } },
            onDismiss = { editing = null },
            onDelete = { editing = null; scope.launch { container.bodyweights.delete(m.id) } },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.body_title), style = MaterialTheme.typography.headlineMedium)
                DecoCard(corners = true) {
                    Eyebrow(stringResource(R.string.body_add))
                    Spacer(Modifier.height(8.dp))
                    NumberStepper(
                        value = value ?: list.firstOrNull()?.kg,
                        onValueChange = { value = it },
                        step = 0.1,
                        min = 0.0,
                        formatter = { WeightFormat.format(it, unit) },
                        parser = unit::toKg,
                    )
                    MeasuredAtLine(at, onChange = { at = it })
                    Spacer(Modifier.height(8.dp))
                    GoButton(stringResource(R.string.action_save), onClick = {
                        val kg = value ?: list.firstOrNull()?.kg
                        if (kg != null && kg > 0) scope.launch {
                            // Adds a measurement: earlier ones of the same day stay (#94).
                            container.bodyweights.add(kg, at ?: System.currentTimeMillis())
                            value = null
                            at = null
                        }
                    }, height = 52.dp)
                }
                if (list.isEmpty()) Text(stringResource(R.string.body_empty), color = Gym.Muted) else BodyweightChart(list, unit)
            }
        }
        items(byDay, key = { it.first }) { (day, measurements) ->
            DayRow(
                day = day,
                measurements = measurements,
                unit = unit,
                open = day in expanded,
                onToggle = { expanded = if (day in expanded) expanded - day else expanded + day },
                onEdit = { editing = it },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** The chart of #94: per day a thin bar from low to high, a dot for the last measurement, small dots for the others. */
@Composable
private fun BodyweightChart(list: List<BodyweightEntity>, unit: WeightUnit) {
    val days = remember(list) { BodyweightLog.days(list.map { it.toMeasurement() }) }
    fun date(day: Long) = LocalDate.ofEpochDay(day)
    LineChart(
        points = days.map { Point(date(it.epochDay), it.last.kg) },
        format = { WeightFormat.format(it, unit) + " " + unit.name.lowercase() },
        modifier = Modifier.background(Gym.Surface, RoundedCornerShape(16.dp)).padding(12.dp),
        height = 170.dp,
        unit = unit.name.lowercase(),
        extraDots = days.flatMap { d -> d.others.map { Point(date(d.epochDay), it.kg) } },
        ranges = days.filter { it.count > 1 }.map { DayRange(date(it.epochDay), it.lowKg, it.highKg) },
        dotRadius = 3.dp,
    )
}

/** One day: its last measurement and how many there are; opened, each measurement with its time, tap to change. */
@Composable
private fun DayRow(
    day: Long,
    measurements: List<BodyweightEntity>,
    unit: WeightUnit,
    open: Boolean,
    onToggle: () -> Unit,
    onEdit: (BodyweightEntity) -> Unit,
) {
    val res = LocalContext.current.resources
    val last = measurements.last()
    fun weight(kg: Double) = WeightFormat.format(kg, unit) + " " + unit.name.lowercase()
    Column(Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp))) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(LocalDate.ofEpochDay(day).format(measurementDate), color = Gym.Muted)
                Text(
                    res.getQuantityString(R.plurals.body_measurements, measurements.size, measurements.size),
                    style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                )
            }
            Text(weight(last.kg), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Gym.Muted)
        }
        if (open) {
            measurements.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clickable { onEdit(m) }.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(m.measuredAt.toLocalDateTime().format(measurementTime), color = Gym.Muted, modifier = Modifier.weight(1f))
                    Text(weight(m.kg), color = if (m === last) Gym.Cream else Gym.Muted)
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.body_edit), tint = Gym.Muted)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
