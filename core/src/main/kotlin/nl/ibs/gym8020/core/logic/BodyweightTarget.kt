package nl.ibs.gym8020.core.logic

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * The part of your body weight a bodyweight exercise moves on one day: body weight × the exercise's share
 * (GLOSSARY "effectief gewicht", ADR 0001). The signed load you enter comes on top of it.
 */
data class BodyShare(val bodyweightKg: Double, val factor: Double) {
    private val kg: Double get() = bodyweightKg * factor

    /** The effective weight of [signedKg] on this day. */
    fun effective(signedKg: Double): Double = grams(kg + signedKg)

    /** The signed load that moves [effectiveKg] on this day, exact (not rounded to the material). */
    fun signed(effectiveKg: Double): Double = grams(effectiveKg - kg)

    /**
     * What you hang or need as assistance today for the target [effectiveKg] (#87): the signed load, rounded by
     * [snap] to what the material allows (plates on the belt, steps of the assistance).
     */
    fun today(effectiveKg: Double, snap: (Double) -> Double): Double = snap(signed(effectiveKg))
}

/** Whole grams, so converting back and forth never drifts. */
private fun grams(kg: Double): Double = (kg * 1000).roundToLong() / 1000.0

/**
 * The target of a bodyweight exercise is an effective weight (ADR 0002, #87): it is stored as such, so a change of
 * your body weight does not silently change how heavy you train. On the training day it is converted to the signed
 * load of that day; everything that compares within a training (default target, advice) then works on that.
 */
object BodyweightTarget {

    /** The work sets' signed loads as effective weights on the day of [share]; unchanged without a body weight. */
    fun toEffective(sets: List<PlannedSet>, share: BodyShare?): List<PlannedSet> = convert(sets, share, BodyShare::effective)

    /** The work sets' effective weights as signed loads on the day of [share]; unchanged without a body weight. */
    fun toSigned(sets: List<PlannedSet>, share: BodyShare?): List<PlannedSet> = convert(sets, share, BodyShare::signed)

    /**
     * The session target (#87): each work set of the stored [target] (effective) as what you hang today, rounded by
     * [snap] to the material; unchanged without a body weight.
     */
    fun forToday(target: List<PlannedSet>, share: BodyShare?, snap: (Double) -> Double): List<PlannedSet> =
        convert(target, share) { s, kg -> s.today(kg, snap) }

    private fun convert(sets: List<PlannedSet>, share: BodyShare?, f: (BodyShare, Double) -> Double): List<PlannedSet> =
        if (share == null) sets else sets.map { s -> s.weightKg?.takeIf { s.role.isWork }?.let { s.copy(weightKg = f(share, it)) } ?: s }

    /**
     * The body weight of a training on [epochDay]: the one it kept ([sessionKg]), else the last measurement on or
     * before that day, else the first measurement there is; null when none is known.
     */
    fun bodyweightFor(sessionKg: Double?, measurements: List<Measurement>, epochDay: Long): Double? =
        sessionKg
            ?: BodyweightLog.lastOnOrBefore(measurements, epochDay)?.kg
            ?: measurements.minWithOrNull(compareBy({ it.epochDay }, { it.measuredAt }))?.kg

    /**
     * Rounding without material: the nearest multiple of [stepKg] (the weight step from the settings), on a tie
     * the lighter one. A step of 0 or less does not round.
     */
    fun roundToStep(kg: Double, stepKg: Double): Double {
        if (stepKg <= 0) return kg
        val below = floor(kg / stepKg + 1e-9) * stepKg
        val above = below + stepKg
        return grams(if (abs(above - kg) < abs(kg - below) - 1e-9) above else below)
    }
}
