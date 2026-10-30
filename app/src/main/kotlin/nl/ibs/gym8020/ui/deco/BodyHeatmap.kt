package nl.ibs.gym8020.ui.deco

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.stats.Comparison
import nl.ibs.gym8020.ui.theme.Gym
import kotlin.math.abs
import kotlin.math.min

/** How muscle values are colored. */
enum class HeatScale {
    /** Magnitude: dark → IBS yellow (values normalized to the maximum). */
    SEQUENTIAL,

    /** Change: red (worse) — grey (same, within ±1 %) — green (better), saturating at ±10 %. */
    DIVERGING,
}

fun heatColor(value: Double?, scale: HeatScale, max: Double): Color = when {
    value == null -> Gym.SurfaceHigh
    scale == HeatScale.SEQUENTIAL -> if (value <= 0 || max <= 0) Gym.SurfaceHigh else lerp(Gym.SurfaceHigh, Gym.Yellow, (0.2 + 0.8 * value / max).toFloat().coerceIn(0f, 1f))
    abs(value) <= Comparison.SAME_MARGIN + 1e-9 -> Gym.Muted
    value > 0 -> lerp(Gym.SurfaceHigh, Gym.Positive, (0.3 + 0.7 * min(value / 0.1, 1.0)).toFloat())
    else -> lerp(Gym.SurfaceHigh, Gym.Negative, (0.3 + 0.7 * min(-value / 0.1, 1.0)).toFloat())
}

private fun inside(p: Offset, poly: List<Pair<Float, Float>>): Boolean {
    var c = false
    var j = poly.lastIndex
    for (i in poly.indices) {
        val (xi, yi) = poly[i]
        val (xj, yj) = poly[j]
        if ((yi > p.y) != (yj > p.y) && p.x < (xj - xi) * (p.y - yi) / (yj - yi) + xi) c = !c
        j = i
    }
    return c
}

/** Diagonal stripes over the whole draw area: the "skipped" state (#67). Clip before drawing. */
fun DrawScope.drawHatch(spacing: Float, strokeWidth: Float) {
    var x = -size.height
    while (x < size.width) {
        drawLine(Gym.Muted, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth)
        x += spacing
    }
}

/** Legend swatch for the "skipped" state, the same stripes as on the body. */
@Composable
fun HatchSwatch(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Gym.SurfaceHigh)
        clipRect { drawHatch(spacing = size.width / 2.5f, strokeWidth = size.width / 8f) }
    }
}

/**
 * One view (front or back) of the body with a color per muscle; tap a muscle to select it.
 * [hatched] muscles get stripes instead of a color: only trained by exercises you skipped (#67).
 */
@Composable
fun BodyFigure(
    shapes: List<BodyShape>,
    values: Map<Muscle, Double>,
    scale: HeatScale,
    selected: Muscle?,
    onSelect: (Muscle?) -> Unit,
    modifier: Modifier = Modifier,
    hatched: Set<Muscle> = emptySet(),
) {
    val max = values.values.maxOrNull() ?: 0.0
    Canvas(
        modifier.aspectRatio(100f / 212f).pointerInput(shapes) {
            detectTapGestures { tap ->
                val s = min(size.width / 100f, size.height / 212f)
                val p = Offset(tap.x / s, tap.y / s)
                onSelect(shapes.lastOrNull { it.muscle != null && inside(p, it.points) }?.muscle)
            }
        },
    ) {
        val s = min(size.width / 100f, size.height / 212f)
        for (shape in shapes) {
            val path = Path().apply {
                shape.points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * s, y * s) else lineTo(x * s, y * s) }
                close()
            }
            val muscle: Muscle? = shape.muscle
            val isHatched = muscle != null && hatched.contains(muscle)
            val fill = when {
                muscle == null -> Gym.Surface
                isHatched -> Gym.SurfaceHigh
                else -> heatColor(values[muscle], scale, max)
            }
            drawPath(path, fill)
            if (isHatched) clipPath(path) { drawHatch(spacing = 3.2f * s, strokeWidth = .7f * s) }
            val isSel = muscle != null && muscle == selected
            drawPath(
                path, if (isSel) Gym.Cream else Gym.Gold.copy(alpha = .75f),
                style = Stroke(width = (if (isSel) 1.6f else .6f) * s, join = StrokeJoin.Round),
            )
        }
    }
}
