package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.LoadType

/**
 * Warm-up for heavy bodyweight exercises (B-4): pull-ups, chin-ups and dips with only your body weight are
 * already a hard set, so they warm up with assistance. Push-ups and the like keep "bodyweight only".
 */
object Warmup {
    /** Default assistance as a share of the moved body weight (Settings, D-83). */
    const val DEFAULT_ASSIST_PERCENT = 50

    /** Exercises that move (nearly) your full body weight. */
    fun needsAssistance(loadType: LoadType, bodyweightFactor: Double): Boolean =
        loadType == LoadType.BODYWEIGHT && bodyweightFactor >= 0.9

    /**
     * Assistance for the warm-up as a signed extra weight (negative), e.g. -40.0. Rounded to the assisted
     * machine's steps when [calc] is such a profile. Null without a known body weight.
     */
    fun assistKg(bodyweightKg: Double?, bodyweightFactor: Double, percent: Int, calc: WeightCalculator?): Double? {
        val bw = bodyweightKg ?: return null
        val raw = -(bw * bodyweightFactor * percent / 100.0)
        if (calc == null || calc.profile.kind != MaterialKind.BODYWEIGHT) return Math.round(raw * 10) / 10.0
        val assisted = calc.achievable.filter { it < 0 }
        if (assisted.isEmpty()) return Math.round(raw * 10) / 10.0
        return assisted.minBy { kotlin.math.abs(it - raw) }
    }
}
