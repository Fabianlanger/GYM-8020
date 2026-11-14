package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import kotlin.math.abs

/**
 * The advice next to your target (#63, decision #3): what [GoalAdvisor] suggests from your performance. Never a
 * target by itself; shown only where it differs from your target.
 */
object Advice {

    /**
     * Per set of your [target] the advised weight, or null when there is none to show: reps progression, a
     * warm-up, a set whose reps you did not enter today, or advice equal to your target ("good").
     * The k-th heavy set of the target takes the k-th heavy set of [today], as in [DefaultTarget].
     */
    fun perSet(
        target: List<PlannedSet>,
        today: List<PerformedSet>,
        progression: Progression,
        loadType: LoadType,
        repsTarget: (SetRole) -> Int?,
        goodMargin: Int,
        stepKg: Double,
        steps: WeightSteps? = null,
    ): List<Double?> {
        // Advice is on weight only; with reps progression you grow in reps.
        if (progression != Progression.WEIGHT) return target.map { null }
        val byRole = today.groupBy { it.role }
        return target.mapIndexed { i, set ->
            val k = target.take(i).count { it.role == set.role }
            val done = byRole[set.role]?.getOrNull(k) ?: return@mapIndexed null
            val reps = done.reps ?: return@mapIndexed null
            val goal = repsTarget(set.role) ?: return@mapIndexed null
            val lifted = PerformanceWeight.orNone(done.weightKg, progression, loadType) ?: return@mapIndexed null
            GoalAdvisor.suggestWeight(lifted, GoalAdvisor.signal(reps, goal, goodMargin), stepKg, loadType, steps)
                .takeIf { set.weightKg == null || abs(it - set.weightKg) > 1e-9 }
        }
    }

    /**
     * The advice as one line next to the collapsed target (#72): per work set of [target], in its order, the
     * advised weight from [advice] (as [perSet] gives it), or the set's own target where there is none to show.
     * So with two heavy sets the k-th entry is always the k-th set ("H 60 · H 47,5 · L 22,5"), never the advice
     * of the second heavy set next to the first. Null when no set has advice.
     */
    fun line(target: List<PlannedSet>, advice: List<Double?>): List<PlannedSet>? {
        if (advice.none { it != null }) return null
        return target.withIndex().filter { it.value.role.isWork }.map { (i, set) -> PlannedSet(set.role, advice.getOrNull(i) ?: set.weightKg) }
    }
}
