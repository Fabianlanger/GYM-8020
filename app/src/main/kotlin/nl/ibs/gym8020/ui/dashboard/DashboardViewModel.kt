package nl.ibs.gym8020.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.core.logic.BodyweightLog
import nl.ibs.gym8020.core.logic.EffectiveWeight
import nl.ibs.gym8020.core.logic.PerformanceWeight
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleGroup
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.stats.Balance
import nl.ibs.gym8020.core.stats.Dashboard
import nl.ibs.gym8020.core.stats.ExerciseProgress
import nl.ibs.gym8020.core.stats.ExerciseTiming
import nl.ibs.gym8020.core.stats.Point
import nl.ibs.gym8020.core.stats.Record
import nl.ibs.gym8020.core.stats.SessionRecord
import nl.ibs.gym8020.core.stats.SetRecord
import nl.ibs.gym8020.core.stats.WeekValue
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.toLocalDate
import nl.ibs.gym8020.data.toMeasurement
import java.time.LocalDate

data class DashboardData(
    val unit: WeightUnit = WeightUnit.KG,
    val targetPerWeek: Int? = null,
    val trainingsPerWeek: List<WeekValue> = emptyList(),
    val durations: List<Pair<LocalDate, Long>> = emptyList(),
    val timePerPosition: List<Pair<Int, Double>> = emptyList(),
    val goalRate: List<WeekValue> = emptyList(),
    val bodyweight: List<Point> = emptyList(),
    val bodyweightTrend: List<Point> = emptyList(),
    /** Days with several measurements: low to high (#94). */
    val bodyweightRanges: List<DayRange> = emptyList(),
    /** The measurements before the last of their day, as small dots (#94). */
    val bodyweightOthers: List<Point> = emptyList(),
    /** Exercises with history, most recently trained first. */
    val exercises: List<ExerciseEntity> = emptyList(),
    val progress: Map<Long, ExerciseProgress> = emptyMap(),
    val records: Map<Long, Record> = emptyMap(),
    val goalRateByExercise: Map<Long, Double> = emptyMap(),
    val setsPerMuscle: Map<Muscle, List<WeekValue>> = emptyMap(),
    val volumePerMuscle: Map<Muscle, List<WeekValue>> = emptyMap(),
    val setsPerGroup: Map<MuscleGroup, List<WeekValue>> = emptyMap(),
    val volumePerGroup: Map<MuscleGroup, List<WeekValue>> = emptyMap(),
    val goalRateByMuscle: Map<Muscle, Double> = emptyMap(),
    val goalRateByGroup: Map<MuscleGroup, Double> = emptyMap(),
    val pushPull: Balance = Balance(0.0, 0.0),
    val upperLower: Balance = Balance(0.0, 0.0),
    val loaded: Boolean = false,
)

const val DASHBOARD_WEEKS = 12

class DashboardViewModel(private val c: AppContainer) : ViewModel() {
    private val _data = MutableStateFlow(DashboardData())
    val data: StateFlow<DashboardData> = _data

    fun refresh() {
        viewModelScope.launch { _data.value = withContext(Dispatchers.Default) { load() } }
    }

    private suspend fun load(): DashboardData {
        val db = c.db
        val today = LocalDate.now()
        val settings = c.settings.current()
        val sessions = db.sessions().all().filter { it.endedAt != null }
        val sessionById = sessions.associateBy { it.id }
        val ses = db.sessions().allExercises().filter { it.sessionId in sessionById && !it.skipped }
        val seById = ses.associateBy { it.id }
        val exercises = db.exercises().all().associateBy { it.id }
        val muscles: Map<Long, List<MuscleShare>> = db.exercises().allMuscles().groupBy { it.exerciseId }
            .mapValues { (_, l) -> l.map { MuscleShare(it.muscle, it.percent) } }

        val sets = db.sessions().allSets().mapNotNull { s ->
            val se = seById[s.sessionExerciseId] ?: return@mapNotNull null
            val session = sessionById[se.sessionId] ?: return@mapNotNull null
            val ex = exercises[se.exerciseId] ?: return@mapNotNull null
            val w = PerformanceWeight.orNone(s.weightKg, ex.progression, ex.loadType)
            SetRecord(
                sessionId = session.id,
                date = session.startedAt.toLocalDate(),
                exerciseId = ex.id,
                role = s.role,
                weightKg = s.weightKg,
                reps = s.reps,
                effectiveKg = w?.let { EffectiveWeight.of(ex.loadType, it, s.bodyweightKg, ex.bodyweightFactor) },
                goalWeightKg = s.goalWeightKg,
                progression = ex.progression,
            )
        }
        val sessionRecords = sessions.map { SessionRecord(it.id, it.startedAt.toLocalDate(), it.endedAt!! - it.startedAt) }
        val timings = ses.map { ExerciseTiming(it.sessionId, it.position + 1, if (it.goAt != null && it.firstInputAt != null) it.firstInputAt - it.goAt else null) }

        // Per day the measurement that counts: the last one (V-14); the others and the spread around it (#94).
        val bwDays = BodyweightLog.days(db.bodyweights().all().map { it.toMeasurement() })
        val bw = bwDays.map { Point(LocalDate.ofEpochDay(it.epochDay), it.last.kg) }
        val goalResults = Dashboard.goalResults(sets)
        val setsPerMuscle = Dashboard.setsPerMuscle(sets, muscles, today, DASHBOARD_WEEKS)
        val volumePerMuscle = Dashboard.volumePerMuscle(sets, muscles, today, DASHBOARD_WEEKS)

        fun rate(results: List<Pair<SetRecord, Boolean>>) = if (results.isEmpty()) null else results.count { it.second }.toDouble() / results.size
        val primary = muscles.mapValues { (_, l) -> l.filter { it.percent >= 100 }.map { it.muscle } }

        val lastTrained = sets.groupBy { it.exerciseId }.mapValues { (_, l) -> l.maxOf { it.date } }
        val activePlan = db.weekPlans().all().firstOrNull { it.active }

        return DashboardData(
            unit = settings.unit,
            targetPerWeek = activePlan?.targetPerWeek,
            trainingsPerWeek = Dashboard.trainingsPerWeek(sessionRecords, today, DASHBOARD_WEEKS),
            durations = sessionRecords.sortedBy { it.date }.takeLast(10).map { it.date to (it.durationMs ?: 0L) },
            timePerPosition = Dashboard.timePerPosition(timings),
            goalRate = Dashboard.goalRatePerWeek(goalResults, today, DASHBOARD_WEEKS),
            bodyweight = bw,
            bodyweightTrend = Dashboard.movingAverage(bw, 7),
            bodyweightRanges = bwDays.filter { it.count > 1 }.map { DayRange(LocalDate.ofEpochDay(it.epochDay), it.lowKg, it.highKg) },
            bodyweightOthers = bwDays.flatMap { day -> day.others.map { Point(LocalDate.ofEpochDay(day.epochDay), it.kg) } },
            exercises = lastTrained.keys.mapNotNull { exercises[it] }.sortedByDescending { lastTrained[it.id] },
            progress = lastTrained.keys.associateWith { Dashboard.exerciseProgress(sets, it) },
            records = Dashboard.records(sets),
            goalRateByExercise = goalResults.groupBy { it.first.exerciseId }.mapNotNull { (k, v) -> rate(v)?.let { k to it } }.toMap(),
            setsPerMuscle = setsPerMuscle,
            volumePerMuscle = volumePerMuscle,
            setsPerGroup = Dashboard.byGroup(setsPerMuscle),
            volumePerGroup = Dashboard.byGroup(volumePerMuscle),
            goalRateByMuscle = Muscle.entries.mapNotNull { m -> rate(goalResults.filter { m in primary[it.first.exerciseId].orEmpty() })?.let { m to it } }.toMap(),
            goalRateByGroup = MuscleGroup.entries.mapNotNull { g -> rate(goalResults.filter { r -> primary[r.first.exerciseId].orEmpty().any { it.group == g } })?.let { g to it } }.toMap(),
            pushPull = Dashboard.pushPull(setsPerMuscle.mapValues { (_, l) -> l.takeLast(4) }),
            upperLower = Dashboard.upperLower(setsPerMuscle.mapValues { (_, l) -> l.takeLast(4) }),
            loaded = true,
        )
    }
}
