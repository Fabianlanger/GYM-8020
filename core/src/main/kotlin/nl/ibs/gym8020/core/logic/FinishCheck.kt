package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole

/** A performed set as needed for the check: only what you entered (the app never prefills it). */
data class FinishSet(val role: SetRole, val weightKg: Double?, val reps: Int?)

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

/**
 * Which exercises need a weight in a performed set (#47). For now only weight progression with an external
 * load does; reps progression and bodyweight exercises need only reps (details follow in #4).
 */
object PerformanceWeight {
    fun required(progression: Progression, loadType: LoadType): Boolean =
        progression == Progression.WEIGHT && loadType == LoadType.EXTERNAL

    /** The weight as read for stats and advice: where it is optional, an empty field means none (0). */
    fun orNone(weightKg: Double?, progression: Progression, loadType: LoadType): Double? =
        weightKg ?: if (required(progression, loadType)) null else 0.0

    /** The weight that goes into the advice: a set you did not do (no reps, no weight) keeps its target. */
    fun forPlan(weightKg: Double?, reps: Int?, goalWeightKg: Double?, progression: Progression, loadType: LoadType): Double? =
        if (weightKg == null && reps == null && goalWeightKg != null) goalWeightKg else orNone(weightKg, progression, loadType)
}

/** The check when you finish a session (#47). The app never fills in a performance itself. */
object FinishCheck {

    fun check(exercises: List<FinishExercise>): List<Finding> = exercises.filter { !it.skipped }.flatMap { ex ->
        performance(ex) + target(ex)
    }

    /** Your target may be incomplete: then the default target applies, so this only warns. */
    private fun target(ex: FinishExercise): List<Finding> {
        if (!PerformanceWeight.required(ex.progression, ex.loadType)) return emptyList()
        return ex.target.orEmpty().mapIndexedNotNull { i, set ->
            if (set.role.isWork && set.weightKg == null) Finding(FindingKind.WARN, ex.sessionExerciseId, ex.exerciseId, set.role, i, Missing.WEIGHT) else null
        }
    }

    private fun performance(ex: FinishExercise): List<Finding> {
        val needsWeight = PerformanceWeight.required(ex.progression, ex.loadType)
        return ex.sets.mapIndexedNotNull { i, set ->
            // Warm-ups are not performance; an empty one is simply not done.
            if (!set.role.isWork) return@mapIndexedNotNull null
            val (kind, missing) = when {
                set.weightKg == null && set.reps == null -> FindingKind.SKIP to Missing.ALL
                set.reps == null -> FindingKind.BLOCK to Missing.REPS
                set.weightKg == null && needsWeight -> FindingKind.BLOCK to Missing.WEIGHT
                else -> return@mapIndexedNotNull null
            }
            Finding(kind, ex.sessionExerciseId, ex.exerciseId, set.role, i, missing)
        }
    }
}
