package nl.ibs.gym8020.ui.tour

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import nl.ibs.gym8020.core.logic.DemoTarget

/** What the demo can do with a part: tap it, type into it, scroll it into view. The lambdas are always the latest. */
@OptIn(ExperimentalFoundationApi::class)
internal class TargetActions(
    val tap: State<(() -> Unit)?>,
    val type: State<((String) -> Unit)?>,
    val requester: BringIntoViewRequester,
)

/** A part of the screen the demo can point at: where it is (in the root, as far as it is visible). */
private class Placed(val owner: Any, val bounds: Rect, val actions: TargetActions)

/**
 * The parts of the training screen in the practice workout (#99): where each is and what a tap or typing does there.
 * The parts mark themselves with [demoTarget]; the demo ([DemoView.play]) performs its script on them. Taps and typing
 * run the same code as the screen's own buttons and fields, without touch events.
 */
@Stable
class DemoTargets {
    private val placed = mutableStateMapOf<DemoTarget, Placed>()

    /** True while the countdown after GO covers the screen: the demo waits with the next part until it is gone. */
    var countdownShown by mutableStateOf(false)

    /** Where [target] is, in the root; null while it is not on the screen. */
    fun boundsOf(target: DemoTarget): Rect? = placed[target]?.bounds?.takeIf { it.width > 0f && it.height > 0f }

    fun has(target: DemoTarget): Boolean = target in placed

    /** Taps [target]; false when it is not there or does nothing on a tap. */
    fun tap(target: DemoTarget): Boolean {
        val action = placed[target]?.actions?.tap?.value ?: return false
        action()
        return true
    }

    /** Types [text] (everything typed so far) into [target]; false when it takes no typing. */
    fun type(target: DemoTarget, text: String): Boolean {
        val action = placed[target]?.actions?.type?.value ?: return false
        action(text)
        return true
    }

    /** Scrolls [target] into view, when it is there. */
    @OptIn(ExperimentalFoundationApi::class)
    suspend fun bringIntoView(target: DemoTarget) {
        placed[target]?.actions?.requester?.bringIntoView()
    }

    internal fun place(target: DemoTarget, owner: Any, coordinates: LayoutCoordinates, actions: TargetActions) {
        if (!coordinates.isAttached || coordinates.size.height <= 0) {
            remove(target, owner)
            return
        }
        val bounds = coordinates.boundsInRoot()
        val now = placed[target]
        if (now == null || now.owner !== owner || now.bounds != bounds) placed[target] = Placed(owner, bounds, actions)
    }

    internal fun remove(target: DemoTarget, owner: Any) {
        if (placed[target]?.owner === owner) placed.remove(target)
    }
}

/** The demo of the practice workout; null outside it, so real trainings have no demo targets (#97, #99). */
val LocalDemo = staticCompositionLocalOf<DemoTargets?> { null }

/**
 * Marks this part of the screen as [target] for the demo of the practice workout; [active] false leaves it unmarked
 * (e.g. the reps only after GO). [onTap] is what a tap by the demo does (the same as the part's own tap), [onType] what
 * typing does (it gets everything typed so far). Does nothing outside the practice workout.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.demoTarget(
    target: DemoTarget,
    active: Boolean = true,
    onTap: (() -> Unit)? = null,
    onType: ((String) -> Unit)? = null,
): Modifier = composed {
    val demo = LocalDemo.current ?: return@composed Modifier
    val owner = remember { Any() }
    val tap = rememberUpdatedState(onTap)
    val type = rememberUpdatedState(onType)
    val requester = remember { BringIntoViewRequester() }
    val actions = remember { TargetActions(tap, type, requester) }
    DisposableEffect(demo, target, active) { onDispose { demo.remove(target, owner) } }
    if (!active) {
        Modifier
    } else {
        Modifier
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { demo.place(target, owner, it, actions) }
    }
}
