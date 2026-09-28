package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.Signal

/** A performed set as needed for planning. [weightKg] is the entered value (signed extra for bodyweight). */
data class PerformedSet(val role: SetRole, val weightKg: Double?, val reps: Int?)

/** A planned set for next time. [weightKg] is null for warm-up sets and reps-progression exercises. */
data class PlannedSet(val role: SetRole, val weightKg: Double?)

/**
 * The method: you always go to failure; reps emerge. Plan forward on weight only.
 * - reps ≥ target + margin + 1 → too light → one step heavier
 * - reps within target ± margin → good → keep the weight (aim for more reps)
 * - reps ≤ target − margin − 1 → too heavy → one step lighter
 */
object GoalAdvisor {

    fun signal(reps: Int, target: Int, goodMargin: Int = 1): Signal = when {
        reps > target + goodMargin -> Signal.TOO_LIGHT
        reps < target - goodMargin -> Signal.TOO_HEAVY
        else -> Signal.GOOD
    }

    /**
     * Suggested weight for next time. [stepKg] is one achievable step; for bodyweight exercises
     * "heavier" means more extra weight / less assistance, which is the same direction on the signed value.
     */
    fun suggestWeight(current: Double, signal: Signal, stepKg: Double, loadType: LoadType): Double {
        val next = when (signal) {
            Signal.TOO_LIGHT -> current + stepKg
            Signal.GOOD -> current
            Signal.TOO_HEAVY -> current - stepKg
        }
        return if (loadType == LoadType.EXTERNAL) next.coerceAtLeast(0.0) else next
    }

    /**
     * Plan for next time (D-18a): same number of sets, same roles and warm-ups as just performed,
     * with the smart weight suggestion per work set.
     */
    fun planNext(
        performed: List<PerformedSet>,
        loadType: LoadType,
        progression: Progression,
        settings: MethodSettings,
    ): List<PlannedSet> = performed.map { set ->
        val target = settings.targetFor(set.role)
        when {
            !set.role.isWork || progression == Progression.REPS -> PlannedSet(set.role, if (set.role.isWork) set.weightKg else null)
            set.weightKg == null -> PlannedSet(set.role, null)
            set.reps == null || target == null -> PlannedSet(set.role, set.weightKg)
            else -> PlannedSet(
                set.role,
                suggestWeight(set.weightKg, signal(set.reps, target, settings.goodMargin), settings.weightStepKg, loadType),
            )
        }
    }

    /**
     * Whether the goal of a set was reached. Weight progression: the planned weight was lifted
     * (signed values: higher = harder). Reps progression: at least as many reps as last time.
     */
    fun goalReached(
        progression: Progression,
        goalWeightKg: Double?,
        liftedWeightKg: Double?,
        previousReps: Int?,
        reps: Int?,
    ): Boolean? = when (progression) {
        Progression.WEIGHT -> if (goalWeightKg == null || liftedWeightKg == null) null else liftedWeightKg >= goalWeightKg - 1e-9
        Progression.REPS -> if (previousReps == null || reps == null) null else reps >= previousReps
    }
}
