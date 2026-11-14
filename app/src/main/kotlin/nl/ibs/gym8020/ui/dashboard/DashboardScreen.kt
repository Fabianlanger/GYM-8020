package nl.ibs.gym8020.ui.dashboard

import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import nl.ibs.gym8020.ui.components.PlainCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.ui.log.SessionRow
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleGroup
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.stats.Dashboard
import nl.ibs.gym8020.core.stats.Point
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.exercises.ExerciseIcon
import nl.ibs.gym8020.ui.exercises.groupName
import nl.ibs.gym8020.ui.exercises.muscleName
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins
import nl.ibs.gym8020.ui.training.formatDuration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Level { EXERCISE, MUSCLE, GROUP }

private fun pct(v: Double) = "${(v * 100).toInt()} %"
private fun sign(v: Double) = (if (v >= 0) "+" else "−") + "${(kotlin.math.abs(v) * 100).toInt()} %"

/** Dashboard (v0.3, D-38/39, D-66/67): progress per exercise, muscle and muscle group, plus body weight, frequency, time and goals. */
@Composable
fun DashboardScreen(container: AppContainer, onOpenBodyweight: () -> Unit, onOpenLog: () -> Unit, onOpenSession: (Long) -> Unit) {
    val vm: DashboardViewModel = viewModel { DashboardViewModel(container) }
    val d by vm.data.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }
    var level by rememberSaveable { mutableStateOf(Level.EXERCISE) }
    val unit = d.unit
    fun kg(v: Double) = WeightFormat.format(v, unit) + " " + unit.name.lowercase()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.headlineMedium)

        // Headline numbers.
        val thisWeek = d.trainingsPerWeek.lastOrNull()?.value?.toInt() ?: 0
        val last4 = d.trainingsPerWeek.takeLast(4).sumOf { it.value }.toInt()
        val avgDuration = d.durations.map { it.second }.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.toLong()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(if (d.targetPerWeek != null) "$thisWeek/${d.targetPerWeek}" else "$thisWeek", stringResource(R.string.dash_this_week), Modifier.weight(1f))
            StatTile("$last4", stringResource(R.string.dash_last_4_weeks), Modifier.weight(1f))
            StatTile(avgDuration?.let(::formatDuration) ?: "—", stringResource(R.string.dash_avg_duration), Modifier.weight(1f))
        }

        // Body weight at the top (V-12): current value and a tap to log; the chart stays lower on the page.
        PlainCard(onClick = onOpenBodyweight) {
            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = Gym.Yellow)
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.body_title))
                val last = d.bodyweight.lastOrNull()
                Text(last?.let { kg(it.value) } ?: stringResource(R.string.home_weight_log), style = MaterialTheme.typography.titleMedium)
                Dashboard.change(d.bodyweight)?.let { Text(stringResource(R.string.dash_change, sign(it)), style = MaterialTheme.typography.bodySmall, color = Gym.Muted) }
            }
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.body_manage), tint = Gym.Yellow)
        }

        // Latest trainings, to look back and correct (log).
        val recent by container.db.sessions().observeFinished().collectAsStateWithLifecycle(emptyList())
        val counts by container.db.sessions().observeWorkSetCounts().collectAsStateWithLifecycle(emptyList())
        if (recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(stringResource(R.string.log_title), Modifier.weight(1f))
                TextButton(onClick = onOpenLog) { Text(stringResource(R.string.log_all), color = Gym.Yellow) }
            }
            val setCount = counts.associate { it.sessionId to it.workSets }
            recent.take(3).forEach { s -> SessionRow(s, setCount[s.id] ?: 0) { onOpenSession(s.id) } }
        }

        if (d.loaded && d.exercises.isEmpty()) {
            DecoCard(corners = true) { Text(stringResource(R.string.dash_empty), color = Gym.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }

        // Progress on three levels.
        DecoDivider()
        Eyebrow(stringResource(R.string.dash_progress))
        Row(Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(14.dp)).padding(4.dp)) {
            Level.entries.forEach { l ->
                val selected = l == level
                Text(
                    stringResource(
                        when (l) {
                            Level.EXERCISE -> R.string.dash_level_exercise
                            Level.MUSCLE -> R.string.dash_level_muscle
                            Level.GROUP -> R.string.dash_level_group
                        },
                    ),
                    modifier = Modifier.weight(1f).background(if (selected) Gym.Yellow else Gym.Surface, RoundedCornerShape(10.dp)).clickable { level = l }.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center, color = if (selected) Gym.OnYellow else Gym.Cream, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        when (level) {
            Level.EXERCISE -> ExerciseSection(d, ::kg)
            Level.MUSCLE -> MuscleSection(d, ::kg)
            Level.GROUP -> GroupSection(d, ::kg)
        }

        // Frequency against the week plan target.
        DecoDivider()
        Eyebrow(stringResource(R.string.dash_frequency))
        val weekFmt = DateTimeFormatter.ofPattern("d/M")
        BarChart(
            d.trainingsPerWeek.map { it.weekStart.format(weekFmt) to it.value },
            format = { "${it.toInt()}" },
            reference = d.targetPerWeek?.toDouble(),
        )

        // Goals reached.
        if (d.goalRate.isNotEmpty()) {
            Eyebrow(stringResource(R.string.dash_goals))
            LineChart(d.goalRate.map { Point(it.weekStart, it.value) }, format = ::pct, fixedMin = 0.0, fixedMax = 1.0, tickFormat = ::pct)
        }

        // Time.
        if (d.durations.isNotEmpty()) {
            DecoDivider()
            Eyebrow(stringResource(R.string.dash_duration))
            val dayFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            BarChart(d.durations.map { it.first.format(dayFmt) to it.second / 60_000.0 }, format = { "${it.toInt()} min" })
        }
        if (d.timePerPosition.size >= 2) {
            Eyebrow(stringResource(R.string.dash_time_per_position))
            BarChart(d.timePerPosition.map { "${it.first}" to it.second / 1000.0 }, format = { formatDuration((it * 1000).toLong()) }, height = 120.dp)
            Text(stringResource(R.string.dash_time_per_position_help), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        }

        // Body weight.
        DecoDivider()
        Eyebrow(stringResource(R.string.body_title))
        if (d.bodyweight.isNotEmpty()) {
            LineChart(
                d.bodyweight, format = ::kg, trend = d.bodyweightTrend.takeIf { d.bodyweight.size >= 3 }, unit = unit.name.lowercase(),
                extraDots = d.bodyweightOthers, ranges = d.bodyweightRanges, dotRadius = 3.dp,
            )
            Dashboard.change(d.bodyweight)?.let { Text(stringResource(R.string.dash_change, sign(it)), style = MaterialTheme.typography.bodySmall, color = Gym.Muted) }
        } else {
            Text(stringResource(R.string.body_empty), color = Gym.Muted)
        }
        LineButton(stringResource(R.string.body_manage), onClick = onOpenBodyweight, icon = Icons.Filled.MonitorWeight, modifier = Modifier.fillMaxWidth())

        // Records.
        if (d.records.isNotEmpty()) {
            DecoDivider()
            Eyebrow(stringResource(R.string.dash_records))
            val byId = d.exercises.associateBy { it.id }
            val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            d.records.values.sortedByDescending { it.date }.forEach { r ->
                val ex = byId[r.exerciseId] ?: return@forEach
                Row(
                    Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(14.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExerciseIcon(ex, size = 36)
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.bodyMedium)
                        Text(r.date.format(fmt), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                    }
                    Text("${r.weightKg?.let { WeightFormat.format(it, unit, ex.loadType == nl.ibs.gym8020.core.model.LoadType.BODYWEIGHT) } ?: "—"} × ${r.reps}", style = MaterialTheme.typography.titleMedium, color = Gym.Yellow)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Gym.Surface, RoundedCornerShape(16.dp)).padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, maxLines = 1)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Gym.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ExerciseSection(d: DashboardData, kg: (Double) -> String) {
    if (d.exercises.isEmpty()) return
    var selectedId by rememberSaveable { mutableStateOf(d.exercises.first().id) }
    val ex = d.exercises.firstOrNull { it.id == selectedId } ?: d.exercises.first()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        d.exercises.forEach { e ->
            val sel = e.id == ex.id
            Row(
                Modifier.background(if (sel) Gym.SurfaceHigh else Gym.Surface, RoundedCornerShape(14.dp)).clickable { selectedId = e.id }.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ExerciseIcon(e, size = 30)
                Text(e.name, style = MaterialTheme.typography.bodySmall, color = if (sel) Gym.Yellow else Gym.Cream, modifier = Modifier.padding(end = 6.dp))
            }
        }
    }
    val p = d.progress[ex.id] ?: return
    val repsMode = ex.progression == Progression.REPS && p.e1rm.isEmpty()
    val series = if (repsMode) p.reps else p.e1rm
    Text(ex.name, style = MaterialTheme.typography.titleLarge)
    Text(stringResource(if (repsMode) R.string.dash_reps_over_time else R.string.dash_e1rm_over_time), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
    if (series.isNotEmpty()) LineChart(series, format = if (repsMode) ({ "${it.toInt()}" }) else kg, unit = if (repsMode) "reps" else d.unit.name.lowercase())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(Dashboard.change(series)?.let(::sign) ?: "—", stringResource(R.string.dash_change_label), Modifier.weight(1f))
        StatTile(d.records[ex.id]?.let { "${WeightFormat.format(it.weightKg ?: 0.0, d.unit)}×${it.reps}" } ?: "—", stringResource(R.string.dash_record), Modifier.weight(1f))
        StatTile(d.goalRateByExercise[ex.id]?.let(::pct) ?: "—", stringResource(R.string.dash_goals_short), Modifier.weight(1f))
    }
}

@Composable
private fun MuscleSection(d: DashboardData, kg: (Double) -> String) {
    var selected by rememberSaveable { mutableStateOf<Muscle?>(null) }
    val avg = d.setsPerMuscle.mapValues { (_, l) -> l.takeLast(4).map { it.value }.average().takeIf { !it.isNaN() } ?: 0.0 }
    val max = avg.values.maxOrNull() ?: 0.0
    nl.ibs.gym8020.ui.training.HeatmapCard(
        total = avg.filterValues { it > 0 }, unit = d.unit,
        title = stringResource(R.string.heat_sets_title), totalFormat = { "%.1f".format(it).replace('.', ',') + " " + "sets" },
    )
    Text(stringResource(R.string.dash_sets_per_week_help), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
    MuscleGroup.entries.forEach { g ->
        val ms = Muscle.entries.filter { it.group == g }
        Text(groupName(g), style = MaterialTheme.typography.titleSmall, color = Gym.Gold)
        ms.forEach { m ->
            HBarRow(muscleName(m), avg[m] ?: 0.0, max, "%.1f".format(avg[m] ?: 0.0).replace('.', ','), selected == m, onClick = { selected = if (selected == m) null else m })
            if (selected == m) Detail(d.setsPerMuscle[m].orEmpty(), d.volumePerMuscle[m].orEmpty(), d.goalRateByMuscle[m], kg)
        }
    }
}

@Composable
private fun GroupSection(d: DashboardData, kg: (Double) -> String) {
    var selected by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    val avg = d.setsPerGroup.mapValues { (_, l) -> l.takeLast(4).map { it.value }.average().takeIf { !it.isNaN() } ?: 0.0 }
    val max = avg.values.maxOrNull() ?: 0.0
    Text(stringResource(R.string.dash_sets_per_week_help), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
    MuscleGroup.entries.forEach { g ->
        HBarRow(groupName(g), avg[g] ?: 0.0, max, "%.1f".format(avg[g] ?: 0.0).replace('.', ','), selected == g, onClick = { selected = if (selected == g) null else g })
        if (selected == g) Detail(d.setsPerGroup[g].orEmpty(), d.volumePerGroup[g].orEmpty(), d.goalRateByGroup[g], kg)
    }
    Spacer(Modifier.height(4.dp))
    Eyebrow(stringResource(R.string.dash_balance))
    BalanceBar(stringResource(R.string.dash_push), stringResource(R.string.dash_pull), d.pushPull.ratio)
    BalanceBar(stringResource(R.string.dash_upper), stringResource(R.string.dash_lower), d.upperLower.ratio)
}

@Composable
private fun Detail(sets: List<nl.ibs.gym8020.core.stats.WeekValue>, volume: List<nl.ibs.gym8020.core.stats.WeekValue>, goalRate: Double?, kg: (Double) -> String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.dash_sets_per_week), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        val fmt = DateTimeFormatter.ofPattern("d/M")
        BarChart(sets.map { it.weekStart.format(fmt) to it.value }, format = { "%.1f".format(it).replace('.', ',') }, height = 110.dp)
        Text(stringResource(R.string.dash_volume_per_week), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        BarChart(volume.map { it.weekStart.format(fmt) to it.value }, format = kg, height = 110.dp)
        goalRate?.let { Text(stringResource(R.string.dash_goals_rate, pct(it)), style = MaterialTheme.typography.bodySmall) }
    }
}
