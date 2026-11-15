package nl.ibs.gym8020.ui.tour

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import nl.ibs.gym8020.core.logic.Tour
import nl.ibs.gym8020.core.logic.TourStep
import nl.ibs.gym8020.core.logic.TourView

/** A part of the screen the tour can light up: where it is (in the root), and how tall it is in full. */
private class Placed(val owner: Any, val bounds: Rect, val fullHeight: Float)

/**
 * The tour of the practice workout (#97) on the screen: which parts are where, and which step shows. The parts mark
 * themselves with [tourTarget]; the logic of the steps is [Tour] (core).
 */
@Stable
class TourState(initial: Tour = Tour()) {
    var tour: Tour by mutableStateOf(initial)
        private set

    /** The area of the overlay, in the root: what counts as "on the screen". */
    var viewport: Rect? by mutableStateOf(null)

    private val placed = mutableStateMapOf<TourStep, Placed>()

    private val present: Set<TourStep> get() = placed.keys.toSet()

    private val inView: Set<TourStep>
        get() {
            val view = viewport ?: return emptySet()
            return placed.filterValues { p ->
                val visible = p.bounds.intersect(view)
                TourView.inView(p.fullHeight, if (visible.width > 0f) visible.height.coerceAtLeast(0f) else 0f)
            }.keys
        }

    /** The step with its balloon now; null while the tour waits for a part, or when it is over. */
    val showing: TourStep? get() = tour.showing(present, inView)

    /** Where the tour is for the balloon's "2 / 6", after passing parts that will not come. */
    val position: Tour get() = tour.catchUp(present)

    /** Where the part of [step] is, in the root. */
    fun boundsOf(step: TourStep): Rect? = placed[step]?.bounds

    /** "Next" in the balloon. */
    fun next() {
        tour = tour.catchUp(present).next()
    }

    /** "Skip" in the balloon. */
    fun skip() {
        tour = tour.skip()
    }

    internal fun place(step: TourStep, owner: Any, coordinates: LayoutCoordinates) {
        val height = coordinates.size.height.toFloat()
        if (!coordinates.isAttached || height <= 0f) {
            remove(step, owner)
            return
        }
        val next = Placed(owner, coordinates.boundsInRoot(), height)
        val now = placed[step]
        if (now == null || now.owner !== owner || now.bounds != next.bounds || now.fullHeight != next.fullHeight) placed[step] = next
    }

    internal fun remove(step: TourStep, owner: Any) {
        if (placed[step]?.owner === owner) placed.remove(step)
    }

    companion object {
        /** Keeps where the tour is over a rotation. */
        val Saver: Saver<TourState, List<Int>> = Saver(
            save = { listOf(it.tour.done, if (it.tour.skipped) 1 else 0) },
            restore = { TourState(Tour(done = it[0], skipped = it[1] == 1)) },
        )
    }
}

/** The tour of the practice workout; null outside it, so real trainings never show balloons (#97). */
val LocalTour = staticCompositionLocalOf<TourState?> { null }

/**
 * Marks this part of the screen as the part of [step] in the tour; [active] false leaves it unmarked (e.g. reps only
 * after GO). While its balloon shows, the part wiggles gently, twice. Does nothing outside the practice workout.
 */
fun Modifier.tourTarget(step: TourStep, active: Boolean = true): Modifier = composed {
    val tour = LocalTour.current ?: return@composed Modifier
    val owner = remember { Any() }
    DisposableEffect(tour, step, active) { onDispose { tour.remove(step, owner) } }
    val angle = remember { Animatable(0f) }
    val showing = active && tour.showing == step
    LaunchedEffect(showing) {
        if (!showing) {
            angle.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(2) {
            angle.animateTo(-1.2f, tween(225, easing = FastOutSlowInEasing))
            angle.animateTo(1.2f, tween(450, easing = FastOutSlowInEasing))
            angle.animateTo(0f, tween(225, easing = FastOutSlowInEasing))
        }
    }
    if (!active) {
        Modifier
    } else {
        Modifier
            .onGloballyPositioned { tour.place(step, owner, it) }
            .graphicsLayer { rotationZ = angle.value }
    }
}
