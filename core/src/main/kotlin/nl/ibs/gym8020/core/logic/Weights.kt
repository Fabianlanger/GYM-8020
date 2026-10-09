package nl.ibs.gym8020.core.logic

import kotlin.math.abs
import kotlin.math.roundToLong

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
