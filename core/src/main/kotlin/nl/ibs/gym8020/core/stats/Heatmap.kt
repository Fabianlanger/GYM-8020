package nl.ibs.gym8020.core.stats

import nl.ibs.gym8020.core.logic.Change
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression

/**
 * Muscle heatmap after a training (D-40, D-41):
 * - total: volume per muscle = Σ effective weight × reps × share (warm-ups excluded);
 * - progress: per exercise the heavy-set e1RM (or max reps for reps progression) compared to the previous
 *   time that exercise was done (D-42), averaged per muscle weighted by share.
 */
object Heatmap {

    fun volume(sets: List<SetRecord>, muscles: Map<Long, List<MuscleShare>>): Map<Muscle, Double> =
        Strength.volumePerMuscle(sets, muscles)

    /** Performance score of one exercise in one training: heavy e1RM, or max reps for reps progression. */
    fun score(sets: List<SetRecord>): Double? {
        val work = sets.filter { it.role.isWork && (it.reps ?: 0) > 0 }
        if (work.isEmpty()) return null
        val repsMode = work.first().progression == Progression.REPS && work.all { (it.weightKg ?: 0.0) == 0.0 }
        if (repsMode) return work.maxOf { it.reps!! }.toDouble()
        return Strength.of(work)
    }

    /**
     * Relative change per exercise (e.g. 0.04 = +4 %) between this training and the previous time.
     * [previous] holds the sets of the previous time per exercise.
     */
    fun exerciseChange(current: List<SetRecord>, previous: Map<Long, List<SetRecord>>): Map<Long, Double> =
        current.groupBy { it.exerciseId }.mapNotNull { (id, sets) ->
            val now = score(sets) ?: return@mapNotNull null
            val before = previous[id]?.let(::score) ?: return@mapNotNull null
            Change.between(before, now)?.relative?.let { id to it }
        }.toMap()

    fun progress(changes: Map<Long, Double>, muscles: Map<Long, List<MuscleShare>>): Map<Muscle, Double> {
        val sum = HashMap<Muscle, Double>()
        val weight = HashMap<Muscle, Double>()
        for ((id, c) in changes) {
            for (m in muscles[id].orEmpty()) {
                val w = m.percent / 100.0
                sum[m.muscle] = (sum[m.muscle] ?: 0.0) + c * w
                weight[m.muscle] = (weight[m.muscle] ?: 0.0) + w
            }
        }
        return sum.mapValues { (m, s) -> s / weight.getValue(m) }
    }
}
