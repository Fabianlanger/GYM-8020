package nl.ibs.gym8020.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PerformanceWeight
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.stats.Comparison
import nl.ibs.gym8020.core.stats.ComparisonFormat
import nl.ibs.gym8020.core.stats.ExerciseComparison
import nl.ibs.gym8020.core.stats.Heatmap
import nl.ibs.gym8020.core.stats.Measure
import nl.ibs.gym8020.core.stats.Trend
import nl.ibs.gym8020.core.stats.WorkoutComparison
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.loadStats
import nl.ibs.gym8020.data.toLocalDate
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.WeightText
import nl.ibs.gym8020.ui.deco.FlourishDivider
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

data class RecordLine(val exercise: ExerciseEntity, val weightKg: Double, val reps: Int)

/** One exercise in the summary: strength today next to the last time of that exercise (#66). */
data class ExerciseLine(val exercise: ExerciseEntity, val comparison: ExerciseComparison)

data class Summary(
    val session: SessionEntity,
    val workSets: Int,
    val goalsReached: Int,
    val goalsTotal: Int,
    val records: List<RecordLine>,
    val unit: WeightUnit,
    /** Against the previous session of the same workout; null for the first one. */
    val workout: WorkoutComparison? = null,
    val exercises: List<ExerciseLine> = emptyList(),
    val heatTotal: Map<Muscle, Double> = emptyMap(),
    val heatProgress: Map<Muscle, Double> = emptyMap(),
)

suspend fun buildSummary(c: AppContainer, sessionId: Long): Summary? {
    val session = c.db.sessions().get(sessionId) ?: return null
    val ses = c.db.sessions().exercises(sessionId)
    val sets = c.db.sessions().sets(sessionId).groupBy { it.sessionExerciseId }
    val unit = c.settings.current().unit
    var work = 0
    var reached = 0
    var total = 0
    for (se in ses) {
        if (se.skipped) continue
        val ex = c.db.exercises().get(se.exerciseId) ?: continue
        val list = sets[se.id].orEmpty()
        val done = list.filter { it.role.isWork && it.reps != null }
        work += done.size
        val previousReps = c.training.lastPerformance(ex.id, sessionId)?.sets
        for (s in done) {
            val prog = if (ex.progression == Progression.REPS && (s.weightKg ?: 0.0) == 0.0) Progression.REPS else Progression.WEIGHT
            val prev = previousReps?.filter { it.role == s.role }?.maxOfOrNull { it.reps ?: 0 }
            GoalAdvisor.goalReached(prog, s.goalWeightKg, PerformanceWeight.orNone(s.weightKg, ex.progression, ex.loadType), prev, s.reps)?.let {
                total++
                if (it) reached++
            }
        }
    }
    // Everything below uses the strength model on the statistics' sets (effective weight, skipped exercises left out).
    val stats = loadStats(c.db)
    val bySession = stats.sets.groupBy { it.sessionId }
    val current = bySession[sessionId].orEmpty()
    val currentByExercise = current.groupBy { it.exerciseId }
    val startedAt = session.startedAt
    val earlier = stats.sessions.filter { it.id != sessionId && it.startedAt < startedAt }.sortedByDescending { it.startedAt }
    // Last time per exercise (#66, as "Laatste keer"): the newest earlier training from any workout, skipped ones not counting.
    val earlierSets = earlier.map { bySession[it.id].orEmpty() }
    val previous = currentByExercise.keys.mapNotNull { exId -> Comparison.lastTime(exId, earlierSets)?.let { exId to it } }.toMap()

    val records = mutableListOf<RecordLine>()
    val lines = mutableListOf<ExerciseLine>()
    for (exId in ses.filter { !it.skipped }.map { it.exerciseId }.distinct()) {
        val ex = stats.exercises[exId] ?: continue
        val today = currentByExercise[exId].orEmpty()
        Comparison.exercise(today, previous[exId])?.let { lines += ExerciseLine(ex, it) }
        // Record: best heavy-set e1RM higher than in any other session of this exercise.
        val best = Strength.bestSet(today) ?: continue
        val bestNow = Strength.e1rm(best) ?: continue
        val before = stats.sets.filter { it.exerciseId == exId && it.sessionId != sessionId }
            .groupBy { it.sessionId }.values.mapNotNull { Strength.of(it) }.maxOrNull()
        if (before != null && bestNow > before + 1e-6) {
            records += RecordLine(ex, best.weightKg ?: 0.0, best.reps ?: 0)
        }
    }

    // Per workout (#66): against the previous session of the same workout; skipped exercises count as 0.
    val previousSession = earlier.firstOrNull { it.schemaName == session.schemaName }
    val workout = previousSession?.let { p ->
        Comparison.workout(current, bySession[p.id].orEmpty(), p.startedAt.toLocalDate(), ses.count { it.skipped })
    }

    // Heatmap (D-40..42): volume of this training, and progress vs. the previous time of each exercise.
    val heatTotal = Heatmap.volume(current, stats.muscles)
    val heatProgress = Heatmap.progress(Heatmap.exerciseChange(current, previous), stats.muscles)
    return Summary(session, work, reached, total, records, unit, workout, lines, heatTotal, heatProgress)
}

@Composable
fun SummaryScreen(container: AppContainer, sessionId: Long, onDone: () -> Unit, onEdit: () -> Unit) {
    var summary by remember { mutableStateOf<Summary?>(null) }
    LaunchedEffect(sessionId) { summary = buildSummary(container, sessionId) }
    val s = summary

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.BottomCenter) {
            Sunburst(Modifier.fillMaxSize().alpha(.55f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Eyebrow(s?.session?.schemaName ?: "")
                Text(stringResource(R.string.home_done_for_today), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            }
        }
        FlourishDivider()
        if (s != null) {
            s.workout?.let { WorkoutComparisonCard(s.session.schemaName, it, s.unit) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(formatDuration((s.session.endedAt ?: s.session.startedAt) - s.session.startedAt), stringResource(R.string.sum_duration), Modifier.weight(1f))
                Stat("${s.workSets}", stringResource(R.string.sum_sets), Modifier.weight(1f))
                Stat(if (s.goalsTotal > 0) "${s.goalsReached}/${s.goalsTotal}" else "—", stringResource(R.string.sum_goals), Modifier.weight(1f))
            }
            s.records.forEach { r ->
                DecoCard(corners = true) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Eyebrow(stringResource(R.string.sum_record), color = Gym.Gold)
                        Text(r.exercise.name, style = MaterialTheme.typography.titleLarge)
                        WeightText(r.weightKg, s.unit, size = 40.sp, signed = r.exercise.loadType == LoadType.BODYWEIGHT, suffix = "${s.unit.name.lowercase()} × ${r.reps}")
                    }
                }
            }
            if (s.exercises.isNotEmpty()) {
                Eyebrow(stringResource(R.string.sum_per_exercise))
                s.exercises.forEach { ExerciseComparisonRow(it, s.unit) }
            }
            if (s.heatTotal.isNotEmpty()) {
                HeatmapCard(s.heatTotal, s.heatProgress, s.unit)
            }
        }
        Spacer(Modifier.height(4.dp))
        GoButton(stringResource(R.string.sum_back_home), onClick = onDone)
        LineButton(stringResource(R.string.log_edit_title), onClick = onEdit, icon = Icons.Filled.Edit, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Gym.Surface, RoundedCornerShape(16.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Gym.Muted)
    }
}

/** Short training date, e.g. "1 okt" (as "Laatste keer"). */
private val compareDate = DateTimeFormatter.ofPattern("d MMM")

private fun trendColor(trend: Trend?): Color = when (trend) {
    Trend.UP -> Gym.Positive
    Trend.DOWN -> Gym.Negative
    else -> Gym.Muted
}

/** "(+2,5 kg · +2,4%)"; a change within the margin of noise as "(≈ −0,7 kg · −0,6%)" in a neutral colour. */
private fun changeText(absolute: String, relative: Double?, trend: Trend?): String {
    val parts = listOfNotNull(absolute, relative?.let(ComparisonFormat::percent)).joinToString(" · ")
    return if (trend == Trend.SAME) "(≈ $parts)" else "($parts)"
}

/** Per workout (#66): "Tegenover vorige Full body (1 okt): volume 12.400 kg (+350 kg · +3%) · 18 sets (−2) · 2 oefeningen overgeslagen". */
@Composable
private fun WorkoutComparisonCard(workoutName: String, w: WorkoutComparison, unit: WeightUnit) {
    val res = LocalContext.current.resources
    val u = unit.name.lowercase()
    Column(
        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Eyebrow(stringResource(R.string.sum_vs_previous, workoutName, w.previousDate.format(compareDate)), color = Gym.Gold)
        val volume = stringResource(R.string.sum_volume, ComparisonFormat.volume(w.volume, unit) + " " + u)
        val change = changeText(ComparisonFormat.volume(w.volumeChange.absolute, unit, signed = true) + " " + u, w.volumeChange.relative, w.volumeTrend)
        Text(
            buildAnnotatedString {
                append("$volume ")
                withStyle(SpanStyle(color = trendColor(w.volumeTrend))) { append(change) }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        val details = listOfNotNull(
            res.getQuantityString(R.plurals.sum_sets_change, w.sets, w.sets, ComparisonFormat.count(w.setsChange)),
            if (w.skipped > 0) res.getQuantityString(R.plurals.sum_skipped, w.skipped, w.skipped) else null,
        ).joinToString(" · ")
        Text(details, style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
    }
}

/** Per exercise (#66): "Back squat · e1RM 104 kg (+2,5 kg · +2,4%)", or "16 reps (+2)", plus a light-set line when it changed. */
@Composable
private fun ExerciseComparisonRow(line: ExerciseLine, unit: WeightUnit) {
    val c = line.comparison
    val signed = line.exercise.loadType == LoadType.BODYWEIGHT
    val now = c.now
    val value = when {
        now == null -> "—"
        c.measure == Measure.REPS -> stringResource(R.string.sum_reps, now.roundToInt())
        else -> stringResource(R.string.sum_e1rm, ComparisonFormat.weight(now, unit, signed = false))
    }
    val change = c.change?.let { ch ->
        if (c.measure == Measure.REPS) "(${ComparisonFormat.count(ch.absolute.roundToInt())})"
        else changeText(ComparisonFormat.weight(ch.absolute, unit), ch.relative, c.trend)
    }
    Column(
        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(line.exercise.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                val sub = change ?: if (c.firstTime) stringResource(R.string.sum_first_time) else null
                if (sub != null) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = trendColor(c.trend))
                }
            }
        }
        c.light?.let { l ->
            val parts = listOfNotNull(
                if (abs(l.weightChangeKg) > 1e-9) ComparisonFormat.weight(l.weightChangeKg, unit) else null,
                if (l.repsChange != 0) stringResource(R.string.sum_reps_change, ComparisonFormat.count(l.repsChange)) else null,
            ).joinToString(" · ")
            val color = when {
                l.weightChangeKg >= -1e-9 && l.repsChange >= 0 -> Gym.Positive
                l.weightChangeKg <= 1e-9 && l.repsChange <= 0 -> Gym.Negative
                else -> Gym.Muted
            }
            Text(
                "${stringResource(R.string.role_light_short)} ${WeightFormat.format(l.weightKg, unit, signed)} × ${l.reps} ($parts)",
                style = MaterialTheme.typography.bodySmall, color = color,
            )
        }
    }
}
