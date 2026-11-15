package nl.ibs.gym8020.ui.tour

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.DemoAction
import nl.ibs.gym8020.core.logic.DemoScene
import nl.ibs.gym8020.core.logic.DemoScript
import nl.ibs.gym8020.core.logic.DemoStep
import nl.ibs.gym8020.core.logic.DemoTarget
import nl.ibs.gym8020.core.logic.PracticeDemo
import nl.ibs.gym8020.ui.theme.Gym
import kotlin.math.roundToInt

/** Test tags for F-11 (TESTING.md): the balloon, the finger and the controls of the demo. */
const val DEMO_BALLOON_TAG = "demo_balloon"
const val DEMO_FINGER_TAG = "demo_finger"
const val DEMO_BACK_TAG = "demo_back"
const val DEMO_PAUSE_TAG = "demo_pause"
const val DEMO_SKIP_TAG = "demo_skip"

/** The dim layer around the part the finger points at, as in the prototype (variant A of #83). */
private val Scrim = Color(0xB8000000)

/** Secondary text on the cream balloon. */
private val BalloonMuted = Color(0xFF6B665E)

/** The finger's border, as in the prototype. */
private val FingerEdge = Color(0xFF111111)

/** The finger glides to a part like the cursor of the prototype. */
private val Glide = CubicBezierEasing(.4f, .1f, .2f, 1f)

/** Waits and pauses go in steps this long, so Pause takes effect at once. */
private const val STEP_MS = 50L

/**
 * The demo of the practice workout on the screen (#99): the finger and its tap ring, the part it points at, and how
 * long the scene has played. [play] performs one scene of the script on the [DemoTargets].
 */
@Stable
class DemoView {
    /** The finger, in the root. */
    internal val finger = Animatable(Offset.Zero, Offset.VectorConverter)

    /** The finger shows from its first move on. */
    internal var fingerShown by mutableStateOf(false)

    /** The tap ring: from 0 to 1 while it plays; 1 when there is none. */
    internal val ring = Animatable(1f)

    /** The overlay in the root: the finger enters from its bottom. */
    internal var area: Rect? by mutableStateOf(null)

    /** The part the finger points at: it lights up. */
    var focus: DemoTarget? by mutableStateOf(null)
        private set

    /** How long the current scene has played, pauses not counted (for "‹"). */
    var elapsedMs: Long = 0L
        private set

    /**
     * Plays [scene]: moves the finger, taps and types on [targets], waits while [paused]. A part that does not come
     * within [PracticeDemo.TARGET_WAIT_MS] is passed, so the demo never hangs.
     */
    suspend fun play(scene: DemoScene, targets: DemoTargets, paused: State<Boolean>) = coroutineScope {
        elapsedMs = 0L

        suspend fun awaitPlaying() {
            if (paused.value) snapshotFlow { paused.value }.first { !it }
        }

        suspend fun hold(ms: Long) {
            var left = ms
            while (left > 0) {
                awaitPlaying()
                val step = minOf(STEP_MS, left)
                delay(step)
                left -= step
                elapsedMs += step
            }
        }

        fun tapRing() = launch {
            ring.snapTo(0f)
            ring.animateTo(1f, tween(PracticeDemo.TAP_MS.toInt(), easing = LinearOutSlowInEasing))
        }

        /**
         * A scroll into view or a finger move that something else cancels (a layout change, another animation) does
         * not stop the scene: without this the scene would end silently and the demo would stay on this step.
         */
        suspend fun tolerant(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }

        /**
         * Taps or types once the part takes it: right after a tap that opens a set the part can still be the closed
         * line for a moment (it takes no typing yet). At most [PracticeDemo.TARGET_WAIT_MS], so the demo never hangs.
         */
        suspend fun untilDone(action: () -> Boolean) {
            var waited = 0L
            while (!action() && waited < PracticeDemo.TARGET_WAIT_MS) {
                hold(STEP_MS)
                waited += STEP_MS
            }
        }

        /** The countdown after GO covers the screen: nothing to show or tap until it is gone (at most a minute). */
        suspend fun afterCountdown() {
            var waited = 0L
            while (targets.countdownShown && waited < 60_000L) {
                hold(STEP_MS)
                waited += STEP_MS
            }
        }

        for (action in scene.actions) {
            awaitPlaying()
            if (action !is DemoAction.Wait) afterCountdown()
            when (action) {
                is DemoAction.MoveTo -> {
                    var waited = 0L
                    while (!targets.has(action.target) && waited < PracticeDemo.TARGET_WAIT_MS) {
                        hold(STEP_MS)
                        waited += STEP_MS
                    }
                    if (!targets.has(action.target)) continue
                    tolerant { targets.bringIntoView(action.target) }
                    // The layout after scrolling reports the new place.
                    delay(STEP_MS)
                    focus = action.target
                    val bounds = targets.boundsOf(action.target)
                    if (bounds != null) {
                        val to = bounds.center
                        if (!fingerShown) {
                            val from = area?.let { Offset(it.center.x, it.bottom) } ?: to
                            finger.snapTo(from)
                            fingerShown = true
                        }
                        tolerant { finger.animateTo(to, tween(PracticeDemo.MOVE_MS.toInt(), easing = Glide)) }
                    }
                    elapsedMs += PracticeDemo.MOVE_MS
                }
                is DemoAction.Tap -> {
                    tapRing()
                    untilDone { targets.tap(action.target) }
                    hold(action.millis)
                }
                is DemoAction.Type -> {
                    for (k in 1..action.text.length) {
                        awaitPlaying()
                        tapRing()
                        untilDone { targets.type(action.target, action.text.take(k)) }
                        hold(PracticeDemo.KEY_MS)
                    }
                }
                is DemoAction.Wait -> hold(action.millis)
            }
        }
        // Played to the end: "‹" before the next scene starts goes back from that one's start.
        elapsedMs = 0L
    }
}

/**
 * The demo over the training screen (#99): the screen dims around the part the finger points at, a balloon explains
 * the step, and the finger taps with a ring. Touches on the screen below do nothing: the demo plays by itself.
 * [heavyTarget] and [lightTarget] are the rep targets of the method.
 */
@Composable
fun DemoOverlay(
    view: DemoView,
    targets: DemoTargets,
    step: DemoStep?,
    number: Int,
    total: Int,
    script: DemoScript,
    heavyTarget: Int,
    lightTarget: Int,
    modifier: Modifier = Modifier,
) {
    val area = view.area
    val focused = view.focus?.let { targets.boundsOf(it) }
    val density = LocalDensity.current
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { view.area = it.boundsInRoot() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    ) {
        if (area != null) {
            val padPx = with(density) { 6.dp.toPx() }
            val radiusPx = with(density) { 16.dp.toPx() }
            // The lit part in the overlay's own coordinates, a little larger than the part.
            val hole = focused?.let {
                Rect(it.left - area.left - padPx, it.top - area.top - padPx, it.right - area.left + padPx, it.bottom - area.top + padPx)
            }
            if (hole != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRoundRect(RoundRect(hole, CornerRadius(radiusPx)))
                    }
                    drawPath(path, Scrim)
                    drawRoundRect(Gym.Yellow, hole.topLeft, hole.size, CornerRadius(radiusPx), style = Stroke(2.dp.toPx()))
                }
            }
            if (step != null) {
                BalloonLayout(hole) {
                    Balloon(step, number, total, script, heavyTarget, lightTarget)
                }
            }
            if (view.fingerShown) {
                Canvas(Modifier.fillMaxSize().testTag(DEMO_FINGER_TAG)) {
                    val at = view.finger.value - area.topLeft
                    val t = view.ring.value
                    if (t < 1f) {
                        drawCircle(
                            Gym.Yellow.copy(alpha = 1f - t), radius = 18.dp.toPx() * (.4f + .8f * t), center = at,
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                    drawCircle(Color.Black.copy(alpha = .35f), radius = 11.dp.toPx(), center = at + Offset(0f, 2.dp.toPx()))
                    drawCircle(Gym.Cream.copy(alpha = .85f), radius = 9.dp.toPx(), center = at)
                    drawCircle(FingerEdge, radius = 9.dp.toPx(), center = at, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

/** Places the balloon under the lit part when it fits, else above it; without a lit part at the bottom. */
@Composable
private fun BalloonLayout(hole: Rect?, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val side = 14.dp.roundToPx()
        val gap = 12.dp.roundToPx()
        val width = (constraints.maxWidth - 2 * side).coerceAtLeast(0)
        val balloon = measurables.first().measure(Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight))
        val bottom = (constraints.maxHeight - side - balloon.height).coerceAtLeast(side)
        val y = if (hole == null) {
            bottom
        } else {
            val below = hole.bottom.roundToInt() + gap
            val above = hole.top.roundToInt() - gap - balloon.height
            when {
                below + balloon.height <= constraints.maxHeight - side -> below
                above >= side -> above
                else -> bottom
            }
        }
        layout(constraints.maxWidth, constraints.maxHeight) { balloon.place(side, y) }
    }
}

@Composable
private fun Balloon(step: DemoStep, number: Int, total: Int, script: DemoScript, heavyTarget: Int, lightTarget: Int) {
    val (title, body) = when (step) {
        DemoStep.GOAL -> stringResource(R.string.tour_goal_title) to stringResource(R.string.tour_goal_body, heavyTarget, lightTarget)
        DemoStep.STACKING -> stringResource(R.string.tour_stacking_title) to stringResource(R.string.tour_stacking_body)
        DemoStep.WARMUP -> stringResource(R.string.tour_warmup_title) to stringResource(R.string.tour_warmup_body)
        DemoStep.GO -> stringResource(R.string.tour_go_title) to stringResource(R.string.tour_go_body)
        DemoStep.HEAVY -> stringResource(R.string.tour_heavy_title, script.heavyReps) to stringResource(R.string.tour_heavy_body, heavyTarget)
        DemoStep.LIGHT -> stringResource(R.string.tour_light_title, script.lightReps) to stringResource(R.string.tour_light_body, lightTarget)
        DemoStep.NEXT_GOAL -> stringResource(R.string.tour_next_goal_title) to stringResource(R.string.tour_next_goal_body)
        DemoStep.FINISH -> stringResource(R.string.tour_finish_title) to stringResource(R.string.tour_finish_body)
    }
    Column(
        Modifier.fillMaxWidth().background(Gym.Cream, RoundedCornerShape(14.dp)).testTag(DEMO_BALLOON_TAG)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.tour_count, number, total), style = MaterialTheme.typography.labelSmall, color = BalloonMuted)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Gym.Bg)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Gym.Bg)
    }
}

/** Under the demo: ‹ back (a step again), Pause / Resume, and Skip (#99). */
@Composable
fun DemoControls(paused: Boolean, onBack: () -> Unit, onPause: () -> Unit, onSkip: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag(DEMO_BACK_TAG)) {
            Text("‹ " + stringResource(R.string.action_back), color = Gym.Cream)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onPause, modifier = Modifier.testTag(DEMO_PAUSE_TAG)) {
            Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null, tint = Gym.Yellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (paused) R.string.demo_resume else R.string.demo_pause), color = Gym.Yellow)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSkip, modifier = Modifier.testTag(DEMO_SKIP_TAG)) {
            Text(stringResource(R.string.tour_skip), color = Gym.Muted)
        }
    }
}
