package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.SetRole

/** Switching the role of a planned set by tapping its label (B-3): heavy → light → warm-up → heavy. */
object PlanRoles {
    fun next(role: SetRole): SetRole = when (role) {
        SetRole.HEAVY -> SetRole.LIGHT
        SetRole.LIGHT -> SetRole.WARMUP
        SetRole.WARMUP -> SetRole.HEAVY
    }

    /**
     * Switches set [index] to the next role. A work set takes the weight of another set with that role
     * (so a new light set gets your light weight), else keeps its own; warm-ups have no planned weight.
     */
    fun cycle(sets: List<PlannedSet>, index: Int): List<PlannedSet> {
        val set = sets.getOrNull(index) ?: return sets
        val role = next(set.role)
        val weight = if (!role.isWork) {
            null
        } else {
            sets.filterIndexed { j, s -> j != index && s.role == role && s.weightKg != null }.lastOrNull()?.weightKg
                ?: set.weightKg
                ?: sets.lastOrNull { it.role.isWork && it.weightKg != null }?.weightKg
        }
        return sets.mapIndexed { j, s -> if (j == index) PlannedSet(role, weight) else s }
    }
}
