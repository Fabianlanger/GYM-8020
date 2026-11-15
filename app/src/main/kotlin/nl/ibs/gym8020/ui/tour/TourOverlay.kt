package nl.ibs.gym8020.ui.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.TourStep
import nl.ibs.gym8020.ui.theme.Gym
import kotlin.math.roundToInt

/** Test tags for F-11 (TESTING.md): the balloon and its buttons. */
const val TOUR_BALLOON_TAG = "tour_balloon"
const val TOUR_NEXT_TAG = "tour_next"
const val TOUR_SKIP_TAG = "tour_skip"

/** The dim layer over the screen, as in the prototype (variant A). */
private val Scrim = Color(0xB8000000)

/** Secondary text on the cream balloon. */
private val BalloonMuted = Color(0xFF6B665E)

/**
 * The tour (#97, variant A): the screen dims, one part lights up (a cut-out in the dim layer) and a balloon explains it,
 * with "Skip" and "Next". While no step shows, this layer is empty and the screen works as usual. [heavyTarget] and
 * [lightTarget] are the rep targets of the method, for the first balloon.
 */
@Composable
fun TourOverlay(state: TourState, heavyTarget: Int, lightTarget: Int, modifier: Modifier = Modifier) {
    val step = state.showing
    val view = state.viewport
    val target = step?.let { state.boundsOf(it) }
    val density = LocalDensity.current
    Box(modifier.fillMaxSize().onGloballyPositioned { state.viewport = it.boundsInRoot() }) {
        if (step != null && view != null && target != null) {
            val padPx = with(density) { 6.dp.toPx() }
            val radiusPx = with(density) { 16.dp.toPx() }
            // The cut-out in the overlay's own coordinates, a little larger than the part.
            val hole = Rect(
                target.left - view.left - padPx, target.top - view.top - padPx,
                target.right - view.left + padPx, target.bottom - view.top + padPx,
            )
            Canvas(
                Modifier.fillMaxSize()
                    // The dimmed screen takes every touch: only the balloon's buttons work while it shows.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
            ) {
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRoundRect(RoundRect(hole, CornerRadius(radiusPx)))
                }
                drawPath(path, Scrim)
                drawRoundRect(Gym.Yellow, hole.topLeft, hole.size, CornerRadius(radiusPx), style = Stroke(2.dp.toPx()))
            }
            BalloonLayout(hole) {
                Balloon(
                    step = step, number = state.position.number, total = state.position.total,
                    heavyTarget = heavyTarget, lightTarget = lightTarget,
                    onNext = state::next, onSkip = state::skip,
                )
            }
        }
    }
}

/** Places the balloon under the cut-out when it fits, else above it. */
@Composable
private fun BalloonLayout(hole: Rect, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val side = 14.dp.roundToPx()
        val gap = 12.dp.roundToPx()
        val width = (constraints.maxWidth - 2 * side).coerceAtLeast(0)
        val balloon = measurables.first().measure(Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight))
        val below = hole.bottom.roundToInt() + gap
        val above = hole.top.roundToInt() - gap - balloon.height
        val y = when {
            below + balloon.height <= constraints.maxHeight - side -> below
            above >= side -> above
            else -> (constraints.maxHeight - side - balloon.height).coerceAtLeast(side)
        }
        layout(constraints.maxWidth, constraints.maxHeight) { balloon.place(side, y) }
    }
}

@Composable
private fun Balloon(
    step: TourStep,
    number: Int,
    total: Int,
    heavyTarget: Int,
    lightTarget: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val (title, body) = when (step) {
        TourStep.GOAL -> stringResource(R.string.tour_goal_title) to stringResource(R.string.tour_goal_body, heavyTarget, lightTarget)
        TourStep.STACKING -> stringResource(R.string.tour_stacking_title) to stringResource(R.string.tour_stacking_body)
        TourStep.WARMUP -> stringResource(R.string.tour_warmup_title) to stringResource(R.string.tour_warmup_body)
        TourStep.GO -> stringResource(R.string.tour_go_title) to stringResource(R.string.tour_go_body)
        TourStep.REPS -> stringResource(R.string.tour_reps_title) to stringResource(R.string.tour_reps_body)
        TourStep.NEXT_GOAL -> stringResource(R.string.tour_next_goal_title) to stringResource(R.string.tour_next_goal_body)
    }
    val last = number >= total
    Column(
        Modifier.fillMaxWidth().background(Gym.Cream, RoundedCornerShape(14.dp)).testTag(TOUR_BALLOON_TAG)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Gym.Bg, modifier = Modifier.padding(end = 8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Gym.Bg, modifier = Modifier.padding(end = 8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onSkip, modifier = Modifier.testTag(TOUR_SKIP_TAG)) {
                Text(stringResource(R.string.tour_skip), color = BalloonMuted)
            }
            TextButton(onClick = onNext, modifier = Modifier.testTag(TOUR_NEXT_TAG)) {
                Text(
                    stringResource(R.string.tour_count, number, total) + " · " +
                        stringResource(if (last) R.string.tour_done else R.string.tour_next),
                    color = Gym.Bg, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
