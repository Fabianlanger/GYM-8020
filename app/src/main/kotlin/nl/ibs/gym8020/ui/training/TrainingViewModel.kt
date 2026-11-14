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
import nl.ibs.gym8020.core.logic.BodyShare
import nl.ibs.gym8020.core.logic.DefaultTarget
import nl.ibs.gym8020.core.logic.FinishCheck
import nl.ibs.gym8020.core.logic.FinishExercise
import nl.ibs.gym8020.core.logic.FinishSet
import nl.ibs.gym8020.core.logic.Finding
import nl.ibs.gym8020.core.logic.LastSet
import nl.ibs.gym8020.core.logic.LastTime
import nl.ibs.gym8020.core.logic.OwnSets
import nl.ibs.gym8020.core.logic.PastSession
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.ProgressionSwitch
import nl.ibs.gym8020.core.logic.RepsTarget
import nl.ibs.gym8020.core.logic.SetByYou
import nl.ibs.gym8020.core.logic.Warmup
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.data.AppSettings
import nl.ibs.gym8020.data.Calculators
import nl.ibs.gym8020.data.LastPerformance
import nl.ibs.gym8020.data.Target
import nl.ibs.gym8020.data.plateStockFor
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
    /**
     * Your stored target as this session started: the sets and previous values of the default target (#52). A
     * bodyweight exercise's weights as the signed load of this training's day, exact (#87).
     */
    val stored: List<PlannedSet>? = null,
    /** The set roles from the workout, for the default target when there is no stored target. */
    val workout: List<SetRole> = emptyList(),
    /** Earlier trainings of this exercise, for "Laatste keer" per set (#65). */
    val history: List<PastSession> = emptyList(),
)

/**
 * The plan for next time as edited on the training screen. [yours] says per set whether you set it yourself (#92);
 * [edited] whether you set at least one set yourself.
 */
data class PlanDraft(val sets: List<PlannedSet>, val note: String = "", val edited: Boolean = false, val yours: List<Boolean> = emptyList()) {
    /** The sets with, per set, whether you set it yourself; without [yours] every set counts as [edited]. */
    fun own(): OwnSets = OwnSets(sets, if (yours.size == sets.size) yours else sets.map { edited })
}

/**
 * An advice you took over (#63): the weight it set and the target it replaced, for "Ongedaan"; [wasYours] whether you
 * had set that set yourself before (#92).
 */
data class TakenAdvice(val adviceKg: Double, val previousKg: Double?, val wasYours: Boolean = false)

data class TrainingItem(
    val se: SessionExerciseEntity,
    val exercise: ExerciseEntity,
    val sets: List<SetEntity>,
    val extra: ExerciseExtra?,
    /** Your target for next time: what you set yourself, else the default target (#52). Null until reps are entered. */
    val plan: PlanDraft?,
    val note: String = "",
    /** Achievable weights and plate plans: this session's material (#48), else the exercise's profile (v0.2). */
    val calc: WeightCalculator? = null,
    /** Pull-ups and dips: warm-up assistance as signed extra (negative), when the body weight is known (B-4). */
    val warmupAssistKg: Double? = null,
    /**
     * The progression of today's sets. Switching "volgende keer op gewicht / op reps" changes the exercise from the
     * next session on (#52), so today's sets keep the progression the session started with.
     */
    val progressionToday: Progression = exercise.progression,
    /**
     * Reps progression: per set the reps you aim at today, the number in its header: your reps target as this
     * training started (#69), or for a set without one the reps of its role last time (#52). Null on weight.
     */
    val repsTargets: List<Int?>? = null,
    /** "Laatste keer" (#65): per set in [sets] its most recent performance with the date; null for warm-ups. */
    val lastPerSet: List<LastSet?> = emptyList(),
    /**
     * Bodyweight exercise on weight (#87): per set in [sets] the target as an effective weight, next to what you hang
     * today (its session target); null for warm-ups, other exercises or without a known body weight.
     */
    val targetEffectiveKg: List<Double?> = emptyList(),
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
    /** Advice you took over on this screen (#63): per session exercise, per set of the target. */
    private val taken = MutableStateFlow<Map<Long, Map<Int, TakenAdvice>>>(emptyMap())
    val takenAdvice: StateFlow<Map<Long, Map<Int, TakenAdvice>>> = taken

    val state: StateFlow<TrainingState> = combine(
        c.db.sessions().observe(sessionId),
        c.db.sessions().observeExercises(sessionId),
        c.db.sessions().observeSets(sessionId),
        combine(c.db.exercises().observeAll(), c.settings.settings, c.db.materials().observeAll(), c.db.gyms().observeAll()) { e, s, m, g ->
            Triple(e, s, m to g)
        },
        combine(extras, combine(drafts, ownTargets) { d, o -> o + d }, notes) { e, d, n -> Triple(e, d, n) },
    ) { session, ses, sets, (exercises, settings, materialsAndGyms), (extraMap, draftMap, noteMap) ->
        val (materials, gyms) = materialsAndGyms
        val byId = exercises.associateBy { it.id }
        val profiles = materials.associateBy { it.id }
        // The plates of this training's gym (A-4); the settings until they moved into the gym.
        val stock = plateStockFor(session?.gymId, gyms, settings.plateStock)
        val setsBySe = sets.groupBy { it.sessionExerciseId }
        val items = ses.mapNotNull { se ->
            val ex = byId[se.exerciseId] ?: return@mapNotNull null
            val list = setsBySe[se.id].orEmpty()
            val calc = Calculators.forExercise(ex, profiles, stock, se.materialProfileId)
            val plan = followed(draftMap[se.id], defaultTarget(ex, list, extraMap[ex.id]))?.let { p -> noteMap[se.id]?.let { p.copy(note = it) } ?: p }
            val assist = if (Warmup.needsAssistance(ex.loadType, ex.bodyweightFactor)) {
                Warmup.assistKg(session?.bodyweightKg, ex.bodyweightFactor, settings.warmupAssistPercent, calc)
            } else null
            val today = startProgression.getOrPut(ex.id) { ex.progression }
            val repsTargets = if (today == Progression.REPS) {
                val lastTime = RepsTarget.forSets(list.map { it.role }, extraMap[ex.id]?.last?.sets?.map { PerformedSet(it.role, it.weightKg, it.reps) })
                list.mapIndexed { i, set -> set.goalReps ?: lastTime[i] }
            } else null
            val lastPerSet = extraMap[ex.id]?.let { e -> LastTime.perSet(list.map { it.role }, e.history) }.orEmpty()
            val effectiveTargets = if (ex.loadType == LoadType.BODYWEIGHT && today == Progression.WEIGHT) {
                targetEffective(list, extraMap[ex.id]?.stored, session?.bodyweightKg?.let { BodyShare(it, ex.bodyweightFactor) })
            } else emptyList()
            TrainingItem(se, ex, list, extraMap[ex.id], plan, noteMap[se.id] ?: plan?.note ?: "", calc, assist, today, repsTargets, lastPerSet, effectiveTargets)
        }
        items.forEach {
            if (it.extra == null) loadExtra(it.exercise.id)
            loadOwnTarget(it.se.id, it.exercise.id)
        }
        TrainingState(session, items, settings, exercises.filter { !it.archived }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingState())

    private val loading = HashSet<Long>()
    /** Per exercise the progression as this session started (#52). */
    private val startProgression = HashMap<Long, Progression>()
    private val loadingTargets = HashSet<Long>()

    private fun loadOwnTarget(seId: Long, exerciseId: Long) {
        if (!loadingTargets.add(seId)) return
        viewModelScope.launch {
            val own = repo.targetSetIn(seId, exerciseId)
            if (own == null) {
                // A note you wrote without setting a set yourself (#92): the sets keep following your performance.
                val kept = repo.targetOf(seId)?.takeIf { !it.note.isNullOrBlank() } ?: return@launch
                ownTargets.update { it + (seId to PlanDraft(kept.sets, kept.note.orEmpty())) }
                return@launch
            }
            // Per set whether you set it yourself (#92); the other sets keep following your performance.
            val yours = repo.yoursIn(seId).takeIf { it.size == own.sets.size } ?: own.sets.map { true }
            ownTargets.update { it + (seId to PlanDraft(own.sets, own.note.orEmpty(), edited = true, yours = yours)) }
        }
    }

    private fun loadExtra(exerciseId: Long) {
        if (!loading.add(exerciseId)) return
        viewModelScope.launch {
            val last = repo.lastPerformance(exerciseId, sessionId)
            val similar = if (last == null) repo.similarPerformances(exerciseId, sessionId) else emptyList()
            val extra = ExerciseExtra(
                last, repo.planNote(exerciseId), similar,
                // As the signed load of this training's day (#87), so the default target compares on one body weight.
                stored = repo.target(exerciseId, sessionId)?.sets,
                workout = repo.workoutRoles(sessionId, exerciseId),
                history = repo.lastTimeHistory(exerciseId, sessionId),
            )
            extras.update { it + (exerciseId to extra) }
        }
    }

    /**
     * Per set the target this training started from as an effective weight (#87): the k-th heavy set takes the k-th
     * heavy set of [stored] (the signed load of today, exact). Only for sets that aim at a weight today.
     */
    private fun targetEffective(sets: List<SetEntity>, stored: List<PlannedSet>?, share: BodyShare?): List<Double?> {
        if (stored == null || share == null) return emptyList()
        return sets.mapIndexed { i, set ->
            if (!set.role.isWork || set.goalWeightKg == null) return@mapIndexed null
            val k = sets.take(i).count { it.role == set.role }
            stored.filter { it.role == set.role }.getOrNull(k)?.weightKg?.let(share::effective)
        }
    }

    /**
     * Your target for next time (#92): the sets you set yourself in [draft] as you set them, the others following the
     * [default] target. Without a default target yet, the draft as it is.
     */
    private fun followed(draft: PlanDraft?, default: PlanDraft?): PlanDraft? {
        if (draft == null || default == null) return draft ?: default
        val own = SetByYou.follow(draft.own(), default.sets)
        return PlanDraft(own.sets, draft.note, edited = own.anyYours, yours = own.yours)
    }

    /** The default target (#52): per set the weight you lifted today; never the advice. Null until reps are in. */
    private fun defaultTarget(ex: ExerciseEntity, sets: List<SetEntity>, extra: ExerciseExtra?): PlanDraft? {
        // Warm-up reps (prefilled from the default warm-up, #93) are no performance.
        if (extra == null || sets.none { it.role.isWork && it.reps != null }) return null
        val today = sets.map { PerformedSet(it.role, it.weightKg, it.reps) }
        return PlanDraft(DefaultTarget.of(extra.stored, extra.workout, today, ex.progression, ex.loadType))
    }

    // ---- timestamps ----
    fun shown(seId: Long) = launch { repo.markShown(seId) }
    fun done(seId: Long) = launch { repo.markDone(seId); persistPlan(seId) }
    fun go(seId: Long) = launch { repo.markGo(seId) }
    fun firstInput(seId: Long) = launch { repo.markFirstInput(seId) }

    /** Starts the warm-up: timer mark plus a warm-up set filled in with your default warm-up (#93). */
    fun startWarmup(seId: Long) = launch {
        repo.markWarmup(seId)
        repo.startWarmupSet(seId)
    }

    /** "Bewaar … als standaard" (#93): the value of your warm-up set becomes the exercise's default warm-up. */
    fun saveDefaultWarmup(exerciseId: Long, value: Double) = launch { repo.saveDefaultWarmupValue(exerciseId, value) }

    // ---- sets ----
    fun updateSet(set: SetEntity) = launch { repo.updateSet(set) }
    /** ＋ Set: a warm-up starts with your default (#93); the session target follows the role (#92). */
    fun addSet(seId: Long, role: SetRole) = launch { repo.addSet(seId, role, withDefaultWarmup = true, followRole = true) }
    fun deleteSet(id: Long) = launch { repo.deleteSet(id) }
    /** Heavy ↔ light: the session target follows the role (#92). */
    fun toggleRole(set: SetEntity) = launch {
        val next = if (set.role == SetRole.HEAVY) SetRole.LIGHT else SetRole.HEAVY
        repo.switchRole(set, next)
    }

    // ---- exercise order ----
    fun skip(seId: Long, skipped: Boolean) = launch { repo.setSkipped(seId, skipped) }
    fun moveToEnd(seId: Long) = launch { repo.moveToEnd(sessionId, seId) }
    fun addExercise(exerciseId: Long) = launch { repo.addExercise(sessionId, exerciseId) }

    // ---- plan for next time (D-18a) ----
    /**
     * You change the sets of your target (add, remove, switch a role or the progression): shown at once and stored
     * right away (B-6). The target is then yours as a whole.
     */
    fun editPlan(seId: Long, transform: (PlanDraft) -> PlanDraft) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val current = item.plan ?: return
        val next = transform(current).let { d -> d.copy(note = item.note, edited = true, yours = d.sets.map { true }) }
        drafts.update { it + (seId to next) }
        launch { repo.setTarget(seId, item.exercise.id, Target(next.sets, next.note)) }
    }

    /**
     * You change set [index] of your target (its weight or reps, or by taking over its advice): only that set is yours
     * from now on (#92); the other sets keep following your performance. Stored right away.
     */
    fun editPlanSet(seId: Long, index: Int, change: (PlannedSet) -> PlannedSet) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val current = item.plan ?: return
        val set = current.sets.getOrNull(index) ?: return
        store(item, SetByYou.change(current.own(), index, change(set)))
    }

    /** Shows [own] as your target at once and stores it right away, with per set whether you set it yourself. */
    private fun store(item: TrainingItem, own: OwnSets) {
        val next = PlanDraft(own.sets, item.note, edited = own.anyYours, yours = own.yours)
        drafts.update { it + (item.se.id to next) }
        launch { repo.setTarget(item.se.id, item.exercise.id, Target(next.sets, next.note), next.yours) }
    }

    /** Overnemen (#63): the advice becomes your target for set [index], set by you (#92) and stored right away. */
    fun takeAdvice(seId: Long, index: Int, advice: Double) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val plan = item.plan ?: return
        val previous = plan.sets.getOrNull(index) ?: return
        val wasYours = plan.own().yours[index]
        taken.update { it + (seId to (it[seId].orEmpty() + (index to TakenAdvice(advice, previous.weightKg, wasYours)))) }
        editPlanSet(seId, index) { it.copy(weightKg = advice) }
    }

    /**
     * Ongedaan (#63, #92): only set [index] goes back. Had you set it yourself before, it gets back that weight; else it
     * follows your performance again, like the sets you did not set yourself.
     */
    fun undoAdvice(seId: Long, index: Int) {
        val undone = taken.value[seId]?.get(index) ?: return
        taken.update { it + (seId to (it[seId].orEmpty() - index)) }
        if (undone.wasYours) {
            editPlanSet(seId, index) { it.copy(weightKg = undone.previousKg) }
            return
        }
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val plan = item.plan ?: return
        val default = defaultTarget(item.exercise, item.sets, item.extra) ?: return
        store(item, SetByYou.release(plan.own(), index, default.sets))
    }

    /** A note is part of your target, so it is stored right away as well; it does not make a set yours (#92). */
    fun setPlanNote(seId: Long, note: String) {
        notes.update { it + (seId to note) }
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val plan = item.plan ?: return
        drafts.update { it + (seId to plan.copy(note = note)) }
        launch { repo.setTarget(seId, item.exercise.id, Target(plan.sets, note), plan.own().yours) }
    }

    /**
     * "Volgende keer op gewicht / op reps" (#52): the exercise switches from the next session on, and your
     * target follows right away (to weight on a bodyweight exercise: 0, body weight only).
     */
    fun switchProgression(seId: Long) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val ex = item.exercise
        val to = ProgressionSwitch.offered(ex.progression, ex.loadType) ?: return
        val today = item.sets.map { PerformedSet(it.role, it.weightKg, it.reps) }
        editPlan(seId) { d -> d.copy(sets = ProgressionSwitch.target(d.sets, today, to, ex.loadType)) }
        launch { repo.setProgression(ex.id, to) }
    }

    /**
     * Moving on or finishing: the default target is stored for the sets you did not set yourself (#92); a target you
     * set as a whole stays as it is.
     */
    private suspend fun persistPlan(seId: Long) {
        val item = state.value.items.firstOrNull { it.se.id == seId } ?: return
        val plan = item.plan ?: return
        if (item.se.skipped || plan.sets.isEmpty() || plan.own().allYours) return
        repo.storeDefaultTarget(seId, item.exercise.id, Target(plan.sets, plan.note))
    }

    /** The check before finishing (#47): what is missing in your performance and your target. */
    fun finishCheck(): List<Finding> = FinishCheck.check(
        state.value.items.map { item ->
            FinishExercise(
                sessionExerciseId = item.se.id,
                exerciseId = item.exercise.id,
                progression = item.progressionToday,
                loadType = item.exercise.loadType,
                sets = item.sets.map { FinishSet(it.role, it.weightKg, it.reps) },
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
