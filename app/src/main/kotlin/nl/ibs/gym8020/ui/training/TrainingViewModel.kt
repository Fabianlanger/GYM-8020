package nl.ibs.gym8020.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.core.logic.FinishCheck
import nl.ibs.gym8020.core.logic.FinishExercise
import nl.ibs.gym8020.core.logic.FinishSet
import nl.ibs.gym8020.core.logic.Finding
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.Warmup
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.Calculators
import nl.ibs.gym8020.data.LastPerformance
import nl.ibs.gym8020.data.Target
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity

/** Per-exercise data that is loaded once (history). */
data class ExerciseExtra(
    val last: LastPerformance?,
    val lastNote: String?,
    /** Only when there is no history: similar exercises as a reference (D-68). */
    val similar: List<Pair<ExerciseEntity, LastPerformance>> = emptyList(),
)

/** The plan for next time as edited on the training screen. */
data class PlanDraft(val sets: List<PlannedSet>, val note: String = "", val edited: Boolean = false)

data class TrainingItem(
    val se: SessionExerciseEntity,
    val exercise: ExerciseEntity,
    val sets: List<SetEntity>,
    val extra: ExerciseExtra?,
    /** The plan for next time: the user's edited draft or the live suggestion. Null until reps are entered. */
    val plan: PlanDraft?,
    val note: String = "",
    /** Achievable weights and plate plans from the exercise's material profile (v0.2). */
    val calc: WeightCalculator? = null,
    /** Pull-ups and dips: warm-up assistance as signed extra (negative), when the body weight is known (B-4). */
    val warmupAssistKg: Double? = null,
) {
    val hasInput: Boolean get() = sets.any { it.reps != null }
}

data class TrainingState(
    val session: SessionEntity? = null,
    val items: List<TrainingItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    val loaded: Boolean = false,
)

fun MethodSettings.targetFor(role: SetRole, exercise: ExerciseEntity): Int? = when (role) {
    SetRole.HEAVY -> exercise.heavyTarget ?: heavyTarget
    SetRole.LIGHT -> exercise.lightTarget ?: lightTarget
    SetRole.WARMUP -> null
}

class TrainingViewModel(private val c: AppContainer, private val sessionId: Long) : ViewModel() {
    private val repo = c.training
    private val extras = MutableStateFlow<Map<Long, ExerciseExtra>>(emptyMap())
    private val drafts = MutableStateFlow<Map<Long, PlanDraft>>(emptyMap())
    private val notes = MutableStateFlow<Map<Long, String>>(emptyMap())
    /** Targets you set yourself earlier in this session, read back after leaving the screen (B-6). */
    private val ownTargets = MutableStateFlow<Map<Long, PlanDraft>>(emptyMap())

    val state: StateFlow<TrainingState> = combine(
        c.db.sessions().observe(sessionId),
        c.db.sessions().observeExercises(sessionId),
        c.db.sessions().observeSets(sessionId),
        combine(c.db.exercises().observeAll(), c.settings.settings, c.db.materials().observeAll()) { e, s, m -> Triple(e, s, m) },
        combine(extras, combine(drafts, ownTargets) { d, o -> o + d }, notes) { e, d, n -> Triple(e, d, n) },
    ) { session, ses, sets, (exercises, settings, materials), (extraMap, draftMap, noteMap) ->
        val byId = exercises.associateBy { it.id }
        val profiles = materials.associateBy { it.id }
        val setsBySe = sets.groupBy { it.sessionExerciseId }
        val items = ses.mapNotNull { se ->
            val ex = byId[se.exerciseId] ?: return@mapNotNull null
            val list = setsBySe[se.id].orEmpty()
            val calc = Calculators.forExercise(ex, profiles, settings.plateStock)
            val plan = (draftMap[se.id] ?: suggestion(ex, list, settings.method, calc))?.let { p -> noteMap[se.id]?.let { p.copy(note = it) } ?: p }
            val assist = if (Warmup.needsAssistance(ex.loadType, ex.bodyweightFactor)) {
                Warmup.assistKg(session?.bodyweightKg, ex.bodyweightFactor, settings.warmupAssistPercent, calc)
            } else null
            TrainingItem(se, ex, list, extraMap[ex.id], plan, noteMap[se.id] ?: plan?.note ?: "", calc, assist)
        }
        items.forEach {
            if (it.extra == null) loadExtra(it.exercise.id)
            loadOwnTarget(it.se.id, it.exercise.id)
        }
        TrainingState(session, items, settings, exercises.filter { !it.archived }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingState())

    private val loading = HashSet<Long>()
    private val loadingTargets = HashSet<Long>()

    private fun loadOwnTarget(seId: Long, exerciseId: Long) {
        if (!loadingTargets.add(seId)) return
        viewModelScope.launch {
            val own = repo.targetSetIn(seId, exerciseId) ?: return@launch
            ownTargets.update { it + (seId to PlanDraft(own.sets, own.note.orEmpty(), edited = true)) }
        }
    }

    private fun loadExtra(exerciseId: Long) {
        if (!loading.add(exerciseId)) return
        viewModelScope.launch {
            val last = repo.lastPerformance(exerciseId, sessionId)
            val similar = if (last == null) repo.similarPerformances(exerciseId, sessionId) else emptyList()
            val extra = ExerciseExtra(last, repo.planNote(exerciseId), similar)
            extras.update { it + (exerciseId to extra) }
        }
    }

    private fun suggestion(ex: ExerciseEntity, sets: List<SetEntity>, method: MethodSettings, calc: WeightCalculator?): PlanDraft? {
        if (sets.none { it.reps != null }) return null
        val performed = sets.map { PerformedSet(it.role, it.weightKg, it.reps) }
        val m = method.copy(
            heavyTarget = ex.heavyTarget ?: method.heavyTarget,
            lightTarget = ex.lightTarget ?: method.lightTarget,
        )
        return PlanDraft(GoalAdvisor.planNext(performed, ex.loadType, ex.progression, m, calc))
    }

    // ---- timestamps ----
    fun shown(seId: Long) = launch { repo.markShown(seId) }
    fun done(seId: Long) = launch { repo.markDone(seId); persistPlan(seId) }
    fun go(seId: Long) = launch { repo.markGo(seId) }
    fun firstInput(seId: Long) = launch { repo.markFirstInput(seId) }

    /** Starts the warm-up: timer mark plus a warm-up set if there is none yet. */
    fun startWarmup(seId: Long) = launch {
        repo.markWarmup(seId)
        val item = state.value.items.firstOrNull { it.se.id == seId }
        if (item != null && item.sets.none { it.role == SetRole.WARMUP }) repo.addSet(seId, SetRole.WARMUP)
    }

    // ---- sets ----
    fun updateSet(set: SetEntity) = launch { repo.updateSet(set) }
    fun addSet(seId: Long, role: SetRole) = launch { repo.addSet(seId, role) }
    fun deleteSet(id: Long) = launch { repo.deleteSet(id) }
    fun toggleRole(set: SetEntity) = launch {
        val next = if (set.role == SetRole.HEAVY) SetRole.LIGHT else SetRole.HEAVY
        repo.updateSet(set.copy(role = next))
    }

    // ---- exercise order ----
    fun skip(seId: Long, skipped: Boolean) = launch { repo.setSkipped(seId, skipped) }
    fun moveToEnd(seId: Long) = launch { repo.moveToEnd(sessionId, seId) }
    fun addExercise(exerciseId: Long) = launch { repo.addExercise(sessionId, exerciseId) }

    // ---- plan for next time (D-18a) ----
    /** You change your target: shown at once and stored right away (B-6). */
    fun editPlan(seId: Long, transform: (PlanDraft) -> PlanDraft) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val current = item.plan ?: return
        val next = transform(current).copy(note = item.note, edited = true)
        drafts.update { it + (seId to next) }
        launch { repo.setTarget(seId, item.exercise.id, Target(next.sets, next.note)) }
    }

    /** A note is part of your target, so it is stored right away as well. */
    fun setPlanNote(seId: Long, note: String) {
        notes.update { it + (seId to note) }
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val plan = item.plan ?: return
        drafts.update { it + (seId to plan.copy(note = note, edited = true)) }
        launch { repo.setTarget(seId, item.exercise.id, Target(plan.sets, note)) }
    }

    /** Moving on or finishing: the advice becomes the target only if you did not set one yourself. */
    private suspend fun persistPlan(seId: Long) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val plan = item.plan ?: return
        if (item.se.skipped || plan.sets.isEmpty() || plan.edited) return
        repo.storeAdvice(seId, item.exercise.id, Target(plan.sets, plan.note))
    }

    /** The check before finishing (#47): what is missing in your performance and your target. */
    fun finishCheck(): List<Finding> = FinishCheck.check(
        state.value.items.map { item ->
            FinishExercise(
                sessionExerciseId = item.se.id,
                exerciseId = item.exercise.id,
                progression = item.exercise.progression,
                loadType = item.exercise.loadType,
                sets = item.sets.map { FinishSet(it.role, it.weightKg, it.reps, it.goalWeightKg) },
                skipped = item.se.skipped,
                target = item.plan?.sets,
            )
        },
    )

    fun finish(onFinished: () -> Unit) = launch {
        state.value.items.forEach { persistPlan(it.se.id) }
        repo.finish(sessionId)
        onFinished()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

/** Weight step from the settings until material profiles exist (v0.2). */
val AppSettings.weightStep: Double get() = method.weightStepKg
