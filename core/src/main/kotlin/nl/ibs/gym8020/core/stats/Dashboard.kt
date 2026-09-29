package nl.ibs.gym8020.core.stats

import nl.ibs.gym8020.core.logic.HomeAdvisor
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleGroup
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import java.time.LocalDate

/** One performed set with everything the statistics need. [effectiveKg] is null when unknown (e.g. no body weight). */
data class SetRecord(
    val sessionId: Long,
    val date: LocalDate,
    val exerciseId: Long,
    val role: SetRole,
    val weightKg: Double?,
    val reps: Int?,
    val effectiveKg: Double?,
    val goalWeightKg: Double?,
    val progression: Progression,
)

data class SessionRecord(val id: Long, val date: LocalDate, val durationMs: Long?)

/** Time spent on one exercise of a training, by its position (1 = first). */
data class ExerciseTiming(val sessionId: Long, val position: Int, val trainMs: Long?)

data class Point(val date: LocalDate, val value: Double)
data class WeekValue(val weekStart: LocalDate, val value: Double)

/** Progress of one exercise per training. */
data class ExerciseProgress(
    /** Best estimated 1RM of the heavy sets (effective weight, Epley). */
    val e1rm: List<Point>,
    /** Heaviest heavy-set effective weight. */
    val heavyWeight: List<Point>,
    /** Most reps in a work set (for reps progression). */
    val reps: List<Point>,
)

data class Record(val exerciseId: Long, val date: LocalDate, val weightKg: Double?, val reps: Int, val e1rm: Double)

data class Balance(val a: Double, val b: Double) {
    /** Share of a in a + b, 0..1 (0.5 = balanced); null without data. */
    val ratio: Double? get() = if (a + b > 0) a / (a + b) else null
}

/**
 * Dashboard statistics (D-39, D-66, D-67), kept separately per exercise, muscle and muscle group.
 * Warm-up sets never count (D-45). A secondary muscle counts for its percentage (50 % = half a set).
 */
object Dashboard {

    private fun work(sets: List<SetRecord>) = sets.filter { it.role.isWork && (it.reps ?: 0) > 0 }

    /** Trainings per week for the last [weeks] weeks, oldest first (weeks start on Monday). */
    fun trainingsPerWeek(sessions: List<SessionRecord>, today: LocalDate, weeks: Int): List<WeekValue> {
        val current = HomeAdvisor.weekStart(today)
        return (weeks - 1 downTo 0).map { back ->
            val start = current.minusWeeks(back.toLong())
            WeekValue(start, sessions.count { !it.date.isBefore(start) && it.date.isBefore(start.plusWeeks(1)) }.toDouble())
        }
    }

    fun exerciseProgress(sets: List<SetRecord>, exerciseId: Long): ExerciseProgress {
        val bySession = work(sets.filter { it.exerciseId == exerciseId }).groupBy { it.sessionId }.values.sortedBy { it.first().date }
        val e1rm = mutableListOf<Point>()
        val heavy = mutableListOf<Point>()
        val reps = mutableListOf<Point>()
        for (s in bySession) {
            val date = s.first().date
            val heavySets = s.filter { it.role == SetRole.HEAVY && it.effectiveKg != null }
            heavySets.maxOfOrNull { Strength.e1rm(it.effectiveKg!!, it.reps!!) }?.let { e1rm += Point(date, it) }
            heavySets.maxOfOrNull { it.effectiveKg!! }?.let { heavy += Point(date, it) }
            s.maxOfOrNull { it.reps!! }?.let { reps += Point(date, it.toDouble()) }
        }
        return ExerciseProgress(e1rm, heavy, reps)
    }

    /** Best heavy set ever per exercise (by e1RM). */
    fun records(sets: List<SetRecord>): Map<Long, Record> =
        work(sets).filter { it.role == SetRole.HEAVY && it.effectiveKg != null }
            .groupBy { it.exerciseId }
            .mapValues { (id, l) ->
                val best = l.maxWith(compareBy<SetRecord> { Strength.e1rm(it.effectiveKg!!, it.reps!!) }.thenBy { it.date })
                Record(id, best.date, best.weightKg, best.reps!!, Strength.e1rm(best.effectiveKg!!, best.reps))
            }

    /** Work sets per week per muscle (weighted by share). */
    fun setsPerMuscle(
        sets: List<SetRecord>,
        muscles: Map<Long, List<MuscleShare>>,
        today: LocalDate,
        weeks: Int,
    ): Map<Muscle, List<WeekValue>> = perMuscleWeekly(sets, muscles, today, weeks) { _, share -> share }

    /** Volume (effective weight × reps × share) per week per muscle. */
    fun volumePerMuscle(
        sets: List<SetRecord>,
        muscles: Map<Long, List<MuscleShare>>,
        today: LocalDate,
        weeks: Int,
    ): Map<Muscle, List<WeekValue>> = perMuscleWeekly(sets, muscles, today, weeks) { s, share ->
        (s.effectiveKg ?: 0.0) * (s.reps ?: 0) * share
    }

    private fun perMuscleWeekly(
        sets: List<SetRecord>,
        muscles: Map<Long, List<MuscleShare>>,
        today: LocalDate,
        weeks: Int,
        amount: (SetRecord, Double) -> Double,
    ): Map<Muscle, List<WeekValue>> {
        val current = HomeAdvisor.weekStart(today)
        val starts = (weeks - 1 downTo 0).map { current.minusWeeks(it.toLong()) }
        val totals = HashMap<Pair<Muscle, LocalDate>, Double>()
        for (s in work(sets)) {
            val week = HomeAdvisor.weekStart(s.date)
            if (week.isBefore(starts.first()) || week.isAfter(current)) continue
            for (m in muscles[s.exerciseId].orEmpty()) {
                val key = m.muscle to week
                totals[key] = (totals[key] ?: 0.0) + amount(s, m.percent / 100.0)
            }
        }
        return Muscle.entries.associateWith { m -> starts.map { WeekValue(it, totals[m to it] ?: 0.0) } }
    }

    /** Roll-up from muscles to muscle groups. */
    fun byGroup(perMuscle: Map<Muscle, List<WeekValue>>): Map<MuscleGroup, List<WeekValue>> =
        MuscleGroup.entries.associateWith { g ->
            val parts = perMuscle.filterKeys { it.group == g }.values
            val weeks = parts.firstOrNull()?.map { it.weekStart }.orEmpty()
            weeks.mapIndexed { i, w -> WeekValue(w, parts.sumOf { it[i].value }) }
        }

    private val push = setOf(Muscle.CHEST, Muscle.FRONT_DELT, Muscle.SIDE_DELT, Muscle.TRICEPS)
    private val pull = setOf(Muscle.LATS, Muscle.MID_BACK, Muscle.REAR_DELT, Muscle.BICEPS, Muscle.TRAPS)

    /** Push vs pull work sets over the given per-muscle weekly sets. */
    fun pushPull(setsPerMuscle: Map<Muscle, List<WeekValue>>): Balance =
        Balance(setsPerMuscle.filterKeys { it in push }.values.sumOf { l -> l.sumOf { it.value } },
            setsPerMuscle.filterKeys { it in pull }.values.sumOf { l -> l.sumOf { it.value } })

    /** Upper body vs legs work sets. */
    fun upperLower(setsPerMuscle: Map<Muscle, List<WeekValue>>): Balance =
        Balance(setsPerMuscle.filterKeys { it.group != MuscleGroup.LEGS && it.group != MuscleGroup.CORE }.values.sumOf { l -> l.sumOf { it.value } },
            setsPerMuscle.filterKeys { it.group == MuscleGroup.LEGS }.values.sumOf { l -> l.sumOf { it.value } })

    /**
     * Whether each work set reached its goal (D-22): planned weight lifted, or for reps progression
     * at least as many reps as the best set of the same role last time. Sets without a goal are skipped.
     */
    fun goalResults(sets: List<SetRecord>): List<Pair<SetRecord, Boolean>> {
        val out = mutableListOf<Pair<SetRecord, Boolean>>()
        val sorted = work(sets).sortedWith(compareBy({ it.date }, { it.sessionId }))
        val lastReps = HashMap<Pair<Long, SetRole>, Int>()
        for ((_, sessionSets) in sorted.groupBy { it.sessionId }) {
            val best = HashMap<Pair<Long, SetRole>, Int>()
            for (s in sessionSets) {
                val key = s.exerciseId to s.role
                if (s.progression == Progression.REPS && (s.weightKg ?: 0.0) == 0.0) {
                    lastReps[key]?.let { out += s to (s.reps!! >= it) }
                } else if (s.goalWeightKg != null && s.weightKg != null) {
                    out += s to (s.weightKg >= s.goalWeightKg - 1e-9)
                }
                best[key] = maxOf(best[key] ?: 0, s.reps!!)
            }
            lastReps.putAll(best)
        }
        return out
    }

    /** Share of reached goals per week, 0..1 (NaN-free: weeks without goals are left out). */
    fun goalRatePerWeek(results: List<Pair<SetRecord, Boolean>>, today: LocalDate, weeks: Int): List<WeekValue> {
        val current = HomeAdvisor.weekStart(today)
        return (weeks - 1 downTo 0).mapNotNull { back ->
            val start = current.minusWeeks(back.toLong())
            val inWeek = results.filter { HomeAdvisor.weekStart(it.first.date) == start }
            if (inWeek.isEmpty()) null else WeekValue(start, inWeek.count { it.second }.toDouble() / inWeek.size)
        }
    }

    /** Average exercise time per position in the training, to see fatigue (D-39). */
    fun timePerPosition(timings: List<ExerciseTiming>): List<Pair<Int, Double>> =
        timings.filter { it.trainMs != null && it.trainMs > 0 }
            .groupBy { it.position }
            .toSortedMap()
            .map { (pos, l) -> pos to l.map { it.trainMs!!.toDouble() }.average() }

    /** Trailing moving average over [window] points (trend line for body weight). */
    fun movingAverage(points: List<Point>, window: Int): List<Point> =
        points.indices.map { i ->
            val from = maxOf(0, i - window + 1)
            Point(points[i].date, points.subList(from, i + 1).map { it.value }.average())
        }

    /** Relative change between the first and last value, e.g. 0.05 = +5 %. */
    fun change(points: List<Point>): Double? {
        if (points.size < 2) return null
        val first = points.first().value
        return if (first == 0.0) null else (points.last().value - first) / first
    }
}
