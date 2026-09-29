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

/** Line over time with an optional trend line; the last point is marked and labeled. */
@Composable
fun LineChart(
    points: List<Point>,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    trend: List<Point>? = null,
    height: Dp = 170.dp,
    fixedMin: Double? = null,
    fixedMax: Double? = null,
) {
    if (points.isEmpty()) return
    val measurer = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(points) {
            detectTapGestures { tap ->
                val left = 40.dp.toPx()
                val w = size.width - left - 12.dp.toPx()
                val x0 = points.first().date.toEpochDay()
                val span = (points.last().date.toEpochDay() - x0).coerceAtLeast(1)
                selected = points.indices.minByOrNull { i ->
                    abs(left + w * (points[i].date.toEpochDay() - x0) / span - tap.x)
                }
            }
        },
    ) {
        val left = 40.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val values = points.map { it.value } + trend.orEmpty().map { it.value }
        var lo = fixedMin ?: values.min()
        var hi = fixedMax ?: values.max()
        if (hi - lo < 1e-9) { lo -= 1; hi += 1 }
        val pad = if (fixedMin == null) (hi - lo) * .12 else 0.0
        lo -= pad; hi += if (fixedMax == null) pad else 0.0
        val x0 = points.first().date.toEpochDay()
        val span = (points.last().date.toEpochDay() - x0).coerceAtLeast(1)
        fun x(p: Point) = left + (right - left) * (p.date.toEpochDay() - x0) / span
        fun y(v: Double) = (bottom - (bottom - top) * (v - lo) / (hi - lo)).toFloat()

        // Recessive grid with three value labels.
        for (k in 0..2) {
            val v = lo + (hi - lo) * k / 2
            val yy = y(v)
            drawLine(Gym.Gold.copy(alpha = .16f), Offset(left, yy), Offset(right, yy), 1.dp.toPx())
            val t = measurer.measure(format(v), axisStyle)
            drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), yy - t.size.height / 2))
        }
        // Date labels: first and last.
        val d0 = measurer.measure(points.first().date.format(dateFormat), axisStyle)
        drawText(d0, topLeft = Offset(left, bottom + 4.dp.toPx()))
        if (points.size > 1) {
            val d1 = measurer.measure(points.last().date.format(dateFormat), axisStyle)
            drawText(d1, topLeft = Offset(right - d1.size.width, bottom + 4.dp.toPx()))
        }
        trend?.takeIf { it.size > 1 }?.let { tr ->
            val p = Path()
            tr.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(pt), y(pt.value)) else p.lineTo(x(pt), y(pt.value)) }
            drawPath(p, Gym.Cream.copy(alpha = .45f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        }
        if (points.size > 1) {
            val p = Path()
            points.forEachIndexed { i, pt -> if (i == 0) p.moveTo(x(pt), y(pt.value)) else p.lineTo(x(pt), y(pt.value)) }
            drawPath(p, Gym.Yellow, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
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

/** Vertical bars with an optional dashed reference line (e.g. the weekly target). */
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
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(values) {
            detectTapGestures { tap ->
                val slot = (size.width - 8.dp.toPx()) / values.size
                selected = ((tap.x - 4.dp.toPx()) / slot).toInt().coerceIn(0, values.lastIndex)
            }
        },
    ) {
        val top = 20.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val hi = (values.maxOf { it.second }.coerceAtLeast(reference ?: 0.0)).coerceAtLeast(1e-9) * 1.1
        val slot = (size.width - 8.dp.toPx()) / values.size
        val bw = (slot * .55f).coerceAtMost(22.dp.toPx())
        val gap = 2.dp.toPx()
        drawLine(Gym.Gold.copy(alpha = .3f), Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx())
        values.forEachIndexed { i, (label, v) ->
            val cx = 4.dp.toPx() + slot * i + slot / 2
            val h = ((bottom - top) * (v / hi)).toFloat()
            if (h > 0) {
                val r = CornerRadius(4.dp.toPx())
                drawRoundRect(if (selected == null || selected == i) Gym.Yellow else Gym.Yellow.copy(alpha = .45f), Offset(cx - bw / 2, bottom - h), Size(bw, h), r)
                // Square off the bottom so the bar sits on the baseline.
                drawRect(if (selected == null || selected == i) Gym.Yellow else Gym.Yellow.copy(alpha = .45f), Offset(cx - bw / 2, bottom - minOf(h, r.x)), Size(bw, minOf(h, r.x) - gap / 2))
            }
            if (i == 0 || i == values.lastIndex || i == selected) {
                val t = measurer.measure(label, axisStyle)
                drawText(t, topLeft = Offset((cx - t.size.width / 2).coerceIn(0f, size.width - t.size.width), bottom + 3.dp.toPx()))
            }
        }
        reference?.let { ref ->
            val yy = (bottom - (bottom - top) * (ref / hi)).toFloat()
            drawLine(Gym.Cream.copy(alpha = .5f), Offset(0f, yy), Offset(size.width, yy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            val t = measurer.measure(format(ref), axisStyle)
            drawText(t, topLeft = Offset(size.width - t.size.width, yy - t.size.height - 2.dp.toPx()))
        }
        val idx = selected ?: values.lastIndex
        val v = values[idx].second
        val cx = 4.dp.toPx() + slot * idx + slot / 2
        val t = measurer.measure(format(v), valueStyle)
        val h = ((bottom - top) * (v / hi)).toFloat()
        drawText(t, topLeft = Offset((cx - t.size.width / 2).coerceIn(0f, size.width - t.size.width), (bottom - h - t.size.height - 3.dp.toPx()).coerceAtLeast(0f)))
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
