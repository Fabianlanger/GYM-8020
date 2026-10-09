package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.SetRole
import java.time.LocalDate

/** An earlier training of one exercise: its training date and its sets in order. */
data class PastSession(val date: LocalDate, val sets: List<PerformedSet>, val skipped: Boolean = false)

/** The last time of one set: what you did and on which training date. */
data class LastSet(val set: PerformedSet, val date: LocalDate)

/**
 * "Laatste keer" (#65, decision #7): per set the most recent performance of the exercise, from any workout,
 * by training date. A set you skipped does not count; then the time before applies.
 */
object LastTime {

    /** One entry per set in [roles]; null for warm-ups and for sets with no earlier performance. */
    fun perSet(roles: List<SetRole>, history: List<PastSession>): List<LastSet?> {
        val newestFirst = history.filter { !it.skipped }.sortedByDescending { it.date }
        return roles.mapIndexed { i, role ->
            if (!role.isWork) return@mapIndexed null
            val k = roles.take(i).count { it == role }
            newestFirst.firstNotNullOfOrNull { s ->
                s.sets.filter { it.role == role }.getOrNull(k)?.takeIf { it.reps != null }?.let { LastSet(it, s.date) }
            }
        }
    }

    /** The one date of all [lastSets] when they share it (shown once), else null (a date per set). */
    fun sharedDate(lastSets: List<LastSet?>): LocalDate? =
        lastSets.mapNotNull { it?.date }.distinct().singleOrNull()
}
