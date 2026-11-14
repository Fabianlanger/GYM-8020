package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.SetRole
import java.time.LocalDate

/**
 * An earlier training of one exercise: its training date and its sets in order, and for a bodyweight exercise the
 * body weight of that training (#87).
 */
data class PastSession(val date: LocalDate, val sets: List<PerformedSet>, val skipped: Boolean = false, val bodyweightKg: Double? = null)

/** The last time of one set: what you did, on which training date, and with which body weight. */
data class LastSet(val set: PerformedSet, val date: LocalDate, val bodyweightKg: Double? = null) {
    /**
     * The weight you moved (#87, ADR 0002): for a bodyweight exercise the effective weight with the body weight of
     * that training (an empty field is bodyweight only), so it compares fairly with today; null when unknown.
     */
    fun effectiveKg(loadType: LoadType, factor: Double): Double? {
        val entered = set.weightKg ?: if (loadType == LoadType.BODYWEIGHT) 0.0 else return null
        return EffectiveWeight.of(loadType, entered, bodyweightKg, factor)
    }
}

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
                s.sets.filter { it.role == role }.getOrNull(k)?.takeIf { it.reps != null }?.let { LastSet(it, s.date, s.bodyweightKg) }
            }
        }
    }

    /** The one date of all [lastSets] when they share it (shown once), else null (a date per set). */
    fun sharedDate(lastSets: List<LastSet?>): LocalDate? =
        lastSets.mapNotNull { it?.date }.distinct().singleOrNull()
}
