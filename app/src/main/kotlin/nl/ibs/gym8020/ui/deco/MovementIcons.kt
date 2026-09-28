package nl.ibs.gym8020.ui.deco

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.ui.theme.Gym

/*
 * Movement pictograms (48×48 grid): cream figure, gold equipment, yellow plates and dumbbells.
 * Generated from the same data as the design preview; edit both together.
 */

private sealed interface Prim
private class Figure(val pts: List<Pair<Float, Float>>) : Prim
private class Equip(val pts: List<Pair<Float, Float>>) : Prim
private class Head(val x: Float, val y: Float) : Prim
private class Disc(val x: Float, val y: Float, val r: Float) : Prim
private class Bell(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : Prim
private class Arrow(val x: Float, val y1: Float, val y2: Float) : Prim
private data object Floor : Prim

private val icons: Map<Movement, List<Prim>> = mapOf(
    Movement.SQUAT to listOf(
        Floor,
        Equip(listOf(5.0f to 15.0f, 43.0f to 15.0f)),
        Disc(6.0f, 15.0f, 5.5f),
        Disc(42.0f, 15.0f, 5.5f),
        Head(24.0f, 9.0f),
        Figure(listOf(14.0f to 15.0f, 18.0f to 19.0f, 24.0f to 17.0f, 30.0f to 19.0f, 34.0f to 15.0f)),
        Figure(listOf(24.0f to 17.0f, 24.0f to 28.0f)),
        Figure(listOf(24.0f to 28.0f, 12.0f to 31.0f, 15.0f to 43.0f)),
        Figure(listOf(24.0f to 28.0f, 36.0f to 31.0f, 33.0f to 43.0f)),
    ),
    Movement.HINGE to listOf(
        Floor,
        Head(35.0f, 14.0f),
        Figure(listOf(31.0f to 17.0f, 16.0f to 22.0f)),
        Figure(listOf(16.0f to 22.0f, 20.0f to 32.0f, 17.0f to 43.0f)),
        Figure(listOf(29.0f to 18.0f, 27.0f to 33.0f)),
        Disc(27.0f, 36.0f, 7.0f),
    ),
    Movement.LUNGE to listOf(
        Floor,
        Head(24.0f, 8.0f),
        Figure(listOf(24.0f to 12.0f, 24.0f to 27.0f)),
        Figure(listOf(24.0f to 27.0f, 33.0f to 34.0f, 33.0f to 43.0f)),
        Figure(listOf(24.0f to 27.0f, 18.0f to 37.0f, 9.0f to 41.0f)),
        Figure(listOf(24.0f to 14.0f, 28.0f to 25.0f)),
        Bell(26.0f, 27.0f, 32.0f, 27.0f),
    ),
    Movement.LEG_MACHINE to listOf(
        Equip(listOf(8.0f to 31.0f, 28.0f to 31.0f)),
        Equip(listOf(8.0f to 31.0f, 6.0f to 13.0f)),
        Head(11.0f, 9.0f),
        Figure(listOf(11.0f to 13.0f, 13.0f to 28.0f)),
        Figure(listOf(13.0f to 28.0f, 27.0f to 28.0f, 40.0f to 21.0f)),
        Figure(listOf(12.0f to 17.0f, 19.0f to 26.0f)),
        Disc(42.0f, 20.0f, 3.2f),
    ),
    Movement.CALF to listOf(
        Equip(listOf(20.0f to 43.0f, 38.0f to 43.0f, 38.0f to 47.0f)),
        Head(24.0f, 7.0f),
        Figure(listOf(24.0f to 11.0f, 24.0f to 27.0f)),
        Figure(listOf(24.0f to 27.0f, 22.0f to 38.0f, 26.0f to 43.0f)),
        Figure(listOf(24.0f to 27.0f, 26.0f to 38.0f, 30.0f to 43.0f)),
        Figure(listOf(24.0f to 13.0f, 20.0f to 25.0f)),
        Figure(listOf(24.0f to 13.0f, 28.0f to 25.0f)),
        Arrow(10.0f, 26.0f, 12.0f),
    ),
    Movement.HORIZONTAL_PUSH to listOf(
        Equip(listOf(8.0f to 33.0f, 40.0f to 33.0f)),
        Equip(listOf(12.0f to 33.0f, 12.0f to 40.0f)),
        Equip(listOf(36.0f to 33.0f, 36.0f to 40.0f)),
        Head(38.0f, 27.0f),
        Figure(listOf(34.0f to 29.0f, 18.0f to 29.0f)),
        Figure(listOf(18.0f to 29.0f, 11.0f to 35.0f, 11.0f to 44.0f)),
        Figure(listOf(31.0f to 29.0f, 31.0f to 15.0f)),
        Disc(31.0f, 12.0f, 6.0f),
    ),
    Movement.VERTICAL_PUSH to listOf(
        Floor,
        Equip(listOf(5.0f to 7.0f, 43.0f to 7.0f)),
        Disc(6.0f, 7.0f, 5.0f),
        Disc(42.0f, 7.0f, 5.0f),
        Head(24.0f, 17.0f),
        Figure(listOf(15.0f to 7.0f, 13.0f to 15.0f, 19.0f to 22.0f, 24.0f to 21.0f, 29.0f to 22.0f, 35.0f to 15.0f, 33.0f to 7.0f)),
        Figure(listOf(24.0f to 21.0f, 24.0f to 33.0f)),
        Figure(listOf(24.0f to 33.0f, 20.0f to 44.0f)),
        Figure(listOf(24.0f to 33.0f, 28.0f to 44.0f)),
    ),
    Movement.FLY to listOf(
        Equip(listOf(4.0f to 5.0f, 6.0f to 22.0f)),
        Equip(listOf(44.0f to 5.0f, 42.0f to 22.0f)),
        Head(24.0f, 11.0f),
        Figure(listOf(6.0f to 22.0f, 11.0f to 17.0f, 24.0f to 16.0f, 37.0f to 17.0f, 42.0f to 22.0f)),
        Figure(listOf(24.0f to 16.0f, 24.0f to 31.0f)),
        Figure(listOf(24.0f to 31.0f, 18.0f to 33.0f, 17.0f to 42.0f)),
        Figure(listOf(24.0f to 31.0f, 30.0f to 33.0f, 31.0f to 42.0f)),
        Disc(6.0f, 22.0f, 2.2f),
        Disc(42.0f, 22.0f, 2.2f),
    ),
    Movement.DIP to listOf(
        Equip(listOf(8.0f to 20.0f, 16.0f to 20.0f)),
        Equip(listOf(32.0f to 20.0f, 40.0f to 20.0f)),
        Equip(listOf(12.0f to 20.0f, 12.0f to 45.0f)),
        Equip(listOf(36.0f to 20.0f, 36.0f to 45.0f)),
        Head(24.0f, 9.0f),
        Figure(listOf(13.0f to 20.0f, 18.0f to 15.0f, 24.0f to 14.0f, 30.0f to 15.0f, 35.0f to 20.0f)),
        Figure(listOf(24.0f to 14.0f, 24.0f to 28.0f)),
        Figure(listOf(24.0f to 28.0f, 21.0f to 36.0f, 26.0f to 41.0f)),
        Figure(listOf(24.0f to 28.0f, 27.0f to 36.0f, 31.0f to 40.0f)),
    ),
    Movement.RAISE to listOf(
        Floor,
        Head(24.0f, 8.0f),
        Figure(listOf(6.0f to 15.0f, 24.0f to 14.0f, 42.0f to 15.0f)),
        Figure(listOf(24.0f to 14.0f, 24.0f to 29.0f)),
        Figure(listOf(24.0f to 29.0f, 21.0f to 44.0f)),
        Figure(listOf(24.0f to 29.0f, 27.0f to 44.0f)),
        Bell(5.0f, 11.0f, 5.0f, 19.0f),
        Bell(43.0f, 11.0f, 43.0f, 19.0f),
    ),
    Movement.VERTICAL_PULL to listOf(
        Equip(listOf(6.0f to 5.0f, 42.0f to 5.0f)),
        Equip(listOf(6.0f to 5.0f, 6.0f to 10.0f)),
        Equip(listOf(42.0f to 5.0f, 42.0f to 10.0f)),
        Head(24.0f, 13.0f),
        Figure(listOf(14.0f to 5.0f, 13.0f to 14.0f, 24.0f to 18.0f, 35.0f to 14.0f, 34.0f to 5.0f)),
        Figure(listOf(24.0f to 18.0f, 24.0f to 31.0f)),
        Figure(listOf(24.0f to 31.0f, 20.0f to 40.0f, 22.0f to 44.0f)),
        Figure(listOf(24.0f to 31.0f, 28.0f to 40.0f, 26.0f to 44.0f)),
    ),
    Movement.HORIZONTAL_PULL to listOf(
        Equip(listOf(6.0f to 33.0f, 20.0f to 33.0f)),
        Equip(listOf(38.0f to 25.0f, 38.0f to 39.0f)),
        Equip(listOf(22.0f to 22.0f, 44.0f to 22.0f)),
        Head(12.0f, 9.0f),
        Figure(listOf(13.0f to 13.0f, 15.0f to 30.0f)),
        Figure(listOf(15.0f to 30.0f, 27.0f to 31.0f, 37.0f to 31.0f)),
        Figure(listOf(14.0f to 16.0f, 8.0f to 22.0f, 20.0f to 22.0f)),
        Disc(21.0f, 22.0f, 1.8f),
    ),
    Movement.CURL to listOf(
        Floor,
        Head(22.0f, 7.0f),
        Figure(listOf(22.0f to 11.0f, 22.0f to 28.0f)),
        Figure(listOf(22.0f to 28.0f, 20.0f to 44.0f)),
        Figure(listOf(22.0f to 28.0f, 26.0f to 44.0f)),
        Figure(listOf(22.0f to 13.0f, 22.0f to 23.0f, 31.0f to 15.0f)),
        Bell(29.0f, 12.0f, 34.0f, 17.0f),
    ),
    Movement.TRICEPS to listOf(
        Floor,
        Equip(listOf(32.0f to 3.0f, 32.0f to 31.0f)),
        Equip(listOf(28.0f to 3.0f, 36.0f to 3.0f)),
        Head(18.0f, 7.0f),
        Figure(listOf(18.0f to 11.0f, 17.0f to 28.0f)),
        Figure(listOf(17.0f to 28.0f, 15.0f to 44.0f)),
        Figure(listOf(17.0f to 28.0f, 21.0f to 44.0f)),
        Figure(listOf(18.0f to 13.0f, 20.0f to 23.0f, 31.0f to 32.0f)),
        Disc(32.0f, 32.0f, 2.2f),
    ),
    Movement.SHRUG to listOf(
        Floor,
        Head(24.0f, 12.0f),
        Figure(listOf(16.0f to 17.0f, 24.0f to 16.0f, 32.0f to 17.0f)),
        Figure(listOf(24.0f to 16.0f, 24.0f to 31.0f)),
        Figure(listOf(16.0f to 17.0f, 14.0f to 30.0f)),
        Figure(listOf(32.0f to 17.0f, 34.0f to 30.0f)),
        Bell(11.0f, 32.0f, 17.0f, 32.0f),
        Bell(31.0f, 32.0f, 37.0f, 32.0f),
        Figure(listOf(24.0f to 31.0f, 21.0f to 44.0f)),
        Figure(listOf(24.0f to 31.0f, 27.0f to 44.0f)),
        Arrow(10.0f, 14.0f, 4.0f),
        Arrow(38.0f, 14.0f, 4.0f),
    ),
    Movement.CORE to listOf(
        Equip(listOf(4.0f to 42.0f, 44.0f to 42.0f)),
        Figure(listOf(24.0f to 40.0f, 31.0f to 29.0f, 39.0f to 40.0f)),
        Figure(listOf(24.0f to 40.0f, 15.0f to 31.0f)),
        Head(12.0f, 27.0f),
        Figure(listOf(16.0f to 30.0f, 11.0f to 22.0f, 10.0f to 25.0f)),
    ),
)

@Composable
fun MovementIcon(movement: Movement, modifier: Modifier = Modifier, figure: Color = Gym.Cream) {
    Canvas(modifier.aspectRatio(1f)) {
        val s = size.minDimension / 48f
        icons[movement].orEmpty().forEach { drawPrim(it, s, figure) }
    }
}

private fun DrawScope.poly(pts: List<Pair<Float, Float>>, s: Float, color: Color, width: Float) {
    val path = Path()
    pts.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x * s, y * s) else path.lineTo(x * s, y * s) }
    drawPath(path, color, style = Stroke(width = width * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawPrim(p: Prim, s: Float, figure: Color) {
    when (p) {
        is Figure -> poly(p.pts, s, figure, 2.4f)
        is Equip -> poly(p.pts, s, Gym.Gold, 2f)
        is Head -> drawCircle(figure, 3.6f * s, Offset(p.x * s, p.y * s), style = Stroke(2.2f * s))
        is Disc -> {
            drawCircle(Gym.Yellow, p.r * s, Offset(p.x * s, p.y * s), style = Stroke(2f * s))
            drawCircle(Gym.Yellow, 1f * s, Offset(p.x * s, p.y * s))
        }
        is Bell -> drawLine(Gym.Yellow, Offset(p.x1 * s, p.y1 * s), Offset(p.x2 * s, p.y2 * s), 3.6f * s, StrokeCap.Round)
        is Arrow -> {
            poly(listOf(p.x to p.y1, p.x to p.y2), s, Gym.Yellow, 1.8f)
            poly(listOf(p.x - 3 to p.y2 + 3, p.x to p.y2, p.x + 3 to p.y2 + 3), s, Gym.Yellow, 1.8f)
        }
        Floor -> drawLine(Gym.Gold.copy(alpha = .45f), Offset(4 * s, 45 * s), Offset(44 * s, 45 * s), 1.2f * s)
    }
}

/** A row of small movement icons, e.g. the exercises in a schema. */
@Composable
fun MovementStrip(movements: List<Movement>, modifier: Modifier = Modifier, max: Int = 9) {
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
        movements.take(max).forEach { MovementIcon(it, androidx.compose.ui.Modifier.size(22.dp)) }
    }
}
