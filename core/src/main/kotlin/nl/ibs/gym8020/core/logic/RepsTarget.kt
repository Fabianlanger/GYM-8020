package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.SetRole

/**
 * The target of an exercise with reps progression (#52): per set the reps of that set's role last time,
 * not the general heavy/light targets from the settings (those are for weight progression only).
 */
object RepsTarget {

    /**
     * One number per set in [roles]; null when there is no last time to beat. The k-th heavy set aims at the k-th
     * heavy set of last time; when that set was skipped it has no number (#91: a skipped set must not shift the
     * sets after it). A set beyond last time's count aims at the last set of that role you really did.
     */
    fun forSets(roles: List<SetRole>, last: List<PerformedSet>?): List<Int?> {
        val byRole = last.orEmpty().filter { it.role.isWork }.groupBy { it.role }
        return roles.mapIndexed { i, role ->
            val sameRole = byRole[role] ?: return@mapIndexed null
            val k = roles.take(i).count { it == role }
            if (k < sameRole.size) sameRole[k].reps else sameRole.lastOrNull { it.reps != null }?.reps
        }
    }
}
