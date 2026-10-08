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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PerformanceWeight
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.WeightText
import nl.ibs.gym8020.ui.deco.FlourishDivider
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins

data class RecordLine(val exercise: ExerciseEntity, val weightKg: Double, val reps: Int)
data class ExerciseTime(val name: String, val trainMs: Long?, val switchMs: Long?)

data class Summary(
    val session: SessionEntity,
    val workSets: Int,
    val goalsReached: Int,
    val goalsTotal: Int,
    val records: List<RecordLine>,
    val times: List<ExerciseTime>,
    val unit: WeightUnit,
    val heatTotal: Map<nl.ibs.gym8020.core.model.Muscle, Double> = emptyMap(),
    val heatProgress: Map<nl.ibs.gym8020.core.model.Muscle, Double> = emptyMap(),
)

suspend fun buildSummary(c: AppContainer, sessionId: Long): Summary? {
    val session = c.db.sessions().get(sessionId) ?: return null
    val ses = c.db.sessions().exercises(sessionId)
    val sets = c.db.sessions().sets(sessionId).groupBy { it.sessionExerciseId }
    val unit = c.settings.current().unit
    var work = 0
    var reached = 0
    var total = 0
    val records = mutableListOf<RecordLine>()
    val times = mutableListOf<ExerciseTime>()
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
        // Record: best heavy-set e1RM (effective weight) higher than in any earlier session.
        val best = done.filter { it.role == SetRole.HEAVY }
            .mapNotNull { s -> c.training.effective(ex, s)?.let { eff -> Triple(s, eff, Strength.e1rm(eff, s.reps!!)) } }
            .maxByOrNull { it.third }
        val before = c.training.bestE1rm(ex, sessionId)
        if (best != null && before != null && best.third > before + 1e-6) {
            records += RecordLine(ex, best.first.weightKg ?: 0.0, best.first.reps ?: 0)
        }
        times += ExerciseTime(
            ex.name,
            if (se.goAt != null && se.firstInputAt != null) se.firstInputAt - se.goAt else null,
            if (se.shownAt != null && se.goAt != null) se.goAt - se.shownAt else null,
        )
    }
    // Heatmap (D-40..42): volume of this training, and progress vs. the previous time of each exercise.
    val stats = nl.ibs.gym8020.data.loadStats(c.db)
    val current = stats.sets.filter { it.sessionId == sessionId }
    val startedAt = session.startedAt
    val earlier = stats.sessions.filter { it.startedAt < startedAt }.sortedByDescending { it.startedAt }
    val previous = current.map { it.exerciseId }.distinct().mapNotNull { exId ->
        earlier.firstNotNullOfOrNull { prev -> stats.sets.filter { it.sessionId == prev.id && it.exerciseId == exId }.takeIf { it.isNotEmpty() } }
            ?.let { exId to it }
    }.toMap()
    val heatTotal = nl.ibs.gym8020.core.stats.Heatmap.volume(current, stats.muscles)
    val heatProgress = nl.ibs.gym8020.core.stats.Heatmap.progress(nl.ibs.gym8020.core.stats.Heatmap.exerciseChange(current, previous), stats.muscles)
    return Summary(session, work, reached, total, records, times, unit, heatTotal, heatProgress)
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
                        WeightText(r.weightKg, s.unit, size = 40.sp, signed = r.exercise.loadType == nl.ibs.gym8020.core.model.LoadType.BODYWEIGHT, suffix = "${s.unit.name.lowercase()} × ${r.reps}")
                    }
                }
            }
            if (s.heatTotal.isNotEmpty()) {
                HeatmapCard(s.heatTotal, s.heatProgress, s.unit)
            }
            if (s.times.isNotEmpty()) {
                Eyebrow(stringResource(R.string.sum_times))
                s.times.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.sum_exercise_time, t.trainMs?.let(::formatDuration) ?: "—", t.switchMs?.let(::formatDuration) ?: "—"),
                            style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
                        )
                    }
                }
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
