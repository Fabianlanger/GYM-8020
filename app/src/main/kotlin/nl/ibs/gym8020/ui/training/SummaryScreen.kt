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
import nl.ibs.gym8020.core.logic.RepsRecord
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.stats.Comparison
import nl.ibs.gym8020.core.stats.ComparisonFormat
import nl.ibs.gym8020.core.stats.ExerciseComparison
import nl.ibs.gym8020.core.stats.Measure
import nl.ibs.gym8020.core.stats.MuscleComparison
import nl.ibs.gym8020.core.stats.MuscleMap
import nl.ibs.gym8020.core.stats.SummaryHistory
import nl.ibs.gym8020.core.stats.TrainingRef
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
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A record in the summary: on weight the heavy set with the best e1RM ([weightKg] × [reps]); with reps progression
 * the record on reps ([onReps], #73).
 */
data class RecordLine(val exercise: ExerciseEntity, val weightKg: Double, val reps: Int, val onReps: RepsRecord? = null)

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
    /** Muscle map (#67); null when nothing was trained. */
    val muscles: MuscleMap? = null,
    /** Exercise names for the muscle map. */
    val exerciseById: Map<Long, ExerciseEntity> = emptyMap(),
)

suspend fun buildSummary(c: AppContainer, sessionId: Long): Summary? {
    val session = c.db.sessions().get(sessionId) ?: return null
    val ses = c.db.sessions().exercises(sessionId)
    val sets = c.db.sessions().sets(sessionId).groupBy { it.sessionExerciseId }
    val unit = c.settings.current().unit
    // Everything is measured against the trainings before this one only (#100): right after finishing that is all
    // history, for an older training from the log it is what you saw then, with the records as they held then.
    val stats = loadStats(c.db)
    val history = SummaryHistory.of(
        sessionId, stats.sessions.map { TrainingRef(it.id, it.startedAt, it.schemaName) }, stats.sets,
    )
    var work = 0
    var reached = 0
    var total = 0
    for (se in ses) {
        if (se.skipped) continue
        val ex = c.db.exercises().get(se.exerciseId) ?: continue
        val list = sets[se.id].orEmpty()
        val done = list.filter { it.role.isWork && it.reps != null }
        work += done.size
        // The training before this one, not a later one (#100: an older training opened from the log).
        val previousReps = history?.lastSets(ex.id)
        // With session targets in reps, an extra set without one does not count (#92).
        val withRepsTargets = list.any { it.goalReps != null }
        for (s in done) {
            val prog = if (ex.progression == Progression.REPS && (s.weightKg ?: 0.0) == 0.0) Progression.REPS else Progression.WEIGHT
            // Reps: your session target reps (#69) when there is one, else (older trainings) the best reps of that role last time.
            val prev = s.goalReps ?: if (withRepsTargets) null else previousReps?.filter { it.role == s.role }?.maxOfOrNull { it.reps ?: 0 }
            GoalAdvisor.goalReached(prog, s.goalWeightKg, PerformanceWeight.orNone(s.weightKg, ex.progression, ex.loadType), prev, s.reps)?.let {
                total++
                if (it) reached++
            }
        }
    }
    // Everything below uses the strength model on the statistics' sets (effective weight, skipped exercises left out).
    if (history == null) return Summary(session, work, reached, total, emptyList(), unit, exerciseById = stats.exercises)
    val current = history.today
    // Last time per exercise (#66, as "Laatste keer"): the newest earlier training from any workout, skipped ones not counting.
    val previous = current.map { it.exerciseId }.distinct().mapNotNull { exId -> history.lastTime(exId)?.let { exId to it } }.toMap()

    val records = mutableListOf<RecordLine>()
    val lines = mutableListOf<ExerciseLine>()
    for (exId in ses.filter { !it.skipped }.map { it.exerciseId }.distinct()) {
        val ex = stats.exercises[exId] ?: continue
        val today = history.today(exId)
        Comparison.exercise(today, previous[exId])?.let { lines += ExerciseLine(ex, it) }
        // With reps progression (#73): the most reps in a work set, at a load at least as heavy as the record's.
        if (ex.progression == Progression.REPS) {
            history.repsRecord(exId)?.let { records += RecordLine(ex, it.effectiveKg ?: it.loadKg, it.reps, onReps = it) }
            continue
        }
        // Record: best heavy-set e1RM higher than in any earlier session of this exercise.
        history.weightRecord(exId)?.let { best -> records += RecordLine(ex, best.weightKg ?: 0.0, best.reps ?: 0) }
    }

    // Per workout (#66): against the previous session of the same workout; skipped exercises count as 0.
    val previousSession = history.previousOfWorkout
    val workout = previousSession?.let { p ->
        Comparison.workout(current, history.setsOf(p.id), p.startedAt.toLocalDate(), ses.count { it.skipped })
    }

    // Muscle map (#67): strength of what you did vs. the last time, volume vs. the previous session of this
    // workout, and the muscles you would only have trained with skipped exercises.
    val muscles = MuscleComparison.of(
        current, ses.map { it.exerciseId }, previous, previousSession?.let { history.setsOf(it.id) }, stats.muscles,
    ).takeIf { it.breakdown.isNotEmpty() }
    return Summary(session, work, reached, total, records, unit, workout, lines, muscles, stats.exercises)
}

/**
 * The summary of a training: right after finishing it, or ([review], #100) an older training opened from the log.
 * Building it only reads: no backup, no goals saved, nothing written (finishing did that once, in the repository).
 * In [review] the heading is the training's date instead of "Done for today", and [onDone] goes back to the log.
 */
@Composable
fun SummaryScreen(container: AppContainer, sessionId: Long, onDone: () -> Unit, onEdit: () -> Unit, review: Boolean = false) {
    var summary by remember { mutableStateOf<Summary?>(null) }
    // Rebuilt each time the screen comes back (after editing too): it is a fresh composition then.
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
                val title = if (!review) stringResource(R.string.home_done_for_today)
                else s?.session?.startedAt?.toLocalDate()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) ?: "" // e.g. "12 October 2026"
                Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
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
                        val onReps = r.onReps
                        if (onReps != null) RepsRecordText(onReps, s.unit)
                        else WeightText(r.weightKg, s.unit, size = 40.sp, signed = r.exercise.loadType == LoadType.BODYWEIGHT, suffix = "${s.unit.name.lowercase()} × ${r.reps}")
                    }
                }
            }
            if (s.exercises.isNotEmpty()) {
                Eyebrow(stringResource(R.string.sum_per_exercise))
                s.exercises.forEach { ExerciseComparisonRow(it, s.unit) }
            }
            s.muscles?.let { MuscleMapCard(it, s.exerciseById, s.unit) }
        }
        Spacer(Modifier.height(4.dp))
        GoButton(stringResource(if (review) R.string.sum_back_log else R.string.sum_back_home), onClick = onDone)
        LineButton(stringResource(R.string.log_edit_title), onClick = onEdit, icon = Icons.Filled.Edit, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * A record on reps (#73): big the reps ("22 reps"), small the weight you moved ("76 kg", never "bodyweight") and the
 * improvement ("+3 reps").
 */
@Composable
private fun RepsRecordText(r: RepsRecord, unit: WeightUnit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "${r.reps}", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Gym.Yellow, lineHeight = 40.sp,
        )
        Text(
            " " + stringResource(R.string.tr_reps_suffix), fontFamily = Poppins, fontSize = 14.sp, color = Gym.Cream,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
    val detail = listOfNotNull(
        r.effectiveKg?.let { WeightFormat.format(it, unit) + " " + unit.name.lowercase() },
        stringResource(R.string.sum_reps_change, ComparisonFormat.count(r.improvement)),
    ).joinToString(" · ")
    Text(detail, style = MaterialTheme.typography.bodySmall, color = Gym.Positive)
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

internal fun trendColor(trend: Trend?): Color = when (trend) {
    Trend.UP -> Gym.Positive
    Trend.DOWN -> Gym.Negative
    else -> Gym.Muted
}

/** "(+2,5 kg · +2,4%)"; a change within the margin of noise as "(≈ −0,7 kg · −0,6%)" in a neutral colour. */
internal fun changeText(absolute: String, relative: Double?, trend: Trend?): String {
    val parts = listOfNotNull(absolute, relative?.let(ComparisonFormat::percent)).joinToString(" · ")
    return if (trend == Trend.SAME) "(≈ $parts)" else "($parts)"
}

/** One exercise's change: "(+2,5 kg · +2,4%)", or "(+2)" for reps; null without a comparison. */
internal fun exerciseChangeText(c: ExerciseComparison, unit: WeightUnit): String? = c.change?.let { ch ->
    if (c.measure == Measure.REPS) "(${ComparisonFormat.count(ch.absolute.roundToInt())})"
    else changeText(ComparisonFormat.weight(ch.absolute, unit), ch.relative, c.trend)
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
    val change = exerciseChangeText(c, unit)
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
