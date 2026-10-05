package nl.ibs.gym8020.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.core.stats.AxisTicks
import nl.ibs.gym8020.core.stats.Point
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/*
 * Charts in the house style: one series in IBS yellow, thin 2 dp lines, recessive gold grid,
 * labels in text colors. Tap a mark to read its exact value (the touch version of a tooltip).
 */

private val axisStyle = TextStyle(fontFamily = Poppins, fontSize = 10.sp, color = Gym.Muted)
private val valueStyle = TextStyle(fontFamily = Poppins, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Gym.Cream)
private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
private val shortDate = DateTimeFormatter.ofPattern("d MMM")

/** Axis tick text: plain numbers with the right decimals and a decimal comma. */
private fun tickText(v: Double, decimals: Int): String = "%.${decimals}f".format(v).replace('.', ',')

/**
 * Line over time with an optional trend line; the last point is marked and labeled. Real axes (V-13): a value
 * axis with round numbers and the [unit] on top, a time axis with dates; light grid lines.
 */
@Composable
fun LineChart(
    points: List<Point>,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    trend: List<Point>? = null,
    height: Dp = 190.dp,
    fixedMin: Double? = null,
    fixedMax: Double? = null,
    unit: String? = null,
    /** Extra marks per point (e.g. several weigh-ins on one day); drawn as small dots. */
    extraDots: List<Point> = emptyList(),
    /** Text for the value axis; plain numbers by default. */
    tickFormat: ((Double) -> String)? = null,
) {
    if (points.isEmpty()) return
    val measurer = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val values = points.map { it.value } + trend.orEmpty().map { it.value } + extraDots.map { it.value }
    val ticks = AxisTicks.nice(fixedMin ?: values.min(), fixedMax ?: values.max(), 4)
    val lo = fixedMin ?: ticks.values.first()
    val hi = fixedMax ?: ticks.values.last()
    val tickLabels = ticks.values.filter { it >= lo - 1e-9 && it <= hi + 1e-9 }.map { it to (tickFormat?.invoke(it) ?: tickText(it, ticks.decimals)) }
    val leftPx = remember(tickLabels) { tickLabels.maxOfOrNull { measurer.measure(it.second, axisStyle).size.width } ?: 0 }
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(points) {
            detectTapGestures { tap ->
                val left = leftPx + 10.dp.toPx()
                val w = size.width - left - 12.dp.toPx()
                val x0 = points.first().date.toEpochDay()
                val span = (points.last().date.toEpochDay() - x0).coerceAtLeast(1)
                selected = points.indices.minByOrNull { i ->
                    abs(left + w * (points[i].date.toEpochDay() - x0) / span - tap.x)
                }
            }
        },
    ) {
        val left = leftPx + 10.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val x0 = points.first().date.toEpochDay()
        val span = (points.last().date.toEpochDay() - x0).coerceAtLeast(1)
        fun xOf(day: Long) = left + (right - left) * (day - x0) / span
        fun x(p: Point) = xOf(p.date.toEpochDay())
        fun y(v: Double) = (bottom - (bottom - top) * (v - lo) / (hi - lo)).toFloat()

        // Value axis: round numbers, light grid lines, the unit on top.
        tickLabels.forEach { (v, text) ->
            val yy = y(v)
            drawLine(Gym.Gold.copy(alpha = .14f), Offset(left, yy), Offset(right, yy), 1.dp.toPx())
            val t = measurer.measure(text, axisStyle)
            drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), yy - t.size.height / 2))
        }
        unit?.let { drawText(measurer.measure(it, axisStyle), topLeft = Offset(0f, 0f)) }
        // Axis lines.
        val axis = Gym.Muted.copy(alpha = .6f)
        drawLine(axis, Offset(left, top - 6.dp.toPx()), Offset(left, bottom), 1.dp.toPx())
        drawLine(axis, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
        // Time axis: up to four dates with tick marks.
        val dayTicks = if (span <= 1) listOf(x0) else (0..3).map { x0 + span * it / 3 }.distinct()
        dayTicks.forEachIndexed { i, day ->
            val xx = xOf(day)
            drawLine(axis, Offset(xx, bottom), Offset(xx, bottom + 4.dp.toPx()), 1.dp.toPx())
            val t = measurer.measure(java.time.LocalDate.ofEpochDay(day).format(shortDate), axisStyle)
            val tx = when (i) {
                0 -> xx
                dayTicks.lastIndex -> xx - t.size.width
                else -> xx - t.size.width / 2
            }
            drawText(t, topLeft = Offset(tx.coerceIn(0f, size.width - t.size.width), bottom + 6.dp.toPx()))
        }
        trend?.takeIf { it.size > 1 }?.let { tr ->
            val p = Path()
            tr.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(pt), y(pt.value)) else p.lineTo(x(pt), y(pt.value)) }
            drawPath(p, Gym.Cream.copy(alpha = .45f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        }
        extraDots.forEach { drawCircle(Gym.Yellow.copy(alpha = .45f), 2.5.dp.toPx(), Offset(x(it), y(it.value))) }
        if (points.size > 1) {
            val p = Path()
            points.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(pt), y(pt.value)) else p.lineTo(x(pt), y(pt.value)) }
            drawPath(p, Gym.Yellow, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        points.forEach { drawCircle(Gym.Yellow, 2.dp.toPx(), Offset(x(it), y(it.value))) }
        val idx = selected ?: points.lastIndex
        val pt = points[idx]
        val c = Offset(x(pt), y(pt.value))
        drawCircle(Gym.Bg, 6.dp.toPx(), c)
        drawCircle(Gym.Yellow, 4.dp.toPx(), c)
        val label = measurer.measure(format(pt.value) + if (selected != null) "  ·  " + pt.date.format(dateFormat) else "", valueStyle)
        val lx = (c.x - label.size.width / 2).coerceIn(left, right - label.size.width)
        drawText(label, topLeft = Offset(lx, (c.y - label.size.height - 8.dp.toPx()).coerceAtLeast(0f)))
    }
}

/** Vertical bars with an optional dashed reference line (e.g. the weekly target), on a value axis (V-13). */
@Composable
fun BarChart(
    values: List<Pair<String, Double>>,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    reference: Double? = null,
    height: Dp = 150.dp,
) {
    if (values.isEmpty()) return
    val measurer = rememberTextMeasurer()
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val maxV = values.maxOf { it.second }.coerceAtLeast(reference ?: 0.0)
    val ticks = AxisTicks.nice(0.0, maxV.coerceAtLeast(1e-9), 3)
    val hi = ticks.values.last().coerceAtLeast(1e-9)
    val tickLabels = ticks.values.map { it to tickText(it, ticks.decimals) }
    val leftPx = remember(tickLabels) { tickLabels.maxOfOrNull { measurer.measure(it.second, axisStyle).size.width } ?: 0 }
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(values) {
            detectTapGestures { tap ->
                val left = leftPx + 10.dp.toPx()
                val slot = (size.width - left - 4.dp.toPx()) / values.size
                selected = ((tap.x - left) / slot).toInt().coerceIn(0, values.lastIndex)
            }
        },
    ) {
        val left = leftPx + 10.dp.toPx()
        val top = 20.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val slot = (size.width - left - 4.dp.toPx()) / values.size
        val bw = (slot * .55f).coerceAtMost(22.dp.toPx())
        val gap = 2.dp.toPx()
        fun y(v: Double) = (bottom - (bottom - top) * (v / hi)).toFloat()
        tickLabels.forEach { (v, text) ->
            val yy = y(v)
            if (v > 0) drawLine(Gym.Gold.copy(alpha = .14f), Offset(left, yy), Offset(size.width, yy), 1.dp.toPx())
            val t = measurer.measure(text, axisStyle)
            drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), yy - t.size.height / 2))
        }
        val axis = Gym.Muted.copy(alpha = .6f)
        drawLine(axis, Offset(left, top - 6.dp.toPx()), Offset(left, bottom), 1.dp.toPx())
        drawLine(axis, Offset(left, bottom), Offset(size.width, bottom), 1.dp.toPx())
        // Show a label every n bars so they never overlap.
        val every = (values.size / 6).coerceAtLeast(1)
        values.forEachIndexed { i, (label, v) ->
            val cx = left + slot * i + slot / 2
            val h = bottom - y(v)
            if (h > 0) {
                val r = CornerRadius(4.dp.toPx())
                val color = if (selected == null || selected == i) Gym.Yellow else Gym.Yellow.copy(alpha = .45f)
                drawRoundRect(color, Offset(cx - bw / 2, bottom - h), Size(bw, h), r)
                // Square off the bottom so the bar sits on the baseline.
                drawRect(color, Offset(cx - bw / 2, bottom - minOf(h, r.x)), Size(bw, minOf(h, r.x) - gap / 2))
            }
            if (i % every == 0 || i == values.lastIndex || i == selected) {
                val t = measurer.measure(label, axisStyle)
                drawText(t, topLeft = Offset((cx - t.size.width / 2).coerceIn(0f, size.width - t.size.width), bottom + 3.dp.toPx()))
            }
        }
        reference?.let { ref ->
            val yy = y(ref)
            drawLine(Gym.Cream.copy(alpha = .5f), Offset(left, yy), Offset(size.width, yy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            val t = measurer.measure(format(ref), axisStyle)
            drawText(t, topLeft = Offset(size.width - t.size.width, yy - t.size.height - 2.dp.toPx()))
        }
        val idx = selected ?: values.lastIndex
        val v = values[idx].second
        val cx = left + slot * idx + slot / 2
        val t = measurer.measure(format(v), valueStyle)
        drawText(t, topLeft = Offset((cx - t.size.width / 2).coerceIn(0f, size.width - t.size.width), (y(v) - t.size.height - 3.dp.toPx()).coerceAtLeast(0f)))
    }
}

/** A labeled horizontal bar row: magnitude in one hue, value in text color. */
@Composable
fun HBarRow(label: String, value: Double, max: Double, valueText: String, selected: Boolean, onClick: () -> Unit, leading: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(if (selected) Gym.SurfaceHigh else Gym.Surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.fillMaxWidth().height(6.dp).background(Gym.Bg, RoundedCornerShape(3.dp))) {
                val f = if (max > 0) (value / max).toFloat().coerceIn(0f, 1f) else 0f
                if (f > 0) Box(Modifier.fillMaxWidth(f).height(6.dp).background(Gym.Yellow, RoundedCornerShape(3.dp)))
            }
        }
        Text(valueText, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(52.dp), maxLines = 1)
    }
}

/** Two-sided balance bar, e.g. push vs pull. */
@Composable
fun BalanceBar(leftLabel: String, rightLabel: String, ratio: Double?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(leftLabel + (ratio?.let { "  ${(it * 100).toInt()} %" } ?: ""), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text((ratio?.let { "${100 - (it * 100).toInt()} %  " } ?: "") + rightLabel, style = MaterialTheme.typography.bodySmall)
        }
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val r = CornerRadius(5.dp.toPx())
            drawRoundRect(Gym.SurfaceHigh, size = size, cornerRadius = r)
            if (ratio != null) {
                val split = size.width * ratio.toFloat()
                drawRoundRect(Gym.Yellow, size = Size(split, size.height), cornerRadius = r)
                drawRoundRect(Gym.Gold.copy(alpha = .55f), Offset(split + 2.dp.toPx(), 0f), Size((size.width - split - 2.dp.toPx()).coerceAtLeast(0f), size.height), r)
            }
            drawLine(Gym.Cream, Offset(size.width / 2, -2f), Offset(size.width / 2, size.height + 2f), 1.dp.toPx())
        }
    }
}
