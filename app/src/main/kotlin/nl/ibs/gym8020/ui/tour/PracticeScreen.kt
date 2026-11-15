package nl.ibs.gym8020.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.DemoEnd
import nl.ibs.gym8020.core.logic.DemoPlayback
import nl.ibs.gym8020.core.logic.DemoScript
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.PracticeDemo
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.PracticeWorkout
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.training.TrainingScreen

/** Test tag of the line "Practice workout · not saved" (F-11). */
const val PRACTICE_BANNER_TAG = "practice_banner"

/** Test tags of the short summary at the end of the practice workout and its two buttons. */
const val PRACTICE_DONE_TAG = "practice_done"
const val PRACTICE_AGAIN_TAG = "practice_again"
const val PRACTICE_CLOSE_TAG = "practice_close"

/** The practice workout: its script, its session in the practice database, where the demo is, and whether it is done. */
data class PracticeUi(
    val script: DemoScript? = null,
    /** Null while the practice workout is being (re)built for the scene the demo starts at. */
    val sessionId: Long? = null,
    val playback: DemoPlayback = DemoPlayback(),
    val finished: Boolean = false,
)

/**
 * The practice workout that plays itself (#99): builds the practice workout for the scene the demo starts at (the
 * first, or an earlier one after "‹", see [DemoScript.progressBefore]) and keeps where the demo is over a rotation.
 */
class PracticeViewModel(private val container: AppContainer) : ViewModel() {
    private val state = MutableStateFlow(PracticeUi())
    val ui: StateFlow<PracticeUi> = state
    private var building: Job? = null

    /** The scene whose actions began on the current practice session: its data is no longer the scene's start. */
    private var begun: Int? = null

    init {
        build()
    }

    /** Builds the practice workout as it is at the start of the current scene. */
    private fun build() {
        building?.cancel()
        begun = null
        state.update { it.copy(sessionId = null) }
        building = viewModelScope.launch {
            val script = state.value.script ?: container.settings.current().method.let {
                PracticeDemo.script(it.heavyTarget, it.lightTarget, it.goodMargin)
            }
            val id = PracticeWorkout.start(container, script.progressBefore(state.value.playback.scene))
            state.update { it.copy(script = script, sessionId = id) }
        }
    }

    /**
     * The demo is about to play [scene] of [take]. False when it should not: that is not where the demo is, or the
     * scene already began on this practice session (e.g. a rotation in the middle of it); it then starts over.
     */
    fun begin(take: Int, scene: Int): Boolean {
        val playback = state.value.playback
        if (playback.take != take || playback.scene != scene || playback.ended) return false
        if (begun == scene) {
            state.update { it.copy(playback = it.playback.restart()) }
            build()
            return false
        }
        begun = scene
        return true
    }

    /** [scene] of [take] played to its end. */
    fun sceneDone(take: Int, scene: Int) {
        state.update { ui ->
            val count = ui.script?.scenes?.size ?: return@update ui
            if (ui.playback.take == take && ui.playback.scene == scene) ui.copy(playback = ui.playback.sceneDone(count)) else ui
        }
    }

    fun togglePause() = state.update { it.copy(playback = it.playback.togglePause()) }

    /** "‹": the previous scene, or this one again, from its start. */
    fun back(elapsedMs: Long) {
        if (state.value.playback.ended) return
        state.update { it.copy(playback = it.playback.back(elapsedMs)) }
        build()
    }

    fun skip() = state.update { it.copy(playback = it.playback.skip()) }

    /** The practice training was finished (the demo tapped "Finish"). */
    fun finish() = state.update { it.copy(finished = true) }

    /** "Watch again": the demo from the start, on a new practice workout. */
    fun again() {
        state.update { it.copy(playback = it.playback.again(), finished = false) }
        build()
    }
}

/**
 * The practice workout that plays itself (#99): the training screen on the practice database, with "Practice workout ·
 * not saved" on top and the demo over it (a finger taps and types, a balloon explains), and ‹ back, Pause and Skip
 * below. After the demo a short summary. Nothing here touches your own data.
 */
@Composable
fun PracticeScreen(container: AppContainer, onExit: () -> Unit) {
    val vm: PracticeViewModel = viewModel(key = "practice") { PracticeViewModel(container) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val targets = remember { DemoTargets() }
    val view = remember { DemoView() }
    val paused = rememberUpdatedState(ui.playback.paused)
    val playback = ui.playback
    val done = ui.finished || playback.end == DemoEnd.FINISHED

    // Skip: the demo stops and the practice workout closes.
    LaunchedEffect(playback.end) {
        if (playback.end == DemoEnd.SKIPPED) onExit()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        PracticeBanner()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val id = ui.sessionId
            val script = ui.script
            if (id != null && done) {
                PracticeSummary(container, id, settings.unit, onAgain = vm::again, onDone = onExit)
            } else if (id != null && script != null) {
                key(id) {
                    CompositionLocalProvider(LocalDemo provides targets) {
                        TrainingScreen(
                            container.practice, id,
                            onFinished = { vm.finish() },
                            // Not while the practice workout is rebuilt for "‹": then this session is gone on purpose.
                            onExit = { if (vm.ui.value.sessionId == id) onExit() },
                            // Your own exercises are not in the practice workout.
                            onEditExercise = {},
                            countdownSeconds = PracticeDemo.COUNTDOWN_SECONDS,
                        )
                    }
                    // The demo plays scene after scene by itself; Pause holds it, "‹" builds an earlier scene.
                    LaunchedEffect(playback.take, playback.scene, playback.ended) {
                        val scene = script.scenes.getOrNull(playback.scene) ?: return@LaunchedEffect
                        if (!vm.begin(playback.take, playback.scene)) return@LaunchedEffect
                        view.play(scene, targets, paused)
                        vm.sceneDone(playback.take, playback.scene)
                    }
                }
                DemoOverlay(
                    view, targets,
                    step = script.scenes.getOrNull(playback.scene)?.step,
                    number = playback.scene + 1, total = script.scenes.size, script = script,
                    heavyTarget = settings.method.heavyTarget, lightTarget = settings.method.lightTarget,
                )
            }
        }
        if (!done) {
            DemoControls(
                paused = playback.paused,
                onBack = { vm.back(view.elapsedMs) },
                onPause = vm::togglePause,
                onSkip = vm::skip,
            )
        }
    }
}

/** "Practice workout · not saved", in the light blue of the app's advice, as in the prototype. */
@Composable
private fun PracticeBanner() {
    Text(
        stringResource(R.string.practice_banner).uppercase(),
        color = Color(0xFF10202E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Gym.Advice, RoundedCornerShape(10.dp)).padding(vertical = 4.dp).testTag(PRACTICE_BANNER_TAG),
    )
}

/** What the summary shows: the sets you did and the target you would have kept. */
private data class PracticeResult(val sets: List<SetEntity>, val target: List<PlannedSet>)

/** The short summary at the end: your sets, your next target, that nothing was saved; watch again or close. */
@Composable
private fun PracticeSummary(container: AppContainer, sessionId: Long, unit: WeightUnit, onAgain: () -> Unit, onDone: () -> Unit) {
    BackHandler { onDone() }
    val result by produceState<PracticeResult?>(null, sessionId) {
        val practice = container.practice
        val sets = practice.db.sessions().sets(sessionId).filter { it.role.isWork && it.reps != null }
        val exercise = practice.db.exercises().all().firstOrNull { it.libraryKey == PracticeWorkout.EXERCISE_KEY }
        val target = exercise?.let { practice.training.target(it.id)?.sets }.orEmpty().filter { it.role.isWork }
        value = PracticeResult(sets, target)
    }
    val heavy = stringResource(R.string.role_heavy_short)
    val light = stringResource(R.string.role_light_short)
    fun letter(role: SetRole) = if (role == SetRole.LIGHT) light else heavy
    fun kg(value: Double) = WeightFormat.format(value, unit) + " " + unit.name.lowercase()

    Column(
        Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp).testTag(PRACTICE_DONE_TAG),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(120.dp)) { Sunburst(Modifier.fillMaxSize()) }
        Text(stringResource(R.string.practice_done_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.practice_done_body), style = MaterialTheme.typography.bodyLarge)
        result?.let { r ->
            if (r.sets.isNotEmpty()) {
                DecoCard(corners = true, padding = 16.dp) {
                    Eyebrow(stringResource(R.string.practice_done_sets))
                    r.sets.forEach { s ->
                        Text(
                            letter(s.role) + "  " + listOfNotNull(s.weightKg?.let(::kg), s.reps?.let { "× $it" }).joinToString(" "),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (r.target.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Eyebrow(stringResource(R.string.tr_next_time), color = Gym.Gold)
                        Text(
                            r.target.joinToString("  ·  ") { p -> letter(p.role) + " " + (p.weightKg?.let(::kg) ?: "–") },
                            style = MaterialTheme.typography.titleMedium, color = Gym.Yellow,
                        )
                    }
                }
            }
        }
        Text(stringResource(R.string.practice_done_again), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        LineButton(
            stringResource(R.string.practice_again), onClick = onAgain, icon = Icons.Filled.Replay,
            modifier = Modifier.fillMaxWidth().testTag(PRACTICE_AGAIN_TAG),
        )
        GoButton(stringResource(R.string.action_done), onClick = onDone, modifier = Modifier.testTag(PRACTICE_CLOSE_TAG))
    }
}
