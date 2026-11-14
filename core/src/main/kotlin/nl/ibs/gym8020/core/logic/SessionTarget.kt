package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.SetRole

/**
 * The session target follows the role (#92, decision #90): what a set aims at today depends on its role, not on its
 * place. Assigned from the target the session started with, so adding a set or switching a role never loses a target.
 */
object SessionTarget {

    /**
     * Per set with [roles], the set of [start] (the target this session started with) it aims at: the k-th heavy set
     * the k-th heavy set of [start], the k-th light set the k-th light one. Null for a warm-up, and for an extra set for
     * which [start] had no target: that set has no session target and does not count for "goals reached".
     */
    fun byRole(start: List<PlannedSet>, roles: List<SetRole>): List<PlannedSet?> = roles.mapIndexed { i, role ->
        if (!role.isWork) return@mapIndexed null
        val k = roles.take(i).count { it == role }
        start.filter { it.role == role }.getOrNull(k)
    }
}
