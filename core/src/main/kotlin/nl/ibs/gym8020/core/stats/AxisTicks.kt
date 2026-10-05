package nl.ibs.gym8020.core.stats

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Axis values for charts (V-13): round numbers that cover the data, e.g. 79 80 81 82 83 kg. */
data class AxisTicks(val values: List<Double>, val step: Double, val decimals: Int) {
    companion object {
        /** About [target] intervals between [lo] and [hi], with a step of 1, 2, 2.5 or 5 × 10^n. */
        fun nice(lo: Double, hi: Double, target: Int = 4): AxisTicks {
            var a = minOf(lo, hi)
            var b = maxOf(lo, hi)
            if (b - a < 1e-9) { a -= 1.0; b += 1.0 }
            val raw = (b - a) / target.coerceAtLeast(1)
            val magnitude = 10.0.pow(floor(log10(raw)))
            val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= raw - 1e-12 }
            val start = floor(a / step + 1e-9) * step
            val end = ceil(b / step - 1e-9) * step
            val count = ((end - start) / step + 0.5).toInt()
            val values = (0..count).map { round(start + it * step) }
            fun whole(x: Double) = kotlin.math.abs(x - kotlin.math.round(x)) < 1e-9
            val decimals = when {
                whole(step) -> 0
                whole(step * 10) -> 1
                else -> 2
            }
            return AxisTicks(values, step, decimals)
        }

        private fun round(v: Double): Double = kotlin.math.round(v * 1e6) / 1e6
    }
}
