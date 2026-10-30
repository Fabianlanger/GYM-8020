package nl.ibs.gym8020.core.stats

import nl.ibs.gym8020.core.logic.Change
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleShare
import kotlin.math.abs

/** One muscle in "Vs. last time": strength, only what you did today. */
data class MuscleStrength(
    /**
     * Share-weighted mean of the relative strength change (0.04 = +4 %) of the exercises done today that train
     * it; null when none of them has a comparison (first time), or when it was only trained by skipped ones.
     */
    val change: Double?,
    val trend: Trend?,
    /** Today only trained by exercises you skipped: its own state, not "equal". */
    val skipped: Boolean,
)

/** One muscle in "Totaal": volume against the previous session of the same workout. */
data class MuscleVolume(
    /** Volume today in kg (Σ effective weight × reps × share); skipped exercises count as 0. */
    val now: Double,
    /** Null without a previous session of this workout. */
    val change: Change?,
    val trend: Trend?,
    /** Today only trained by exercises you skipped. */
    val skipped: Boolean,
)

/** An exercise done today that trains the muscle, with its strength next to the last time. */
data class StrengthPart(val exerciseId: Long, val percent: Int, val comparison: ExerciseComparison)

/** An exercise done today that trains the muscle, with the volume it gave that muscle. */
data class VolumePart(
    val exerciseId: Long,
    val percent: Int,
    /** Volume for this muscle: the exercise's volume × its share. */
    val kg: Double,
    /** Part of the muscle's volume today (0.35 = 35 %); null when the muscle has no volume today. */
    val share: Double?,
)

/** Tap a muscle: which exercises count, with their numbers, and which you skipped. In session order. */
data class MuscleBreakdown(
    val strength: List<StrengthPart>,
    val volume: List<VolumePart>,
    val skipped: List<Long>,
)

data class MuscleMap(
    val strength: Map<Muscle, MuscleStrength>,
    val volume: Map<Muscle, MuscleVolume>,
    val breakdown: Map<Muscle, MuscleBreakdown>,
)

/**
 * The muscle map in the summary (#67, decision #8 points 2, 3 and 5).
 *
 * Rules:
 * - **Done / skipped**: an exercise of this session counts as done with a work set with reps today
 *   ([Comparison.exercise]); every other exercise of the session counts as skipped.
 * - **Strength** ("Vs. last time"): per exercise the comparison of the summary list ([Comparison.exercise], against
 *   its last time); per muscle the share-weighted mean of their relative changes. Only what you did counts.
 *   Within ±1 % ([Comparison.SAME_MARGIN]) is equal, as per exercise. A muscle only trained by skipped exercises
 *   is [MuscleStrength.skipped]; one trained today without any comparison has no change.
 * - **Volume** ("Totaal"): today against the previous session of the same workout ([Strength.volumePerMuscle]);
 *   skipped exercises have no sets, so they count as 0. Absolute and relative; trend as [Comparison.trend].
 * - A muscle that no exercise of today or of the previous session trains is absent.
 */
object MuscleComparison {

    /**
     * [today] holds this session's sets (skipped exercises have none), [planned] its exercises in session order
     * (done and skipped), [lastTimes] the last time per exercise ([Comparison.lastTime]), [previousSession] the
     * sets of the previous session of the same workout (null when there is none).
     */
    fun of(
        today: List<SetRecord>,
        planned: List<Long>,
        lastTimes: Map<Long, List<SetRecord>>,
        previousSession: List<SetRecord>?,
        muscles: Map<Long, List<MuscleShare>>,
    ): MuscleMap {
        val byExercise = today.groupBy { it.exerciseId }
        val order = (planned + byExercise.keys).distinct()
        val comparisons = order.mapNotNull { id ->
            byExercise[id]?.let { Comparison.exercise(it, lastTimes[id]) }?.let { id to it }
        }.toMap()
        val skippedIds = order.filter { it !in comparisons }
        fun shares(id: Long) = muscles[id].orEmpty().filter { it.percent > 0 }

        val volumeNow = Strength.volumePerMuscle(today, muscles)
        val volumeBefore = previousSession?.let { Strength.volumePerMuscle(it, muscles) }

        val trained = (order.flatMap { id -> shares(id).map { it.muscle } } + volumeBefore?.keys.orEmpty()).distinct()
        val strength = HashMap<Muscle, MuscleStrength>()
        val volume = HashMap<Muscle, MuscleVolume>()
        val breakdown = HashMap<Muscle, MuscleBreakdown>()
        for (m in trained) {
            val strengthParts = comparisons.mapNotNull { (id, c) ->
                shares(id).firstOrNull { it.muscle == m }?.let { StrengthPart(id, it.percent, c) }
            }
            val skipped = skippedIds.filter { id -> shares(id).any { it.muscle == m } }
            val onlySkipped = strengthParts.isEmpty() && skipped.isNotEmpty()

            val compared = strengthParts.filter { it.comparison.change?.relative != null }
            val weight = compared.sumOf { it.percent / 100.0 }
            val change = if (compared.isEmpty()) null else compared.sumOf { it.comparison.change!!.relative!! * it.percent / 100.0 } / weight
            strength[m] = MuscleStrength(change, change?.let(::trend), onlySkipped)

            val now = volumeNow[m] ?: 0.0
            val volumeChange = volumeBefore?.let { Change.between(it[m] ?: 0.0, now) }
            volume[m] = MuscleVolume(now, volumeChange, volumeChange?.let { Comparison.trend(it) }, onlySkipped)

            val volumeParts = strengthParts.map { p ->
                val kg = Strength.volume(byExercise[p.exerciseId].orEmpty()) * p.percent / 100.0
                VolumePart(p.exerciseId, p.percent, kg, if (now > 0) kg / now else null)
            }
            breakdown[m] = MuscleBreakdown(strengthParts, volumeParts, skipped)
        }
        return MuscleMap(strength, volume, breakdown)
    }

    /** Trend of a muscle's mean relative change, with the same margin of noise as per exercise. */
    private fun trend(change: Double): Trend = when {
        abs(change) <= Comparison.SAME_MARGIN + 1e-9 -> Trend.SAME
        change > 0 -> Trend.UP
        else -> Trend.DOWN
    }
}
