package nl.ibs.gym8020.core.logic

/**
 * A session exercise as the backfill sees it: its training (id, start, finished), whether it was skipped, and its
 * session target per set in order (role, `goalWeightKg`, `goalReps`).
 */
data class BackfillEntry(
    val sessionExerciseId: Long,
    val exerciseId: Long,
    val sessionId: Long,
    val startedAt: Long,
    val finished: Boolean,
    val skipped: Boolean,
    val sessionTarget: List<PlannedSet>,
)

/** A target for next time derived for a session exercise that had none stored. Its origin is "unknown". */
sealed interface Backfilled {
    val sessionExerciseId: Long
}

/** Derived from the session target of the next training with the same exercise. */
data class FromNextSession(override val sessionExerciseId: Long, val sets: List<PlannedSet>) : Backfilled

/** The latest finished training of an exercise: its target for next time is the current target. */
data class FromCurrentTarget(override val sessionExerciseId: Long, val exerciseId: Long) : Backfilled

/**
 * Schema v3 (#11, owner decision Q3): before v3 only the current target was kept. The target for next time of an
 * older training is what the next training with that exercise aimed at: its session target. Nothing is invented:
 * a next training without a session target gives no target, and only work sets with a session target count.
 */
object TargetBackfill {
    fun derive(entries: List<BackfillEntry>, withTarget: Set<Long>): List<Backfilled> {
        val order = compareBy<BackfillEntry>({ it.startedAt }, { it.sessionId })
        val out = mutableListOf<Backfilled>()
        for ((exerciseId, list) in entries.groupBy { it.exerciseId }) {
            val sorted = list.sortedWith(order)
            for (entry in sorted) {
                if (entry.sessionExerciseId in withTarget) continue
                val next = sorted.firstOrNull { order.compare(it, entry) > 0 && it.sessionId != entry.sessionId }
                if (next != null) {
                    val sets = next.sessionTarget.filter { it.role.isWork && (it.weightKg != null || it.reps != null) }
                    if (sets.isNotEmpty()) out += FromNextSession(entry.sessionExerciseId, sets)
                } else if (entry.finished && !entry.skipped) {
                    out += FromCurrentTarget(entry.sessionExerciseId, exerciseId)
                }
            }
        }
        return out
    }
}
