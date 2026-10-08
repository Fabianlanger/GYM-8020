package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole

/**
 * The default target (#52): what holds for next time when you set nothing yourself. Never the advice.
 */
object DefaultTarget {

    /**
     * The sets of your [stored] target (or, without one, the [workout]'s roles), each with the weight of the
     * matching set you did [today]. A set you did not do (no reps, so also a skipped set) keeps its previous
     * target. With reps progression, and for warm-ups, the target has no weight.
     */
    fun of(
        stored: List<PlannedSet>?,
        workout: List<SetRole>,
        today: List<PerformedSet>,
        progression: Progression,
        loadType: LoadType,
    ): List<PlannedSet> {
        val structure = stored?.takeIf { it.isNotEmpty() } ?: workout.map { PlannedSet(it, null) }
        val byRole = today.groupBy { it.role }
        return structure.mapIndexed { i, set ->
            // Warm-ups have no target weight; with reps progression the target is reps only.
            if (!set.role.isWork || progression == Progression.REPS) return@mapIndexed PlannedSet(set.role, null)
            // The k-th heavy set of the target takes the k-th heavy set of today, and so on.
            val k = structure.take(i).count { it.role == set.role }
            val done = byRole[set.role]?.getOrNull(k)?.takeIf { it.reps != null }
            val lifted = done?.let { PerformanceWeight.orNone(it.weightKg, progression, loadType) }
            PlannedSet(set.role, lifted ?: set.weightKg)
        }
    }
}
