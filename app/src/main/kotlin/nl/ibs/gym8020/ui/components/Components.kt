package nl.ibs.gym8020.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.ui.deco.CornerOrnament
import nl.ibs.gym8020.ui.deco.drawDecoFrame
import nl.ibs.gym8020.ui.deco.drawSunburst
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.theme.Poppins

/** Small caps label with wide spacing, like the IBS website. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Gym.Muted) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

/** Card with a chamfered double gold frame and optional art deco corners. */
@Composable
fun DecoCard(
    modifier: Modifier = Modifier,
    corners: Boolean = false,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .background(Gym.Surface, RoundedCornerShape(6.dp))
            .drawBehind { drawDecoFrame() },
    ) {
        if (corners) {
            CornerOrnament(Modifier.align(Alignment.TopStart).padding(5.dp).size(30.dp))
            CornerOrnament(Modifier.align(Alignment.TopEnd).padding(5.dp).size(30.dp), flipX = true)
        }
        Column(Modifier.fillMaxWidth().padding(padding), content = content)
    }
}

@Composable
fun PlainCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val m = modifier.fillMaxWidth().background(Gym.Surface, RoundedCornerShape(20.dp))
    Row(
        (if (onClick != null) m.clickable(onClick = onClick) else m).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Primary action: filled yellow with a subtle sunburst. */
@Composable
fun GoButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Dp = 64.dp) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gym.Yellow, contentColor = Gym.OnYellow),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().drawBehind { drawSunburst(Gym.OnYellow.copy(alpha = .22f), full = false, arcs = false) })
            Text(text.uppercase(), fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 6.sp)
        }
    }
}

/** Secondary action: outlined in cream, like the website buttons. */
@Composable
fun LineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, if (enabled) Gym.Cream else Gym.Muted),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gym.Cream),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Big weight number in yellow with a small unit. */
@Composable
fun WeightText(
    kg: Double?,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
    size: TextUnit = 56.sp,
    signed: Boolean = false,
    color: Color = Gym.Yellow,
    suffix: String? = null,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            kg?.let { WeightFormat.format(it, unit, signed) } ?: "—",
            fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = size, color = color,
            lineHeight = size, style = TextStyle(fontFeatureSettings = "tnum"),
        )
        Text(
            " " + (suffix ?: unit.name.lowercase()),
            fontFamily = Poppins, fontSize = (size.value * .36f).coerceAtLeast(12f).sp, color = Gym.Cream,
            modifier = Modifier.padding(bottom = (size.value * .08f).dp),
        )
    }
}

/** Week progress as diamonds. */
@Composable
fun WeekPips(done: Int, target: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(maxOf(target, done)) { i ->
            val on = i < done
            Box(
                Modifier.size(13.dp).rotate(45f)
                    .background(if (on) Gym.Yellow else Color.Transparent)
                    .border(1.5.dp, if (on) Gym.Yellow else Gym.Gold),
            )
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, size: Dp) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Gym.SurfaceHigh,
        contentColor = Gym.Cream,
        modifier = Modifier.size(size).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
    }
}

/**
 * A number field with large − / + tap areas. Typing is possible too; `,` and `.` both work.
 * [allowNegative] enables the sign for bodyweight extra (assistance), typing `-` makes it negative.
 */
@Composable
fun NumberStepper(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    step: Double,
    modifier: Modifier = Modifier,
    decimals: Boolean = true,
    allowNegative: Boolean = false,
    min: Double? = null,
    placeholder: String = "—",
    minusLabel: String = "−",
    plusLabel: String = "+",
    textSize: TextUnit = 28.sp,
    buttonSize: Dp = 56.dp,
    /** Value the first tap on − or + jumps to when the field is still empty (e.g. the rep target). */
    startFrom: Double? = null,
    onFirstInput: () -> Unit = {},
    formatter: (Double) -> String = { WeightFormat.format(it) },
    parser: (Double) -> Double = { it },
) {
    var text by remember { mutableStateOf(value?.let(formatter) ?: "") }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!focused) text = value?.let(formatter) ?: ""
    }
    fun emit(v: Double?) {
        val clamped = if (v != null && min != null) v.coerceAtLeast(min) else v
        onFirstInput()
        onValueChange(clamped)
        text = clamped?.let(formatter) ?: ""
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundIconButton(Icons.Filled.Remove, minusLabel, { emit(if (value == null && startFrom != null) startFrom else (value ?: 0.0) - step) }, buttonSize)
        BasicTextField(
            value = text,
            onValueChange = { raw ->
                val cleaned = raw.replace('−', '-').replace(',', '.')
                    .filter { it.isDigit() || it == '.' || (allowNegative && it == '-') }
                val negative = allowNegative && cleaned.contains('-')
                val body = cleaned.replace("-", "")
                val shown = (if (negative) "-" else "") + body
                text = shown.replace('.', ',')
                val parsed = body.toDoubleOrNull()?.let { if (negative) -it else it }?.let(parser)
                    ?.let { if (decimals) it else kotlin.math.floor(it) }
                onFirstInput()
                onValueChange(if (body.isEmpty()) null else parsed)
            },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = textSize,
                color = Gym.Cream, textAlign = TextAlign.Center, fontFeatureSettings = "tnum",
            ),
            cursorBrush = SolidColor(Gym.Yellow),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimals || allowNegative) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).widthIn(min = 56.dp).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) Text(placeholder, color = Gym.Muted, fontSize = textSize, fontFamily = Poppins)
                    inner()
                }
            },
        )
        RoundIconButton(Icons.Filled.Add, plusLabel, { emit(if (value == null && startFrom != null) startFrom else (value ?: 0.0) + step) }, buttonSize)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.headlineSmall)
}

@Composable
fun ScreenBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(Gym.Bg), content = content)
}
