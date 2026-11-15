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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.WeightFormat
import nl.ibs.gym8020.core.logic.WeightUnit
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.PracticeWorkout
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.deco.Sunburst
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.training.TrainingScreen

/** Test tag of the line "Practice workout · not saved" (F-11). */
const val PRACTICE_BANNER_TAG = "practice_banner"

/** Test tag of the short summary at the end of the practice workout. */
const val PRACTICE_DONE_TAG = "practice_done"

/** The countdown before GO in the practice workout: short, no real rest (#97). */
private const val PRACTICE_COUNTDOWN_SECONDS = 2

/** Starts a practice workout once per screen (it survives a rotation) and knows when it is finished. */
class PracticeViewModel(private val container: AppContainer) : ViewModel() {
    private val session = MutableStateFlow<Long?>(null)
    private val done = MutableStateFlow(false)
    val sessionId: StateFlow<Long?> = session
    val finished: StateFlow<Boolean> = done

    init {
        viewModelScope.launch { session.value = PracticeWorkout.start(container) }
    }

    fun finish() {
        done.value = true
    }
}

/**
 * The practice workout with the tour (#97): the training screen on the practice database, with "Practice workout · not
 * saved" on top and the tour over it; after finishing a short summary. Nothing here touches your own data.
 */
@Composable
fun PracticeScreen(container: AppContainer, onExit: () -> Unit) {
    val vm: PracticeViewModel = viewModel(key = "practice") { PracticeViewModel(container) }
    val sessionId by vm.sessionId.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val tour = rememberSaveable(saver = TourState.Saver) { TourState() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PracticeBanner()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val id = sessionId
            if (id != null && finished) {
                PracticeSummary(container, id, settings.unit, onDone = onExit)
            } else if (id != null) {
                CompositionLocalProvider(LocalTour provides tour) {
                    TrainingScreen(
                        container.practice, id,
                        onFinished = { vm.finish() },
                        onExit = onExit,
                        // Your own exercises are not in the practice workout.
                        onEditExercise = {},
                        countdownSeconds = PRACTICE_COUNTDOWN_SECONDS,
                    )
                }
                TourOverlay(tour, settings.method.heavyTarget, settings.method.lightTarget)
            }
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

/** The short summary at the end: your sets, your next target, and that nothing was saved. */
@Composable
private fun PracticeSummary(container: AppContainer, sessionId: Long, unit: WeightUnit, onDone: () -> Unit) {
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
        GoButton(stringResource(R.string.action_done), onClick = onDone)
    }
}
