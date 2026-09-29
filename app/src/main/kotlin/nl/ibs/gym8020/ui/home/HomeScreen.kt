package nl.ibs.gym8020.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.logic.DoneTraining
import nl.ibs.gym8020.core.logic.HomeAdvice
import nl.ibs.gym8020.core.logic.HomeAdvisor
import nl.ibs.gym8020.core.logic.PlanSlot
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.data.movementOrDefault
import nl.ibs.gym8020.ui.deco.MovementStrip
import nl.ibs.gym8020.data.db.BodyweightEntity
import nl.ibs.gym8020.data.db.SchemaEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.WeekPlanEntity
import nl.ibs.gym8020.data.toLocalDate
import nl.ibs.gym8020.ui.components.DecoCard
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.components.LineButton
import nl.ibs.gym8020.ui.components.PlainCard
import nl.ibs.gym8020.ui.components.WeekPips
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.deco.IbsMark
import nl.ibs.gym8020.ui.deco.Lattice
import nl.ibs.gym8020.ui.theme.Gym
import nl.ibs.gym8020.ui.training.BodyweightPrompt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class HomeState(
    val today: LocalDate = LocalDate.now(),
    val plan: WeekPlanEntity? = null,
    val advice: HomeAdvice? = null,
    val schemas: Map<Long, SchemaEntity> = emptyMap(),
    val entryCounts: Map<Long, Int> = emptyMap(),
    val schemaIcons: Map<Long, List<Movement>> = emptyMap(),
    val planSchemaIds: List<Long> = emptyList(),
    val openSession: SessionEntity? = null,
    val lastBodyweightDate: LocalDate? = null,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val finished = c.db.sessions().observeFinished()

    val state: StateFlow<HomeState> = combine(
        c.db.weekPlans().observeActive(),
        c.db.weekPlans().observeAllSlots(),
        combine(c.db.schemas().observeActive(), c.db.schemas().observeAllEntries(), c.db.exercises().observeAll()) { s, e, x -> Triple(s, e, x) },
        combine(finished, c.db.sessions().observeOpen()) { f, o -> f to o },
        c.db.bodyweights().observeAll(),
    ) { plan, allSlots, (schemas, entries, exercises), (done, open), bodyweights ->
        val exById = exercises.associateBy { it.id }
        val today = LocalDate.now()
        val schemaMap = schemas.associateBy { it.id }
        val slots = allSlots.filter { it.weekPlanId == plan?.id && it.schemaId in schemaMap }
        val advice = plan?.let {
            HomeAdvisor.advise(
                today,
                slots.map { s -> PlanSlot(s.schemaId, s.day?.let(DayOfWeek::of)) },
                it.targetPerWeek,
                done.map { s -> DoneTraining(s.startedAt.toLocalDate(), s.schemaId) },
            )
        }
        HomeState(
            today = today,
            plan = plan,
            advice = advice,
            schemas = schemaMap,
            entryCounts = entries.groupingBy { it.schemaId }.eachCount(),
            schemaIcons = entries.groupBy { it.schemaId }.mapValues { (_, l) -> l.mapNotNull { exById[it.exerciseId]?.movementOrDefault } },
            planSchemaIds = slots.map { it.schemaId }.distinct(),
            openSession = open,
            lastBodyweightDate = bodyweights.firstOrNull()?.epochDay?.let(LocalDate::ofEpochDay),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** Saves today's body weight first (if given), so the new session picks it up. */
    fun start(schemaId: Long, bodyweightKg: Double? = null, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            if (bodyweightKg != null) {
                c.db.bodyweights().upsert(BodyweightEntity(epochDay = LocalDate.now().toEpochDay(), kg = bodyweightKg))
            }
            onStarted(c.training.startSession(schemaId))
        }
    }

    fun discard(id: Long) {
        viewModelScope.launch { c.training.discard(id) }
    }

    fun finish(id: Long) {
        viewModelScope.launch { c.training.finish(id) }
    }
}

@Composable
fun HomeScreen(container: AppContainer, onOpenTraining: (Long) -> Unit, onAllSchemas: () -> Unit) {
    val vm: HomeViewModel = viewModel { HomeViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    var pendingStart by remember { mutableStateOf<Long?>(null) }

    fun requestStart(schemaId: Long) {
        if (HomeAdvisor.shouldAskBodyweight(state.today, state.lastBodyweightDate)) pendingStart = schemaId
        else vm.start(schemaId, null, onOpenTraining)
    }

    pendingStart?.let { schemaId ->
        BodyweightPrompt(
            onSave = { kg -> pendingStart = null; vm.start(schemaId, kg, onOpenTraining) },
            onSkip = { pendingStart = null; vm.start(schemaId, null, onOpenTraining) },
            onDismiss = { pendingStart = null },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), Modifier.weight(1f))
                IbsMark(Modifier.size(26.dp))
            }

            state.openSession?.let { open ->
                val old = System.currentTimeMillis() - open.startedAt > 4 * 60 * 60 * 1000L
                DecoCard {
                    Eyebrow(stringResource(R.string.home_resume_title), color = Gym.Gold)
                    Text(open.schemaName, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    GoButton(stringResource(R.string.home_resume), onClick = { onOpenTraining(open.id) }, height = 52.dp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (old) LineButton(stringResource(R.string.home_finish_old), onClick = { vm.finish(open.id) }, modifier = Modifier.weight(1f))
                        LineButton(stringResource(R.string.home_discard), onClick = { vm.discard(open.id) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            val advice = state.advice
            if (state.plan == null || advice == null) {
                DecoCard(corners = true) {
                    Text(stringResource(R.string.home_no_plan), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            } else if (state.openSession == null) {
                AdviceCard(advice, state, onStart = ::requestStart)
            }

            DecoDivider()
            Eyebrow(stringResource(R.string.home_other_schema))
            val others = state.planSchemaIds.mapNotNull { state.schemas[it] } +
                state.schemas.values.filter { it.id !in state.planSchemaIds }.sortedBy { it.name }.take(3)
            others.forEach { schema ->
                PlainCard(onClick = { if (state.openSession == null) requestStart(schema.id) else onOpenTraining(state.openSession!!.id) }) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(schema.name, style = MaterialTheme.typography.titleMedium)
                        MovementStrip(state.schemaIcons[schema.id].orEmpty())
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gym.Yellow)
                }
            }
            LineButton(stringResource(R.string.home_all_schemas), onClick = onAllSchemas, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AdviceCard(advice: HomeAdvice, state: HomeState, onStart: (Long) -> Unit) {
    val suggested: Long? = when (advice) {
        is HomeAdvice.Today -> advice.schemaId
        is HomeAdvice.RestDay -> advice.suggestion
        is HomeAdvice.Next -> advice.schemaId
        is HomeAdvice.DoneForToday -> null
    }
    val label = when (advice) {
        is HomeAdvice.Today -> stringResource(R.string.home_today)
        is HomeAdvice.RestDay -> stringResource(R.string.home_rest_day)
        is HomeAdvice.Next -> stringResource(R.string.home_next)
        is HomeAdvice.DoneForToday -> stringResource(R.string.home_done_for_today)
    }
    DecoCard(corners = true, padding = 22.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow(label)
            val schema = suggested?.let { state.schemas[it] }
            if (advice is HomeAdvice.DoneForToday) {
                Text(stringResource(R.string.home_done_for_today), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            } else if (schema != null) {
                Text(schema.name, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text(stringResource(R.string.home_exercises_count, state.entryCounts[schema.id] ?: 0), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
                MovementStrip(state.schemaIcons[schema.id].orEmpty())
            }
            Spacer(Modifier.height(4.dp))
            WeekPips(advice.doneThisWeek, advice.targetPerWeek)
            Text(
                stringResource(R.string.home_week_progress, advice.doneThisWeek, advice.targetPerWeek),
                style = MaterialTheme.typography.bodySmall, color = Gym.Muted,
            )
        }
    }
    val startId = suggested ?: state.planSchemaIds.firstOrNull()
    if (startId != null) {
        val text = when (advice) {
            is HomeAdvice.DoneForToday, is HomeAdvice.RestDay -> stringResource(R.string.home_start_anyway)
            else -> stringResource(R.string.home_start)
        }
        GoButton(text, onClick = { onStart(startId) })
    }
}
