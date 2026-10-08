package nl.ibs.gym8020.ui.components

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
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
            CornerOrnament(Modifier.align(Alignment.TopStart).padding(5.dp).size(26.dp))
            CornerOrnament(Modifier.align(Alignment.TopEnd).padding(5.dp).size(26.dp), flipX = true)
        }
        // With corners, content starts below them so labels are never covered.
        Column(Modifier.fillMaxWidth().padding(start = padding, end = padding, bottom = padding, top = if (corners) maxOf(padding, 30.dp) else padding), content = content)
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gym.Cream),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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

/**
 * A number field where typing comes first (V-11, variant A): a clear input box, tap anywhere to type (the
 * number is selected, so typing replaces it), with small quiet − / + inside the box for one step, e.g. the next
 * achievable weight. `,` and `.` both work. [allowNegative] enables the sign for bodyweight extra (assistance);
 * typing `-` makes it negative. An empty field shows a muted pencil ("tap to type", #49) unless a [placeholder]
 * text is given.
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
    /** Grey hint text in an empty field (e.g. the target weight); null shows the pencil. */
    placeholder: String? = null,
    minusLabel: String = "−",
    plusLabel: String = "+",
    textSize: TextUnit = 28.sp,
    /** Height of the input box. */
    buttonSize: Dp = 56.dp,
    /** Value the first tap on − or + jumps to when the field is still empty (e.g. the rep target). */
    startFrom: Double? = null,
    /** Tapping the [placeholder] takes over [startFrom] (e.g. the target weight shown as a hint, #47). */
    placeholderTakesOver: Boolean = false,
    /** Custom up/down (e.g. next achievable weight); defaults to ± [step]. */
    next: ((Double) -> Double)? = null,
    previous: ((Double) -> Double)? = null,
    onFirstInput: () -> Unit = {},
    formatter: (Double) -> String = { WeightFormat.format(it) },
    parser: (Double) -> Double = { it },
    /** Unit shown after the number, e.g. "kg". */
    suffix: String? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(value?.let(formatter) ?: "")) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(value) {
        if (!focused) field = TextFieldValue(value?.let(formatter) ?: "")
    }
    fun emit(v: Double?) {
        val clamped = if (v != null && min != null) v.coerceAtLeast(min) else v
        onFirstInput()
        onValueChange(clamped)
        val shown = clamped?.let(formatter) ?: ""
        field = TextFieldValue(shown, TextRange(shown.length))
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .heightIn(min = maxOf(buttonSize, 48.dp))
            .background(Gym.SurfaceHigh.copy(alpha = .55f), shape)
            .border(2.dp, if (focused) Gym.Yellow else Gym.SurfaceHigh, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.requestFocus() }
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = field,
            onValueChange = { input ->
                val cleaned = input.text.replace('−', '-').replace(',', '.')
                    .filter { it.isDigit() || it == '.' || (allowNegative && it == '-') }
                val negative = allowNegative && cleaned.contains('-')
                val body = cleaned.replace("-", "")
                val shown = ((if (negative) "-" else "") + body).replace('.', ',')
                field = TextFieldValue(shown, TextRange(shown.length))
                val parsed = body.toDoubleOrNull()?.let { if (negative) -it else it }?.let(parser)
                    ?.let { if (decimals) it else kotlin.math.floor(it) }
                onFirstInput()
                onValueChange(if (body.isEmpty()) null else parsed)
            },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = textSize,
                color = Gym.Cream, textAlign = TextAlign.Start, fontFeatureSettings = "tnum",
            ),
            cursorBrush = SolidColor(Gym.Yellow),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimals || allowNegative) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).widthIn(min = 40.dp).focusRequester(focus).onFocusChanged {
                // Select the number on focus, so typing replaces it.
                if (it.isFocused && !focused) field = field.copy(selection = TextRange(0, field.text.length))
                focused = it.isFocused
            },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (field.text.isEmpty()) {
                        if (placeholder == null) {
                            // Not a dash: that looks like the − button next to it (#49).
                            val iconSize = with(LocalDensity.current) { textSize.toDp() }
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = Gym.Muted.copy(alpha = .6f), modifier = Modifier.size(iconSize))
                        } else {
                            val takeOver = if (placeholderTakesOver && startFrom != null) Modifier.clickable { emit(startFrom) } else Modifier
                            Text(placeholder, color = Gym.Muted, fontSize = textSize, fontFamily = Poppins, modifier = takeOver)
                        }
                    }
                    inner()
                }
            },
        )
        if (suffix != null) Text(suffix, color = Gym.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 6.dp))
        MiniStepButton(Icons.Filled.Remove, minusLabel) {
            emit(if (value == null && startFrom != null) startFrom else previous?.invoke(value ?: 0.0) ?: ((value ?: 0.0) - step))
        }
        MiniStepButton(Icons.Filled.Add, plusLabel) {
            emit(if (value == null && startFrom != null) startFrom else next?.invoke(value ?: 0.0) ?: ((value ?: 0.0) + step))
        }
    }
}

/** Small, quiet step button inside a number field (V-11). Tap area 40dp. */
@Composable
private fun MiniStepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(30.dp).border(1.dp, Gym.SurfaceHigh, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Gym.Muted, modifier = Modifier.size(16.dp))
        }
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

/** Whether the app runs in Dutch (D-88). */
@Composable
fun appIsDutch(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "nl"
