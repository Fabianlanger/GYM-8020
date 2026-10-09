package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.SetRecord

/*
 * The strength model (A-3, #12): every formula for how strong you are and how much work you did lives here,
 * so the summary, dashboard, records and heatmap always show the same number.
 */

object EffectiveWeight {
    /**
     * The weight actually moved (GLOSSARY "effectief gewicht", ADR 0001).
     * For [LoadType.EXTERNAL] it is the entered weight. For [LoadType.BODYWEIGHT], [input] is the signed load
     * (negative = assistance, 0 = bodyweight only), [bodyweightKg] the body weight on the day of the set and
     * [factor] the exercise's bodyweight share; unknown without a body weight.
     */
    fun of(loadType: LoadType, input: Double, bodyweightKg: Double?, factor: Double): Double? =
        when (loadType) {
            LoadType.EXTERNAL -> input
            LoadType.BODYWEIGHT -> bodyweightKg?.let { it * factor + input }
        }
}

object Strength {
    /**
     * Estimated one-rep max with Epley: `weight × (1 + reps / 30)`, and one rep is the weight itself
     * (D-41, SPEC "Vooruitgang-kaart"). Epley is reliable up to ~10–12 reps, which is why strength uses heavy
     * sets only ([of]). [weightKg] is the effective weight. Returns 0 for no reps.
     */
    fun e1rm(weightKg: Double, reps: Int): Double =
        if (reps <= 0) 0.0 else if (reps == 1) weightKg else weightKg * (1 + reps / 30.0)

    /** [e1rm] of a set, or null when its weight or reps are missing (or there are no reps). */
    fun e1rmOrNull(weightKg: Double?, reps: Int?): Double? =
        if (weightKg == null || reps == null || reps <= 0) null else e1rm(weightKg, reps)

    /** [e1rm] of a performed set, on its effective weight. */
    fun e1rm(set: SetRecord): Double? = e1rmOrNull(set.effectiveKg, set.reps)

    /**
     * The heavy set that shows your strength best: highest e1RM; on a tie the latest date.
     * Warm-ups and light sets never count (D-41); null without a heavy set with weight and reps.
     */
    fun bestSet(sets: List<SetRecord>): SetRecord? =
        sets.filter { it.role == SetRole.HEAVY && e1rm(it) != null }
            .maxWithOrNull(compareBy<SetRecord> { e1rm(it)!! }.thenBy { it.date })

    /** Strength of one exercise in one session: the best heavy-set e1RM, see [bestSet]. */
    fun of(sets: List<SetRecord>): Double? = bestSet(sets)?.let(::e1rm)

    /** Volume of a set: moved weight × reps × muscle share. */
    fun volume(weightKg: Double, reps: Int, percent: Int = 100): Double = weightKg * reps * percent / 100.0

    /** Volume of a performed set: effective weight × reps; 0 for a warm-up or when weight or reps are missing. */
    fun volume(set: SetRecord): Double = if (countsForVolume(set)) volume(set.effectiveKg!!, set.reps!!) else 0.0

    private fun countsForVolume(set: SetRecord) = set.role.isWork && set.effectiveKg != null && set.reps != null

    /** Total volume of the work sets (GLOSSARY "volume"); warm-ups do not count. */
    fun volume(sets: List<SetRecord>): Double = sets.sumOf(::volume)

    /** Volume per muscle, each set counting for the muscle's share of its exercise. Untouched muscles are absent. */
    fun volumePerMuscle(sets: List<SetRecord>, muscles: Map<Long, List<MuscleShare>>): Map<Muscle, Double> {
        val out = HashMap<Muscle, Double>()
        for (s in sets) {
            if (!countsForVolume(s)) continue
            val v = volume(s)
            for (m in muscles[s.exerciseId].orEmpty()) out[m.muscle] = (out[m.muscle] ?: 0.0) + v * m.percent / 100.0
        }
        return out
    }
}

/**
 * A result next to an earlier one (GLOSSARY "vergelijking"): always absolute and relative,
 * e.g. "+2,5 kg · +2,4%" is `Change(2.5, 0.024)`.
 */
data class Change(
    /** now − before, in the unit of the values (kg, reps, sets). */
    val absolute: Double,
    /** Change as a fraction of before (0.024 = +2.4 %); null when before is not positive. */
    val relative: Double?,
) {
    companion object {
        /** Null when either value is unknown; the caller decides whether something missing counts as 0. */
        fun between(before: Double?, now: Double?): Change? {
            if (before == null || now == null) return null
            return Change(now - before, if (before > 0) (now - before) / before else null)
        }
    }
}
