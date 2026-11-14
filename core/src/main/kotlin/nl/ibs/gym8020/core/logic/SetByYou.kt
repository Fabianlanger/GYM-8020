package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.TargetOrigin

/**
 * Your target for next time with, per set, whether you set it yourself (#92): you changed that set or took over its
 * advice. [yours] has one entry per set of [sets].
 */
data class OwnSets(val sets: List<PlannedSet>, val yours: List<Boolean>) {
    init {
        require(sets.size == yours.size) { "one origin per set: ${sets.size} sets, ${yours.size} origins" }
    }

    /** At least one set is yours. */
    val anyYours: Boolean get() = yours.any { it }

    /** Every work set is yours: the whole target is yours, nothing follows your performance. */
    val allYours: Boolean
        get() = sets.indices.filter { sets[it].role.isWork }.let { work -> work.isNotEmpty() && work.all { yours[it] } }

    companion object {
        /** A target in which you set nothing yourself. */
        fun none(sets: List<PlannedSet>) = OwnSets(sets, sets.map { false })

        /** A target you set as a whole: you changed its sets (added, removed, switched a role) or all of them. */
        fun all(sets: List<PlannedSet>) = OwnSets(sets, sets.map { true })
    }
}

/**
 * "Zelf gezet" per set (#92, decision #90): a set of your target that you changed, or whose advice you took over, stays
 * as you set it; the other sets keep following your performance (the default target), each with its own advice.
 */
object SetByYou {

    /**
     * Your target for next time from the sets you set yourself in [own] and the [default] target of your performance.
     * - Nothing yours: the default target.
     * - Every work set yours (the target as a whole): [own] as it is.
     * - Otherwise the sets of the default target, where the k-th heavy set you set yourself takes the place of the
     *   k-th heavy set (and so on per role). A set of yours that finds no place there is kept at the end.
     */
    fun follow(own: OwnSets?, default: List<PlannedSet>): OwnSets {
        if (own == null || !own.anyYours) return OwnSets.none(default)
        if (own.allYours) return own
        val placed = HashSet<Int>()
        val followed = default.mapIndexed { i, set ->
            if (!set.role.isWork) return@mapIndexed set to false
            val k = default.take(i).count { it.role == set.role }
            val j = own.sets.indices.filter { own.sets[it].role == set.role }.getOrNull(k)
            if (j != null && own.yours[j]) {
                placed += j
                own.sets[j] to true
            } else {
                set to false
            }
        }
        val rest = own.sets.indices.filter { own.yours[it] && own.sets[it].role.isWork && it !in placed }.map { own.sets[it] to true }
        val all = followed + rest
        return OwnSets(all.map { it.first }, all.map { it.second })
    }

    /** You set set [index] of [target] to [set] (changed it or took over its advice): from now on that set is yours. */
    fun change(target: OwnSets, index: Int, set: PlannedSet): OwnSets {
        if (index !in target.sets.indices) return target
        return OwnSets(
            target.sets.mapIndexed { j, s -> if (j == index) set else s },
            target.yours.mapIndexed { j, y -> y || j == index },
        )
    }

    /**
     * Ongedaan (#92): set [index] is no longer yours and follows your performance again, as the [default] target gives
     * it. The other sets stay as they are.
     */
    fun release(target: OwnSets, index: Int, default: List<PlannedSet>): OwnSets {
        if (index !in target.sets.indices) return target
        return follow(OwnSets(target.sets, target.yours.mapIndexed { j, y -> y && j != index }), default)
    }

    /** The origin of the target as a whole: yours when you set at least one set yourself. */
    fun originOf(sets: List<TargetOrigin>): TargetOrigin = when {
        TargetOrigin.YOURS in sets -> TargetOrigin.YOURS
        TargetOrigin.DEFAULT in sets || sets.isEmpty() -> TargetOrigin.DEFAULT
        else -> TargetOrigin.UNKNOWN
    }
}
