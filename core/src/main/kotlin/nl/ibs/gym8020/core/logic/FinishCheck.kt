package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole

/**
 * A performed set as needed for the check. [goalWeightKg] is the weight the app put in from your target;
 * a set with only that weight and no reps is one you did not fill in.
 */
data class FinishSet(val role: SetRole, val weightKg: Double?, val reps: Int?, val goalWeightKg: Double? = null)

/** One exercise of the session as needed for the check at finishing (#47). */
data class FinishExercise(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val progression: Progression,
    val loadType: LoadType,
    val sets: List<FinishSet>,
    /** Skipped exercises are left out of the check entirely. */
    val skipped: Boolean = false,
    /** The target for next time as shown; null when there is none yet. */
    val target: List<PlannedSet>? = null,
)

/** BLOCK: finishing is not possible. SKIP: an empty set that will count as skipped. WARN: may continue. */
enum class FindingKind { BLOCK, SKIP, WARN }

/** What is missing in a set: weight, reps, or both ([ALL], an empty set). */
enum class Missing { WEIGHT, REPS, ALL }

/** One line in the overview at finishing; [position] is the index of the set in the exercise's list. */
data class Finding(
    val kind: FindingKind,
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val role: SetRole,
    val position: Int,
    val missing: Missing,
)

/** The check when you finish a session (#47). The app never fills in a performance itself. */
object FinishCheck {

    /**
     * Whether a work set needs a weight. For now only weight progression with an external load does;
     * reps progression and bodyweight exercises need only reps (details follow in #4).
     */
    fun weightRequired(progression: Progression, loadType: LoadType): Boolean =
        progression == Progression.WEIGHT && loadType == LoadType.EXTERNAL

    fun check(exercises: List<FinishExercise>): List<Finding> = exercises.filter { !it.skipped }.flatMap { ex ->
        performance(ex) + target(ex)
    }

    /** Your target may be incomplete: then the default target applies, so this only warns. */
    private fun target(ex: FinishExercise): List<Finding> {
        if (!weightRequired(ex.progression, ex.loadType)) return emptyList()
        return ex.target.orEmpty().mapIndexedNotNull { i, set ->
            if (set.role.isWork && set.weightKg == null) Finding(FindingKind.WARN, ex.sessionExerciseId, ex.exerciseId, set.role, i, Missing.WEIGHT) else null
        }
    }

    private fun performance(ex: FinishExercise): List<Finding> {
        val needsWeight = weightRequired(ex.progression, ex.loadType)
        return ex.sets.mapIndexedNotNull { i, set ->
            // Warm-ups are not performance; an empty one is simply not done.
            if (!set.role.isWork) return@mapIndexedNotNull null
            // A weight only counts as entered when it is something other than what the app put in.
            val weight = set.weightKg?.takeIf { it != set.goalWeightKg && !(it == 0.0 && !needsWeight) }
            val (kind, missing) = when {
                weight == null && set.reps == null -> FindingKind.SKIP to Missing.ALL
                set.reps == null -> FindingKind.BLOCK to Missing.REPS
                set.weightKg == null && needsWeight -> FindingKind.BLOCK to Missing.WEIGHT
                else -> return@mapIndexedNotNull null
            }
            Finding(kind, ex.sessionExerciseId, ex.exerciseId, set.role, i, missing)
        }
    }
}
