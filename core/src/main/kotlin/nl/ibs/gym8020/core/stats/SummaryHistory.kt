package nl.ibs.gym8020.core.stats

import nl.ibs.gym8020.core.logic.RepsRecord
import nl.ibs.gym8020.core.logic.Strength

/** A finished training as the summary orders them: by start time, then by id; [workout] is its workout name. */
data class TrainingRef(val id: Long, val startedAt: Long, val workout: String)

/**
 * The history one training's summary looks at (#66, #100): only the trainings **before** it count. Right after
 * finishing the training is the newest, so that is all history; for an older training opened from the log it is
 * what you saw then. A later, better training never changes its comparison or its records ("records zoals ze toen
 * golden").
 *
 * - [earlier]: the trainings before it, newest first (start time; on equal start times the lower id is earlier).
 * - [lastTime]: per exercise the newest earlier training with a work set ([Comparison.lastTime]).
 * - [previousOfWorkout]: the newest earlier training of the same workout.
 * - [weightRecord] / [repsRecord]: a record against the earlier trainings only.
 */
class SummaryHistory private constructor(
    val training: TrainingRef,
    /** This training's sets. */
    val today: List<SetRecord>,
    /** The trainings before [training], newest first. */
    val earlier: List<TrainingRef>,
    private val setsBySession: Map<Long, List<SetRecord>>,
) {
    /** Sets per earlier training, newest first. */
    val earlierSets: List<List<SetRecord>> = earlier.map { setsOf(it.id) }

    /** The sets of training [id] (empty when it has none). */
    fun setsOf(id: Long): List<SetRecord> = setsBySession[id].orEmpty()

    /** Today's sets of [exerciseId]. */
    fun today(exerciseId: Long): List<SetRecord> = today.filter { it.exerciseId == exerciseId }

    /** The last time of [exerciseId] before this training, as the comparison uses it; null when there is none. */
    fun lastTime(exerciseId: Long): List<SetRecord>? = Comparison.lastTime(exerciseId, earlierSets)

    /** The newest earlier training that has any set of [exerciseId], with those sets (older goal rule, #69). */
    fun lastSets(exerciseId: Long): List<SetRecord>? =
        earlierSets.firstNotNullOfOrNull { sets -> sets.filter { it.exerciseId == exerciseId }.takeIf { it.isNotEmpty() } }

    /** The previous training of the same workout; null for the first one. */
    val previousOfWorkout: TrainingRef? get() = earlier.firstOrNull { it.workout == training.workout }

    /**
     * Record on weight: today's best heavy set ([Strength.bestSet]) when its e1RM beats every earlier training of
     * the exercise. The first time is not a record (nothing to beat). Later trainings never count.
     */
    fun weightRecord(exerciseId: Long): SetRecord? {
        val best = Strength.bestSet(today(exerciseId)) ?: return null
        val now = Strength.e1rm(best) ?: return null
        val before = earlierSets.mapNotNull { sets -> Strength.of(sets.filter { it.exerciseId == exerciseId }) }.maxOrNull()
            ?: return null
        return best.takeIf { now > before + 1e-6 }
    }

    /** Record on reps (#73) against the earlier trainings only; null when it is none. */
    fun repsRecord(exerciseId: Long): RepsRecord? =
        RepsRecord.of(today(exerciseId), earlierSets.reversed().map { sets -> sets.filter { it.exerciseId == exerciseId } })

    companion object {
        /** True when [a] comes before [b]: earlier start, or the same start and a lower id. */
        fun before(a: TrainingRef, b: TrainingRef): Boolean =
            a.startedAt < b.startedAt || (a.startedAt == b.startedAt && a.id < b.id)

        /**
         * The history of training [trainingId] among [trainings] (finished ones) and their [sets]; null when the
         * training is not among them.
         */
        fun of(trainingId: Long, trainings: List<TrainingRef>, sets: List<SetRecord>): SummaryHistory? {
            val training = trainings.firstOrNull { it.id == trainingId } ?: return null
            val earlier = trainings.filter { before(it, training) }
                .sortedWith(compareByDescending<TrainingRef> { it.startedAt }.thenByDescending { it.id })
            val bySession = sets.groupBy { it.sessionId }
            return SummaryHistory(training, bySession[trainingId].orEmpty(), earlier, bySession)
        }
    }
}
