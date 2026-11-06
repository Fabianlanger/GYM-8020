package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole

/**
 * The default target (#52): what holds for next time when you set nothing yourself. Never the advice.
 */
object DefaultTarget {

    /**
     * The sets you did [today], each with the weight you lifted (an extra set included, owner 6 Nov 2026); a set
     * you did not do (no reps, so also a skipped set) keeps the target of the matching set of [stored]. Without
     * work sets today: the sets of your [stored] target, or the [workout]'s roles. Warm-ups of [stored] stay at the
     * front, without a value. With reps progression the target is reps only (#69). Never the advice.
     */
    fun of(
        stored: List<PlannedSet>?,
        workout: List<SetRole>,
        today: List<PerformedSet>,
        progression: Progression,
        loadType: LoadType,
    ): List<PlannedSet> {
        val previous = stored?.takeIf { it.isNotEmpty() } ?: workout.map { PlannedSet(it, null) }
        val todayWork = today.filter { it.role.isWork }
        val structure = if (todayWork.isEmpty()) previous
        else previous.filter { !it.role.isWork } + todayWork.mapIndexed { i, set ->
            // A set not done today keeps the previous target of the k-th set with that role.
            val k = todayWork.take(i).count { it.role == set.role }
            previous.filter { it.role == set.role }.getOrNull(k) ?: PlannedSet(set.role, null)
        }
        val byRole = today.groupBy { it.role }
        return structure.mapIndexed { i, set ->
            // Warm-ups have no target.
            if (!set.role.isWork) return@mapIndexed PlannedSet(set.role, null)
            // The k-th heavy set of the target takes the k-th heavy set of today, and so on.
            val k = structure.take(i).count { it.role == set.role }
            val done = byRole[set.role]?.getOrNull(k)?.takeIf { it.reps != null }
            // With reps progression the target is reps only.
            if (progression == Progression.REPS) return@mapIndexed PlannedSet(set.role, null, done?.reps ?: set.reps)
            val lifted = done?.let { PerformanceWeight.orNone(it.weightKg, progression, loadType) }
            PlannedSet(set.role, lifted ?: set.weightKg)
        }
    }
}
