package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import kotlin.math.abs
import kotlin.math.roundToLong

object EffectiveWeight {
    /**
     * The weight actually moved.
     * For [LoadType.BODYWEIGHT], [input] is the signed extra weight (negative = assistance)
     * and [bodyweightKg] the body weight at the time of the set.
     */
    fun of(loadType: LoadType, input: Double, bodyweightKg: Double?, factor: Double): Double? =
        when (loadType) {
            LoadType.EXTERNAL -> input
            LoadType.BODYWEIGHT -> bodyweightKg?.let { it * factor + input }
        }
}

object Strength {
    /** Estimated one-rep max (Epley). Reliable up to ~10–12 reps; used on heavy sets only. */
    fun e1rm(weightKg: Double, reps: Int): Double =
        if (reps <= 0) 0.0 else if (reps == 1) weightKg else weightKg * (1 + reps / 30.0)

    /** Volume of a set: moved weight × reps × muscle share. */
    fun volume(weightKg: Double, reps: Int, percent: Int = 100): Double = weightKg * reps * percent / 100.0
}

enum class WeightUnit(val perKg: Double) {
    KG(1.0), LBS(2.2046226218);

    fun fromKg(kg: Double): Double = kg * perKg
    fun toKg(value: Double): Double = value / perKg
}

object WeightFormat {
    /**
     * Formats a weight without trailing zeros, e.g. 60 → "60", 62.5 → "62,5".
     * [signed] adds a leading "+" for positive values (bodyweight extra).
     */
    fun format(kg: Double, unit: WeightUnit = WeightUnit.KG, signed: Boolean = false, decimalComma: Boolean = true): String {
        val v = unit.fromKg(kg)
        val rounded = (v * 100).roundToLong() / 100.0
        val whole = rounded == rounded.toLong().toDouble()
        var text = if (whole) abs(rounded).toLong().toString() else trimZeros("%.2f".format(java.util.Locale.ROOT, abs(rounded)))
        if (decimalComma) text = text.replace('.', ',')
        val sign = when {
            rounded < 0 -> "−"
            signed && rounded > 0 -> "+"
            else -> ""
        }
        return sign + text
    }

    private fun trimZeros(s: String): String = s.trimEnd('0').trimEnd('.')
}
