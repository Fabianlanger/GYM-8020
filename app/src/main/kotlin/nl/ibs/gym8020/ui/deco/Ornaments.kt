package nl.ibs.gym8020.ui.deco

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import nl.ibs.gym8020.R
import nl.ibs.gym8020.ui.theme.Gym
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Art deco ornaments, drawn as own vectors (D-43, D-54a). Roughly 80 % straight line work
 * (double lines, diagonal kinks, crossing lines, diamonds, fans) and 20 % curls and leaves.
 * Geometry mirrors the style preview (docs/DESIGN.md).
 */

private fun path(block: Path.() -> Unit) = Path().apply(block)

/** Draws in a virtual [w]×[h] box scaled uniformly into the canvas, centered. */
private fun DrawScope.inBox(w: Float, h: Float, block: DrawScope.(stroke: Float) -> Unit) {
    val s = min(size.width / w, size.height / h)
    val dx = (size.width - w * s) / 2
    val dy = (size.height - h * s) / 2
    withTransform({
        translate(dx, dy)
        scale(s, s, Offset.Zero)
    }) { block(1.2f * density / 2f / s) }
}

/** The app emblem: the arm in a plate ring of mini IBS marks, as on the app icon. */
@Composable
fun AppEmblem(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_app_emblem), contentDescription = null, modifier = modifier.aspectRatio(1f))
}

/** The arm: the IBS mark without its top panel, with the shoulder lowered and the biceps raised. */
@Composable
fun ArmMark(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_arm_mark), contentDescription = null, modifier = modifier.aspectRatio(1f))
}

/** The IBS mark: four tilted panels, traced from the logo. Virtual box 30..317 × 5..292. */
private val markPanels = listOf(
    listOf(37f to 42f, 123f to 31f, 103f to 100f, 121f to 211f, 55f to 190f),
    listOf(57f to 197f, 121f to 217f, 234f to 202f, 214f to 266f, 67f to 285f),
    listOf(130f to 31f, 277f to 12f, 290f to 99f, 224f to 79f, 110f to 96f),
    listOf(225f to 85f, 291f to 106f, 310f to 254f, 222f to 264f, 242f to 200f),
)

@Composable
fun IbsMark(modifier: Modifier = Modifier, color: Color = Gym.Yellow) {
    Canvas(modifier.aspectRatio(1f)) {
        val s = min(size.width, size.height) / 287f
        withTransform({
            scale(s, s, Offset.Zero)
            translate(-30f, -5f)
        }) {
            markPanels.forEach { panel ->
                drawPath(
                    path {
                        moveTo(panel[0].first, panel[0].second)
                        panel.drop(1).forEach { (x, y) -> lineTo(x, y) }
                        close()
                    },
                    color,
                )
            }
        }
    }
}

/** Double line ← ◆ → with diagonal kinks, small curls and the IBS square in a diamond. */
@Composable
fun DecoDivider(modifier: Modifier = Modifier.fillMaxWidth(), color: Color = Gym.Gold, accent: Color = Gym.Yellow) {
    Canvas(modifier.aspectRatio(320f / 28f)) {
        inBox(320f, 28f) { sw ->
            val st = Stroke(width = sw)
            drawPath(path { moveTo(0f, 11f); lineTo(120f, 11f); lineTo(130f, 5f); moveTo(0f, 17f); lineTo(120f, 17f); lineTo(130f, 23f) }, color, style = st)
            drawPath(path { moveTo(320f, 11f); lineTo(200f, 11f); lineTo(190f, 5f); moveTo(320f, 17f); lineTo(200f, 17f); lineTo(190f, 23f) }, color, style = st)
            drawPath(path { moveTo(160f, 0f); lineTo(174f, 14f); lineTo(160f, 28f); lineTo(146f, 14f); close() }, color, style = st)
            drawPath(
                path {
                    moveTo(130f, 5f); cubicTo(137f, 1f, 142f, 6f, 146f, 14f)
                    moveTo(130f, 23f); cubicTo(137f, 27f, 142f, 22f, 146f, 14f)
                    moveTo(190f, 5f); cubicTo(183f, 1f, 178f, 6f, 174f, 14f)
                    moveTo(190f, 23f); cubicTo(183f, 27f, 178f, 22f, 174f, 14f)
                },
                color, style = st,
            )
            drawPath(path { moveTo(40f, 14f); lineTo(80f, 14f); moveTo(240f, 14f); lineTo(280f, 14f) }, color.copy(alpha = color.alpha * .6f), style = st)
            rotate(-8f, Offset(160f, 14f)) {
                drawRect(accent, Offset(153f, 7f), Size(14f, 14f))
                drawRect(Gym.Bg, Offset(157.5f, 11.5f), Size(5f, 5f))
            }
        }
    }
}

/** Floral divider for reward moments: curls, leaves, a diamond and a dot. */
@Composable
fun FlourishDivider(modifier: Modifier = Modifier.fillMaxWidth(), color: Color = Gym.Gold, accent: Color = Gym.Yellow) {
    Canvas(modifier.aspectRatio(320f / 40f)) {
        inBox(320f, 40f) { sw ->
            val st = Stroke(width = sw)
            drawPath(
                path {
                    moveTo(0f, 20f); lineTo(110f, 20f); cubicTo(124f, 20f, 128f, 8f, 140f, 10f)
                    cubicTo(150f, 12f, 148f, 24f, 139f, 22f); cubicTo(133f, 21f, 134f, 14f, 140f, 15f)
                    moveTo(320f, 20f); lineTo(210f, 20f); cubicTo(196f, 20f, 192f, 8f, 180f, 10f)
                    cubicTo(170f, 12f, 172f, 24f, 181f, 22f); cubicTo(187f, 21f, 186f, 14f, 180f, 15f)
                    moveTo(160f, 4f); lineTo(172f, 20f); lineTo(160f, 36f); lineTo(148f, 20f); close()
                },
                color, style = st,
            )
            val leaves = path {
                moveTo(150f, 20f); quadraticTo(140f, 34f, 124f, 32f); quadraticTo(136f, 26f, 150f, 20f); close()
                moveTo(170f, 20f); quadraticTo(180f, 34f, 196f, 32f); quadraticTo(184f, 26f, 170f, 20f); close()
            }
            drawPath(leaves, color.copy(alpha = .25f), style = Fill)
            drawPath(leaves, color, style = st)
            drawCircle(accent, 4f, Offset(160f, 20f))
        }
    }
}

/**
 * Stepped, interlaced corner with a small curl and leaf. Drawn for the top-left corner;
 * use [flipX]/[flipY] for the others.
 */
@Composable
fun CornerOrnament(modifier: Modifier = Modifier, flipX: Boolean = false, flipY: Boolean = false, color: Color = Gym.Gold) {
    Canvas(modifier.aspectRatio(1f)) {
        withTransform({ scale(if (flipX) -1f else 1f, if (flipY) -1f else 1f) }) {
            inBox(64f, 64f) { sw ->
                val st = Stroke(width = sw)
                drawPath(path { moveTo(3f, 64f); lineTo(3f, 3f); lineTo(64f, 3f) }, color, style = st)
                drawPath(path { moveTo(9f, 64f); lineTo(9f, 9f); lineTo(64f, 9f) }, color, style = st)
                drawPath(path { moveTo(0f, 18f); lineTo(30f, 18f); moveTo(18f, 0f); lineTo(18f, 30f) }, color.copy(alpha = .8f), style = st)
                drawPath(path { moveTo(15f, 44f); lineTo(15f, 15f); lineTo(44f, 15f); moveTo(15f, 15f); lineTo(26f, 26f) }, color, style = st)
                drawPath(path { moveTo(26f, 26f); cubicTo(34f, 22f, 40f, 30f, 34f, 34f); cubicTo(30f, 37f, 26f, 33f, 29f, 30f) }, color, style = st)
                val leaf = path { moveTo(34f, 34f); quadraticTo(44f, 46f, 56f, 42f); quadraticTo(46f, 36f, 34f, 34f); close() }
                drawPath(leaf, color.copy(alpha = .25f))
                drawPath(leaf, color, style = st)
                rotate(45f, Offset(15f, 15f)) { drawRect(Gym.Yellow, Offset(12f, 12f), Size(6f, 6f)) }
            }
        }
    }
}

/** Sunburst of straight rays, alternating long and short. Half fan (upwards) or full circle. */
@Composable
fun Sunburst(modifier: Modifier = Modifier, color: Color = Gym.Gold, full: Boolean = false, arcs: Boolean = true) {
    Canvas(modifier) { drawSunburst(color, full, arcs) }
}

fun DrawScope.drawSunburst(color: Color, full: Boolean, arcs: Boolean = true) {
    val cx = size.width / 2
    val cy = if (full) size.height / 2 else size.height
    val r = if (full) min(size.width, size.height) / 2 else min(size.width / 2, size.height)
    val step = 7.5
    val start = if (full) 0.0 else 7.5
    val end = if (full) 352.5 else 172.5
    var a = start
    var i = 0
    val sw = 1.2f * density / 2f
    while (a <= end + 1e-6) {
        val rad = a * PI / 180
        val len = if (i % 2 == 1) r * .72f else r
        val inner = if (full) r * .36f else r * .28f
        drawLine(
            color,
            Offset(cx + inner * cos(rad).toFloat(), cy - inner * sin(rad).toFloat()),
            Offset(cx + len * cos(rad).toFloat(), cy - len * sin(rad).toFloat()),
            strokeWidth = if (i % 2 == 1) sw * .7f else sw,
        )
        a += step; i++
    }
    if (arcs) {
        val st = Stroke(width = sw)
        listOf(.2f, .24f).forEach { f ->
            val rr = r * f * (if (full) 2.6f else 1f)
            drawArc(color, if (full) 0f else 180f, if (full) 360f else 180f, false, Offset(cx - rr, cy - rr), Size(rr * 2, rr * 2), style = st)
        }
    }
}

/** Diamond lattice with small fans; used at low alpha as background texture. */
@Composable
fun Lattice(modifier: Modifier = Modifier, color: Color = Gym.Gold, alpha: Float = .07f, tile: Float = 40f) {
    Canvas(modifier) {
        val t = tile * density / 2f
        val c = color.copy(alpha = alpha)
        val sw = 1f * density / 2f
        val st = Stroke(width = sw)
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                val u = t / 40f
                drawPath(path { moveTo(x, y + 20 * u); lineTo(x + 20 * u, y); lineTo(x + 40 * u, y + 20 * u); lineTo(x + 20 * u, y + 40 * u); close() }, c, style = st)
                drawArc(c, 180f, 180f, false, Offset(x + 8 * u, y + 26 * u), Size(24 * u, 24 * u), style = st)
                for (k in 1..5) {
                    val rad = (k * 30.0) * PI / 180
                    drawLine(c, Offset(x + 20 * u, y + 38 * u), Offset(x + 20 * u + 12 * u * cos(rad).toFloat(), y + 38 * u - 12 * u * sin(rad).toFloat()), sw)
                }
                x += t
            }
            y += t
        }
    }
}

/** Double frame with chamfered corners, drawn behind card content. */
fun DrawScope.drawDecoFrame(color: Color = Gym.GoldSoft, chamfer: Float = 12f * density / 2f, gap: Float = 4f * density / 2f) {
    val sw = 1f * density / 2f
    fun frame(inset: Float) = path {
        val c = chamfer
        val l = inset; val t = inset; val r = size.width - inset; val b = size.height - inset
        moveTo(l + c, t); lineTo(r - c, t); lineTo(r, t + c); lineTo(r, b - c); lineTo(r - c, b)
        lineTo(l + c, b); lineTo(l, b - c); lineTo(l, t + c); close()
    }
    drawPath(frame(sw), color, style = Stroke(sw))
    drawPath(frame(sw + gap), color, style = Stroke(sw))
}

/**
 * Swipe ribbon (D-71): a slim art deco spire at the screen edge. Triple spine with stepped caps,
 * an inward-facing fan with a yellow diamond, chevrons that shimmer in the swipe direction ([phase] 0..1),
 * and a curl with a leaf at both ends. Drawn for the right edge; [mirror] for the left.
 * Virtual box 34 × 200.
 */
fun DrawScope.drawRibbon(phase: Float, mirror: Boolean, color: Color = Gym.Gold, accent: Color = Gym.Yellow) {
    val w = 34f
    val h = 200f
    val s = min(size.width / w, size.height / h)
    withTransform({
        if (mirror) scale(-1f, 1f)
        translate((size.width - w * s) / 2, (size.height - h * s) / 2)
        scale(s, s, Offset.Zero)
    }) {
        val sw = 1.1f * density / 2f / s
        val st = Stroke(width = sw)
        val cy = h / 2
        val a = w - 2.5f
        val b = w - 7f
        val c = w - 11f
        drawLine(color, Offset(a, 0f), Offset(a, h), sw * 1.1f)
        drawPath(path {
            moveTo(a, 6f); cubicTo(b, 20f, b, 30f, b, 46f); lineTo(b, h - 46f); cubicTo(b, h - 30f, b, h - 20f, a, h - 6f)
        }, color, style = st)
        drawPath(path { moveTo(c, cy - 58f); lineTo(c, cy - 30f); moveTo(c, cy + 30f); lineTo(c, cy + 58f) }, color.copy(alpha = color.alpha * .55f), style = st)
        for (sgn in listOf(-1f, 1f)) {
            val y0 = cy + sgn * 58f
            for (k in 0..2) {
                val y = y0 + sgn * k * 3.5f
                drawLine(color.copy(alpha = color.alpha * (0.8f - k * 0.2f)), Offset(c - (k + 1) * 2.2f, y), Offset(b, y), sw * .8f)
            }
        }
        // Fan pointing into the screen.
        var i = 0
        var deg = 95.0
        while (deg <= 265.0) {
            val rad = deg * PI / 180
            val r = if (i % 2 == 0) 18f else 13f
            drawLine(color, Offset(b + 5f * cos(rad).toFloat(), cy - 5f * sin(rad).toFloat()), Offset(b + r * cos(rad).toFloat(), cy - r * sin(rad).toFloat()), sw * .85f)
            deg += 17.0; i++
        }
        listOf(21f to 1f, 24f to .6f).forEach { (rr, op) ->
            drawArc(color.copy(alpha = color.alpha * op), 90f, 180f, false, Offset(b - rr, cy - rr), Size(rr * 2, rr * 2), style = Stroke(sw * .85f))
        }
        drawPath(path { moveTo(b + 1f, cy - 6f); lineTo(b - 4f, cy); lineTo(b + 1f, cy + 6f); lineTo(b + 4f, cy); close() }, accent)
        // Chevrons: a light travels outward-to-inward, suggesting the pull direction.
        for (sgn in listOf(-1f, 1f)) {
            for (k in 0..2) {
                val y = cy + sgn * (33f + k * 8f)
                val wave = ((phase * 3f - k).let { it - kotlin.math.floor(it) })
                val alpha = 0.3f + 0.7f * (1f - wave)
                val z = 3.8f
                drawPath(
                    path { moveTo(b - 3f, y - z); lineTo(b - 3f - z, y); lineTo(b - 3f, y + z) },
                    (if (k == 0) accent else color).copy(alpha = alpha),
                    style = Stroke(width = sw * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        // Curls and leaves: the floral touch.
        for (sgn in listOf(-1f, 1f)) {
            val y = cy + sgn * 74f
            drawPath(path { moveTo(b, y); cubicTo(b - 10f, y - sgn * 2f, b - 9f, y + sgn * 9f, b - 4f, y + sgn * 7f) }, color, style = Stroke(sw * .85f))
            val leaf = path { moveTo(b, y + sgn * 10f); quadraticTo(b - 8f, y + sgn * 15f, b - 2f, y + sgn * 24f); quadraticTo(b + 1f, y + sgn * 16f, b, y + sgn * 10f); close() }
            drawPath(leaf, color.copy(alpha = .28f))
            drawPath(leaf, color, style = Stroke(sw * .7f))
        }
    }
}

/** Background patterns: each area of the app has its own (home, training, everything else). */
enum class DecoPattern { LATTICE, FANS, ARCHES }

/**
 * Art deco pattern at the top of a screen, fading out towards [fadeHeight] so content stays readable.
 */
@Composable
fun DecoBackground(pattern: DecoPattern, modifier: Modifier = Modifier, fadeHeight: Float = 300f, color: Color = Gym.Gold) {
    Canvas(modifier) {
        val fadePx = fadeHeight * density
        val sw = 1f * density / 2.2f
        fun fade(y: Float) = (1f - y / fadePx).coerceIn(0f, 1f)
        when (pattern) {
            DecoPattern.LATTICE -> {
                val t = 40f * density / 2f
                var y = 0f
                while (y < fadePx) {
                    val c = color.copy(alpha = .10f * fade(y))
                    var x = 0f
                    while (x < size.width) {
                        val u = t / 40f
                        drawPath(path { moveTo(x, y + 20 * u); lineTo(x + 20 * u, y); lineTo(x + 40 * u, y + 20 * u); lineTo(x + 20 * u, y + 40 * u); close() }, c, style = Stroke(sw))
                        drawArc(c, 180f, 180f, false, Offset(x + 8 * u, y + 26 * u), Size(24 * u, 24 * u), style = Stroke(sw))
                        for (k in 1..5) {
                            val rad = (k * 30.0) * PI / 180
                            drawLine(c, Offset(x + 20 * u, y + 38 * u), Offset(x + 20 * u + 12 * u * cos(rad).toFloat(), y + 38 * u - 12 * u * sin(rad).toFloat()), sw)
                        }
                        x += t
                    }
                    y += t
                }
            }
            DecoPattern.FANS -> {
                // Overlapping fan scales.
                val t = 36f * density / 2.2f
                var row = 0
                var y = 0f
                while (y < fadePx) {
                    val op = .22f * fade(y)
                    val off = if (row % 2 == 1) t / 2 else 0f
                    var x = -t + off
                    while (x < size.width + t) {
                        val cx = x + t / 2
                        val cy = y + t / 2
                        drawArc(color.copy(alpha = op), 180f, 180f, false, Offset(x, cy - t / 2), Size(t, t), style = Stroke(sw))
                        val r2 = t / 2 - t * .14f
                        drawArc(color.copy(alpha = op * .7f), 180f, 180f, false, Offset(cx - r2, cy - r2), Size(r2 * 2, r2 * 2), style = Stroke(sw * .8f))
                        for (k in 1..5) {
                            val rad = (k * 30.0) * PI / 180
                            val rr = t / 2 - t * .17f
                            drawLine(color.copy(alpha = op * .6f), Offset(cx, cy), Offset(cx + rr * cos(rad).toFloat(), cy - rr * sin(rad).toFloat()), sw * .7f)
                        }
                        x += t
                    }
                    y += t / 2; row++
                }
            }
            DecoPattern.ARCHES -> {
                // Rows of double arches with a dot: calm, architectural.
                val t = 30f * density / 2.2f
                var y = 0f
                while (y < fadePx) {
                    val op = .2f * fade(y)
                    var x = 0f
                    while (x < size.width) {
                        val m = t * .13f
                        drawPath(path {
                            moveTo(x + m, y + t); lineTo(x + m, y + t * .4f)
                            arcTo(androidx.compose.ui.geometry.Rect(x + m, y + t * .4f - (t / 2 - m), x + t - m, y + t * .4f + (t / 2 - m)), 180f, 180f, false)
                            lineTo(x + t - m, y + t)
                        }, color.copy(alpha = op), style = Stroke(sw))
                        val m2 = t * .3f
                        drawPath(path {
                            moveTo(x + m2, y + t); lineTo(x + m2, y + t * .47f)
                            arcTo(androidx.compose.ui.geometry.Rect(x + m2, y + t * .47f - (t / 2 - m2), x + t - m2, y + t * .47f + (t / 2 - m2)), 180f, 180f, false)
                            lineTo(x + t - m2, y + t)
                        }, color.copy(alpha = op * .7f), style = Stroke(sw * .8f))
                        drawCircle(color.copy(alpha = (op * 1.3f).coerceAtMost(1f)), 1.3f * density / 2.2f, Offset(x + t / 2, y + t * .3f))
                        x += t
                    }
                    y += t
                }
            }
        }
    }
}
