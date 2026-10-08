package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.SetRole

/**
 * The target of an exercise with reps progression (#52): per set the reps of that set's role last time,
 * not the general heavy/light targets from the settings (those are for weight progression only).
 */
object RepsTarget {

    /** One number per set in [roles]; null when there is no last time to beat. */
    fun forSets(roles: List<SetRole>, last: List<PerformedSet>?): List<Int?> {
        // Only sets you really did count; the k-th heavy set aims at the k-th heavy set (else the last one).
        val done = last.orEmpty().filter { it.role.isWork && it.reps != null }.groupBy { it.role }
        return roles.mapIndexed { i, role ->
            val sameRole = done[role] ?: return@mapIndexed null
            val k = roles.take(i).count { it == role }
            (sameRole.getOrNull(k) ?: sameRole.last()).reps
        }
    }
}
