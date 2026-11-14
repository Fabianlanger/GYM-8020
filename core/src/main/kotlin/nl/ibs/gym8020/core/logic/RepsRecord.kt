package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.stats.SetRecord

/**
 * A record on reps (#73), for an exercise with reps progression: the most reps in one work set, at a signed load at
 * least as heavy as the previous record's; more reps with more assistance does not count. [loadKg] is the signed
 * load (an empty field is 0), [effectiveKg] the weight you moved (null when unknown), [improvement] the reps more
 * than the previous record. Exercises on weight keep their record on e1RM ([Strength]).
 */
data class RepsRecord(val reps: Int, val loadKg: Double, val effectiveKg: Double?, val improvement: Int) {
    companion object {
        /** Today's record against the [earlier] trainings of the exercise (oldest first); null when it is none. */
        fun of(today: List<SetRecord>, earlier: List<List<SetRecord>>): RepsRecord? {
            val record = earlier.fold(null as SetRecord?) { held, sets -> best(sets, held)?.takeIf { held == null || it.reps!! > held.reps!! } ?: held }
                ?: return null
            val now = best(today, record) ?: return null
            if (now.reps!! <= record.reps!!) return null
            return RepsRecord(now.reps, load(now), now.effectiveKg, now.reps - record.reps)
        }

        /** The work set with the most reps at a load at least as heavy as [record]'s; on equal reps the heavier. */
        private fun best(sets: List<SetRecord>, record: SetRecord?): SetRecord? =
            sets.filter { it.role.isWork && (it.reps ?: 0) > 0 && (record == null || load(it) >= load(record) - 1e-9) }
                .maxWithOrNull(compareBy<SetRecord>({ it.reps }, { load(it) }))

        private fun load(set: SetRecord): Double = set.weightKg ?: 0.0
    }
}
