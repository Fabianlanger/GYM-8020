package nl.ibs.gym8020.core.stats

import nl.ibs.gym8020.core.logic.Change
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/** Direction of a [Change]; [SAME] also for a change within the margin of noise ([Comparison.SAME_MARGIN]). */
enum class Trend { UP, SAME, DOWN }

/** What an exercise's strength is measured in: heavy-set e1RM, or reps for reps progression. */
enum class Measure { E1RM, REPS }

/** The best light set of today next to the last time, only when it changed: "L 40 × 20 (+3 reps)". */
data class LightComparison(
    /** Entered weight (signed for bodyweight exercises), as shown in the training screen. */
    val weightKg: Double,
    val reps: Int,
    /** Entered weight now − last time. */
    val weightChangeKg: Double,
    val repsChange: Int,
)

/** One exercise in the summary: strength today next to the last time (GLOSSARY "vergelijking"). */
data class ExerciseComparison(
    val measure: Measure,
    /** e1RM in kg or reps; null when unknown (e.g. no heavy set, or no body weight for a bodyweight exercise). */
    val now: Double?,
    /** Null without a last time, or when the last time is measured differently. */
    val change: Change?,
    val trend: Trend?,
    val light: LightComparison?,
    /** No earlier training of this exercise with a work set. */
    val firstTime: Boolean = false,
)

/** One training against the previous session of the same workout. */
data class WorkoutComparison(
    val previousDate: LocalDate,
    /** Volume of the work sets in kg; skipped exercises count as 0. */
    val volume: Double,
    val volumeChange: Change,
    val volumeTrend: Trend,
    /** Work sets with reps. */
    val sets: Int,
    val setsChange: Int,
    /** Exercises skipped in this training. */
    val skipped: Int,
)

/**
 * Comparison in the summary (#66, decision #8 points 1 and 4).
 *
 * Rules:
 * - **Per exercise**: today against the last time of that exercise ([lastTime]: the newest earlier training
 *   from any workout with a work set, skipped ones never count). Weight progression compares the best
 *   heavy-set e1RM ([Strength.of], light sets never count, D-41); reps progression (no weight) the most reps in
 *   one work set, the same score as the heatmap. A different measure than last time is not compared.
 * - **Light set**: the best light set (highest effective weight, then most reps) gets its own line when its
 *   entered weight or reps changed since the last time; not for reps progression (light sets count there).
 * - **Per workout**: volume and work sets against the previous session of the same workout.
 * - A relative difference within ±1 % counts as equal ([SAME_MARGIN]): noise, less than a step on the bar.
 *   Reps are whole, so any rep more or less counts.
 */
object Comparison {

    /** Relative change that still counts as equal. */
    const val SAME_MARGIN = 0.01

    private fun work(sets: List<SetRecord>) = sets.filter { it.role.isWork && (it.reps ?: 0) > 0 }

    /** Reps progression without weight: measured in reps (also the heatmap's rule). [sets] are work sets. */
    fun repsMode(sets: List<SetRecord>): Boolean =
        sets.isNotEmpty() && sets.first().progression == Progression.REPS && sets.all { (it.weightKg ?: 0.0) == 0.0 }

    fun trend(change: Change, margin: Double = SAME_MARGIN): Trend {
        val rel = change.relative
        return when {
            abs(change.absolute) < 1e-9 -> Trend.SAME
            rel != null && abs(rel) <= margin + 1e-9 -> Trend.SAME
            change.absolute > 0 -> Trend.UP
            else -> Trend.DOWN
        }
    }

    /**
     * The last time of an exercise: its sets in the newest of [earlierNewestFirst] (sets per training, newest
     * first, skipped exercises left out) that has a work set with reps for it; null when there is none.
     */
    fun lastTime(exerciseId: Long, earlierNewestFirst: List<List<SetRecord>>): List<SetRecord>? =
        earlierNewestFirst.firstNotNullOfOrNull { training ->
            training.filter { it.exerciseId == exerciseId }.takeIf { work(it).isNotEmpty() }
        }

    /** One exercise today against its [lastTime]; null when today has no work set with reps. */
    fun exercise(today: List<SetRecord>, lastTime: List<SetRecord>?): ExerciseComparison? {
        val now = work(today)
        if (now.isEmpty()) return null
        val repsMode = repsMode(now)
        val last = lastTime?.let(::work).orEmpty()
        val before = last.takeIf { it.isNotEmpty() && repsMode(it) == repsMode }
        val nowValue = score(now, repsMode)
        val change = before?.let { Change.between(score(it, repsMode), nowValue) }
        return ExerciseComparison(
            measure = if (repsMode) Measure.REPS else Measure.E1RM,
            now = nowValue,
            change = change,
            trend = change?.let { trend(it, if (repsMode) 0.0 else SAME_MARGIN) },
            light = if (repsMode || before == null) null else light(now, before),
            firstTime = last.isEmpty(),
        )
    }

    private fun score(work: List<SetRecord>, repsMode: Boolean): Double? =
        if (repsMode) work.maxOf { it.reps!! }.toDouble() else Strength.of(work)

    private fun bestLight(work: List<SetRecord>): SetRecord? =
        work.filter { it.role == SetRole.LIGHT }
            .maxWithOrNull(compareBy<SetRecord> { it.effectiveKg ?: Double.NEGATIVE_INFINITY }.thenBy { it.reps!! })

    private fun light(now: List<SetRecord>, before: List<SetRecord>): LightComparison? {
        val n = bestLight(now) ?: return null
        val b = bestLight(before) ?: return null
        val w = n.weightKg ?: 0.0
        val weightChange = w - (b.weightKg ?: 0.0)
        val repsChange = n.reps!! - b.reps!!
        if (abs(weightChange) < 1e-9 && repsChange == 0) return null
        return LightComparison(w, n.reps, weightChange, repsChange)
    }

    /** This training against the [previous] session of the same workout, held on [previousDate]. */
    fun workout(today: List<SetRecord>, previous: List<SetRecord>, previousDate: LocalDate, skipped: Int): WorkoutComparison {
        val volume = Strength.volume(today)
        val change = Change.between(Strength.volume(previous), volume)!!
        val sets = workSets(today)
        return WorkoutComparison(previousDate, volume, change, trend(change), sets, sets - workSets(previous), skipped)
    }

    /** Work sets with reps, as counted in the summary. */
    fun workSets(sets: List<SetRecord>): Int = sets.count { it.role.isWork && it.reps != null }
}

/** Numbers of a [Change] as the app shows them: sign, decimal comma, weight unit (WeightFormat). */
object ComparisonFormat {

    private const val MINUS = "−"

    /** A weight rounded to 0,1 in [unit], with the unit: "+2,5 kg", "113,3 kg". */
    fun weight(kg: Double, unit: WeightUnit, signed: Boolean = true): String {
        val tenths = (unit.fromKg(kg) * 10).roundToLong() / 10.0
        return WeightFormat.format(unit.toKg(tenths), unit, signed) + " " + unit.name.lowercase()
    }

    /** A fraction as percent with at most one decimal: 0.024 → "+2,4%", −0.006 → "−0,6%". */
    fun percent(fraction: Double): String {
        val tenths = (fraction * 1000).roundToLong()
        val sign = if (tenths > 0) "+" else if (tenths < 0) MINUS else ""
        val a = abs(tenths)
        val text = if (a % 10 == 0L) "${a / 10}" else "${a / 10},${a % 10}"
        return "$sign$text%"
    }

    /** A whole difference: "+2", "−2", "±0". */
    fun count(n: Int): String = when {
        n > 0 -> "+$n"
        n < 0 -> "$MINUS${-n}"
        else -> "±0"
    }

    /** Volume in [unit], whole, with a dot per thousand: "12.400", signed "+350". No unit. */
    fun volume(kg: Double, unit: WeightUnit, signed: Boolean = false): String {
        val v = unit.fromKg(kg).roundToLong()
        val digits = abs(v).toString().reversed().chunked(3).joinToString(".").reversed()
        val sign = if (v < 0) MINUS else if (signed && v > 0) "+" else ""
        return sign + digits
    }
}
