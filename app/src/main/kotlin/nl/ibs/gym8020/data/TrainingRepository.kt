package nl.ibs.gym8020.data

import androidx.room.withTransaction
import nl.ibs.gym8020.core.logic.BodyShare
import nl.ibs.gym8020.core.logic.BodyweightTarget
import nl.ibs.gym8020.core.logic.DefaultTarget
import nl.ibs.gym8020.core.logic.Measurement
import nl.ibs.gym8020.core.logic.PastSession
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.TargetOrigin
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.PlanEntity
import nl.ibs.gym8020.data.db.PlanSetEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.data.db.TargetEntity
import nl.ibs.gym8020.data.db.TargetSetEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Default roles for a fresh exercise: heavy, light, and extra sets are heavy (D-10). */
fun defaultRoles(count: Int): List<SetRole> = List(count.coerceAtLeast(1)) { i -> if (i == 1) SetRole.LIGHT else SetRole.HEAVY }

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * Your target for next time for one exercise: per set the role and weight (or reps, #69), plus a note. Read and set
 * through a training, the weight of a bodyweight exercise is the signed load on the day of that training (exact,
 * not rounded); it is stored as an effective weight (#87, ADR 0002).
 */
data class Target(val sets: List<PlannedSet>, val note: String? = null)

/** What was done last time for an exercise (reference and "vorige keer"). */
data class LastPerformance(val date: LocalDate, val sets: List<SetEntity>)

class TrainingRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    /** Called after a training is finished (automatic backup). */
    private val onFinished: () -> Unit = {},
) {

    private val sessions get() = db.sessions()

    suspend fun startSession(schemaId: Long, now: Long = System.currentTimeMillis()): Long {
        // A body weight entered just before starting makes converting the bodyweight targets possible (#87).
        BodyweightTargets(db, settings).convertIfNeeded()
        return startSessionInternal(schemaId, now)
    }

    private suspend fun startSessionInternal(schemaId: Long, now: Long): Long = db.withTransaction {
        val schema = requireNotNull(db.schemas().get(schemaId))
        val method = settings.current().method
        val bodyweight = db.bodyweights().latestOnOrBefore(now.toLocalDate().toEpochDay())?.kg
        val gymId = defaultGym(db.gyms().all())?.id
        val sessionId = sessions.insert(
            SessionEntity(schemaId = schemaId, schemaName = schema.name, startedAt = now, bodyweightKg = bodyweight, gymId = gymId),
        )
        db.schemas().entries(schemaId).forEachIndexed { i, entry ->
            val exercise = db.exercises().get(entry.exerciseId) ?: return@forEachIndexed
            if (exercise.archived) return@forEachIndexed
            addExerciseInternal(sessionId, exercise, i, entry.setCount, method)
        }
        sessionId
    }

    /**
     * Adds an exercise during the training (D-35), at the end. When correcting a finished training
     * ([withGoals] = false) the sets get no goal, so "goals reached" is not affected afterwards.
     */
    suspend fun addExercise(sessionId: Long, exerciseId: Long, withGoals: Boolean = true) = db.withTransaction {
        val exercise = requireNotNull(db.exercises().get(exerciseId))
        requireNotNull(sessions.get(sessionId))
        val position = (sessions.exercises(sessionId).maxOfOrNull { it.position } ?: -1) + 1
        addExerciseInternal(sessionId, exercise, position, null, settings.current().method, withGoals)
    }

    /** Removes an exercise (with its sets) from a training. */
    suspend fun removeExercise(seId: Long) = sessions.deleteExercise(seId)

    /** Corrects start and end of a finished training. */
    suspend fun updateTimes(sessionId: Long, startedAt: Long, endedAt: Long) {
        val session = sessions.get(sessionId) ?: return
        sessions.update(session.copy(startedAt = startedAt, endedAt = endedAt.coerceAtLeast(startedAt)))
    }

    private suspend fun addExerciseInternal(
        sessionId: Long,
        exercise: ExerciseEntity,
        position: Int,
        setCount: Int?,
        method: MethodSettings,
        withGoals: Boolean = true,
    ) {
        val seId = sessions.insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exercise.id, position = position))
        val bodyweight = sessions.get(sessionId)?.bodyweightKg
        val plan = db.plans().sets(exercise.id)
        val planned: List<PlannedSet> = if (plan.isNotEmpty()) {
            // A bodyweight exercise aims at an effective weight: today that is this signed load, rounded to the material (#87).
            val kept = plan.map { p -> PlannedSet(p.role, p.weightKg, p.reps) }
            val share = shareIn(exercise, sessionId)
            if (share == null) kept else BodyweightTarget.forToday(kept, share, snapFor(exercise, sessionId, method))
        } else {
            val zero = if (exercise.loadType == LoadType.BODYWEIGHT) 0.0 else null
            defaultRoles(setCount ?: method.defaultSetCount).map { PlannedSet(it, zero) }
        }
        val isReps = exercise.progression == Progression.REPS
        // Your performance is never prefilled (#47): the target only goes into the session target, goalWeightKg,
        // or with reps progression goalReps (#69).
        sessions.insertSets(
            planned.mapIndexed { i, p ->
                SetEntity(
                    sessionExerciseId = seId,
                    position = i,
                    role = p.role,
                    bodyweightKg = if (exercise.loadType == LoadType.BODYWEIGHT) bodyweight else null,
                    goalWeightKg = if (withGoals && p.role.isWork && !isReps) p.weightKg else null,
                    goalReps = if (withGoals && p.role.isWork && isReps) p.reps else null,
                )
            },
        )
    }

    // ---- timestamps (D-30, D-31, D-45a) ----

    suspend fun markShown(seId: Long, now: Long = System.currentTimeMillis()) = updateSe(seId) { if (it.shownAt == null) it.copy(shownAt = now) else it }

    suspend fun markWarmup(seId: Long, now: Long = System.currentTimeMillis()) = updateSe(seId) { if (it.warmupAt == null && it.goAt == null) it.copy(warmupAt = now) else it }

    suspend fun markGo(seId: Long, now: Long = System.currentTimeMillis()) = updateSe(seId) { if (it.goAt == null) it.copy(goAt = now) else it }

    suspend fun markFirstInput(seId: Long, now: Long = System.currentTimeMillis()) = updateSe(seId) { if (it.firstInputAt == null) it.copy(firstInputAt = now) else it }

    /** Called when swiping away; only the first time counts, so correcting later does not spoil the times. */
    suspend fun markDone(seId: Long, now: Long = System.currentTimeMillis()) = updateSe(seId) {
        if (it.doneAt == null && it.firstInputAt != null) it.copy(doneAt = now) else it
    }

    suspend fun setSkipped(seId: Long, skipped: Boolean) = updateSe(seId) { it.copy(skipped = skipped) }

    /** Moves an exercise to the end of the training (D-35). */
    suspend fun moveToEnd(sessionId: Long, seId: Long) = db.withTransaction {
        val list = sessions.exercises(sessionId)
        val moved = list.filter { it.id != seId } + list.filter { it.id == seId }
        sessions.updateExercises(moved.mapIndexed { i, e -> e.copy(position = i) })
    }

    private suspend fun updateSe(seId: Long, f: (SessionExerciseEntity) -> SessionExerciseEntity) {
        val se = sessions.exercise(seId) ?: return
        val next = f(se)
        if (next != se) sessions.updateExercise(next)
    }

    // ---- sets ----

    suspend fun updateSet(set: SetEntity) = sessions.updateSet(set)

    suspend fun addSet(seId: Long, role: SetRole) = db.withTransaction {
        val sets = sessions.setsOf(seId)
        val se = requireNotNull(sessions.exercise(seId))
        val session = requireNotNull(sessions.get(se.sessionId))
        val exercise = requireNotNull(db.exercises().get(se.exerciseId))
        val bw = if (exercise.loadType == LoadType.BODYWEIGHT) session.bodyweightKg else null
        if (role == SetRole.WARMUP) {
            // Warm-ups go before the work sets.
            sessions.updateSets(sets.map { it.copy(position = it.position + 1) })
            sessions.insertSet(SetEntity(sessionExerciseId = seId, position = 0, role = SetRole.WARMUP, bodyweightKg = bw))
        } else {
            // Weight stays empty: you fill in what you did (#47).
            sessions.insertSet(
                SetEntity(sessionExerciseId = seId, position = (sets.maxOfOrNull { it.position } ?: -1) + 1, role = role, bodyweightKg = bw),
            )
        }
    }

    suspend fun deleteSet(setId: Long) = sessions.deleteSet(setId)

    // ---- bodyweight exercises: the target in effective weight (#87, ADR 0002) ----

    /**
     * The share of body weight a bodyweight exercise moves in training [sessionId], to convert its target between the
     * signed load of that day and the stored effective weight. Null (no conversion) for other exercises, while the
     * targets are not converted yet (`BodyweightTargets`), or without a known body weight.
     */
    private suspend fun shareIn(exercise: ExerciseEntity, sessionId: Long): BodyShare? {
        if (exercise.loadType != LoadType.BODYWEIGHT || !settings.current().bodyweightTargetsEffective) return null
        val session = sessions.get(sessionId) ?: return null
        val measurements = db.bodyweights().all().map { Measurement(it.epochDay, it.measuredAt, it.kg) }
        val kg = BodyweightTarget.bodyweightFor(session.bodyweightKg, measurements, session.startedAt.toLocalDate().toEpochDay())
            // As in `BodyweightTargets`: as a last resort the most recent body weight a training kept.
            ?: sessions.all().filter { it.bodyweightKg != null }.maxByOrNull { it.startedAt }?.bodyweightKg
            ?: return null
        return BodyShare(kg, exercise.bodyweightFactor)
    }

    /** [shareIn] for the training of session exercise [seId]. */
    private suspend fun shareOf(seId: Long): BodyShare? {
        val se = sessions.exercise(seId) ?: return null
        val exercise = db.exercises().get(se.exerciseId) ?: return null
        return shareIn(exercise, se.sessionId)
    }

    /** Rounding to what the material allows: the exercise's profile, else the weight step from the settings. */
    private suspend fun snapFor(exercise: ExerciseEntity, sessionId: Long, method: MethodSettings): (Double) -> Double {
        val stock = plateStockFor(sessions.get(sessionId)?.gymId, db.gyms().all(), settings.current().plateStock)
        val calc = Calculators.forExercise(exercise, db.materials().all().associateBy { it.id }, stock)
        val step = method.weightStepKg
        return calc?.let { c -> { kg: Double -> c.snap(kg) } } ?: { kg: Double -> BodyweightTarget.roundToStep(kg, step) }
    }

    // ---- plan for next time ----

    /**
     * Stores the current target of an exercise as it is kept (a bodyweight exercise in effective weight): what the
     * next session starts from.
     */
    suspend fun savePlan(exerciseId: Long, sets: List<PlannedSet>, note: String?) {
        val target = stored(Target(sets, note))
        db.plans().replace(
            PlanEntity(exerciseId = exerciseId, note = target.note, updatedAt = System.currentTimeMillis()),
            target.sets.map { PlanSetEntity(exerciseId = exerciseId, position = 0, role = it.role, weightKg = it.weightKg, reps = it.reps) },
        )
    }

    /** A target as it is stored: weight and reps only on work sets, no blank note. */
    private fun stored(target: Target) = Target(
        target.sets.map { if (it.role.isWork) it else PlannedSet(it.role, null, null) },
        target.note?.takeIf { it.isNotBlank() },
    )

    /**
     * Stores [target] as the current target and keeps it as the target set in session exercise [seId] (schema v3),
     * with who set it.
     */
    private suspend fun storeTarget(seId: Long, exerciseId: Long, target: Target, origin: TargetOrigin) = db.withTransaction {
        val kept = stored(Target(BodyweightTarget.toEffective(target.sets, shareOf(seId)), target.note))
        savePlan(exerciseId, kept.sets, kept.note)
        db.targets().replace(
            TargetEntity(sessionExerciseId = seId, origin = origin, note = kept.note, setAt = System.currentTimeMillis()),
            kept.sets.map { TargetSetEntity(sessionExerciseId = seId, position = 0, role = it.role, weightKg = it.weightKg, reps = it.reps) },
        )
    }

    /** Whether you set the target yourself in session exercise [seId]. */
    private suspend fun isSetByYou(seId: Long): Boolean = db.targets().get(seId)?.origin == TargetOrigin.YOURS

    /**
     * The target kept for session exercise [seId] (schema v3), whoever set it; null when it has none. A bodyweight
     * exercise's weight as the signed load on the day of that training.
     */
    suspend fun targetOf(seId: Long): Target? {
        val target = db.targets().get(seId) ?: return null
        val sets = db.targets().sets(seId).map { PlannedSet(it.role, it.weightKg, it.reps) }
        return Target(BodyweightTarget.toSigned(sets, shareOf(seId)), target.note)
    }

    suspend fun planNote(exerciseId: Long): String? = db.plans().get(exerciseId)?.note

    // ---- target for next time (B-6, D-97) ----

    /**
     * The stored target for an exercise: what the next session starts from. With [sessionId], a bodyweight
     * exercise's weight is the signed load on the day of that training (exact, #87); without, as kept.
     */
    suspend fun target(exerciseId: Long, sessionId: Long? = null): Target? {
        val plan = db.plans().get(exerciseId) ?: return null
        val sets = db.plans().sets(exerciseId).map { s -> PlannedSet(s.role, s.weightKg, s.reps) }
        val share = sessionId?.let { id -> db.exercises().get(exerciseId)?.let { shareIn(it, id) } }
        return Target(BodyweightTarget.toSigned(sets, share), plan.note)
    }

    /** You set your target during session exercise [seId]: stored right away, and kept as yours in that session. */
    suspend fun setTarget(seId: Long, exerciseId: Long, target: Target) {
        storeTarget(seId, exerciseId, target, TargetOrigin.YOURS)
    }

    /**
     * Undoes a target you set during [seId] (Ongedaan, #63): [before] is stored again and the target no longer
     * counts as yours, so the default target applies when moving on.
     */
    suspend fun withdrawTarget(seId: Long, exerciseId: Long, before: Target) {
        db.withTransaction {
            savePlan(exerciseId, BodyweightTarget.toEffective(before.sets, shareOf(seId)), before.note)
            db.targets().delete(seId)
        }
    }

    /**
     * The target you set yourself during session exercise [seId], or null if you did not. Read from what that
     * session kept (schema v3), so a later session's target does not change it; [exerciseId] is no longer needed.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun targetSetIn(seId: Long, exerciseId: Long): Target? =
        if (isSetByYou(seId)) targetOf(seId) else null

    /**
     * Stores the default target (#52) when moving on, unless you set one yourself during [seId]. Never the
     * advice: the advice does not become a target by itself.
     */
    suspend fun storeDefaultTarget(seId: Long, exerciseId: Long, target: Target) {
        if (isSetByYou(seId)) return
        storeTarget(seId, exerciseId, target, TargetOrigin.DEFAULT)
    }

    /** The set roles the workout of [sessionId] gives this exercise; the default target's sets without a target. */
    suspend fun workoutRoles(sessionId: Long, exerciseId: Long): List<SetRole> {
        val schemaId = sessions.get(sessionId)?.schemaId
        val entry = schemaId?.let { id -> db.schemas().entries(id).firstOrNull { it.exerciseId == exerciseId } }
        return defaultRoles(entry?.setCount ?: settings.current().method.defaultSetCount)
    }

    /** "Volgende keer op gewicht / op reps" (#52): the exercise's progression, from the next session on. */
    suspend fun setProgression(exerciseId: Long, progression: Progression) {
        val exercise = db.exercises().get(exerciseId) ?: return
        if (exercise.progression != progression) db.exercises().update(exercise.copy(progression = progression))
    }

    /** Whether [sessionId] is the most recent finished training with this exercise (then it drives the plan). */
    suspend fun isLatestFor(exerciseId: Long, sessionId: Long): Boolean =
        sessions.historyExercises(exerciseId, excludeSession = -1).firstOrNull()?.sessionId == sessionId

    /**
     * The default target again from the (corrected) sets of a training (#52), keeping the note. A target you
     * set yourself in that training stays.
     */
    suspend fun recomputeTarget(exercise: ExerciseEntity, sessionId: Long) {
        val se = sessions.exercises(sessionId).firstOrNull { it.exerciseId == exercise.id && !it.skipped } ?: return
        if (isSetByYou(se.id)) return
        val target = defaultTargetFrom(exercise, sessionId, se.id) ?: return
        storeTarget(se.id, exercise.id, Target(target, planNote(exercise.id)), TargetOrigin.DEFAULT)
    }

    /** The default target from the sets of session exercise [seId]; null while it has no reps. Not stored. */
    private suspend fun defaultTargetFrom(exercise: ExerciseEntity, sessionId: Long, seId: Long): List<PlannedSet>? {
        val sets = sessions.setsOf(seId)
        if (sets.none { it.reps != null }) return null
        val stored = target(exercise.id, sessionId)?.sets
        return DefaultTarget.of(
            stored,
            if (stored.isNullOrEmpty()) workoutRoles(sessionId, exercise.id) else emptyList(),
            sets.map { PerformedSet(it.role, it.weightKg, it.reps) },
            exercise.progression,
            exercise.loadType,
        )
    }

    /**
     * Saving a corrected training (#64): for each changed exercise for which [sessionId] is the latest training
     * (counted now, also after moving its date), the default target follows the corrected performance. A target
     * you set yourself is never changed by a correction.
     */
    suspend fun applyCorrection(sessionId: Long, exerciseIds: Collection<Long>) {
        exerciseIds.distinct().forEach { id ->
            val exercise = db.exercises().get(id) ?: return@forEach
            if (isLatestFor(id, sessionId)) recomputeTarget(exercise, sessionId)
        }
    }

    /**
     * "Doel volgende keer" in the edit screen (#64): null when [sessionId] is not the latest training with this
     * exercise. Otherwise the target you set yourself in it, or else the default target the (corrected)
     * performance gives, or else the stored target.
     */
    suspend fun nextTargetIn(sessionId: Long, exercise: ExerciseEntity): Target? {
        if (!isLatestFor(exercise.id, sessionId)) return null
        val se = sessions.exercises(sessionId).firstOrNull { it.exerciseId == exercise.id && !it.skipped } ?: return null
        targetSetIn(se.id, exercise.id)?.let { return it }
        val note = planNote(exercise.id)
        defaultTargetFrom(exercise, sessionId, se.id)?.let { return Target(it, note) }
        return target(exercise.id, sessionId)
    }

    // ---- history ----

    suspend fun lastPerformance(exerciseId: Long, excludeSession: Long): LastPerformance? {
        val se = sessions.historyExercises(exerciseId, excludeSession).firstOrNull() ?: return null
        val session = sessions.get(se.sessionId) ?: return null
        return LastPerformance(session.startedAt.toLocalDate(), sessions.setsOf(se.id))
    }

    /**
     * The earlier trainings of an exercise for "Laatste keer" per set (#65): from any workout, with the training
     * date (also for a training entered afterwards), skipped ones marked. LastTime picks per set from these.
     */
    suspend fun lastTimeHistory(exerciseId: Long, excludeSession: Long): List<PastSession> {
        val sets = sessions.historySets(exerciseId, excludeSession).groupBy { it.sessionExerciseId }
        return sessions.historyDates(exerciseId, excludeSession).map { row ->
            val done = sets[row.sessionExerciseId].orEmpty()
            PastSession(
                date = row.startedAt.toLocalDate(),
                sets = done.map { PerformedSet(it.role, it.weightKg, it.reps) },
                skipped = row.skipped,
                // The body weight of that training, kept with its bodyweight sets: "Laatste keer" in effective weight (#87).
                bodyweightKg = done.firstNotNullOfOrNull { it.bodyweightKg },
            )
        }
    }

    /**
     * Similar exercises (D-68): when an exercise has no history, the latest performance of other exercises
     * that train the same primary muscles, as a reference to find a starting weight. No conversion.
     */
    suspend fun similarPerformances(exerciseId: Long, excludeSession: Long, limit: Int = 2): List<Pair<ExerciseEntity, LastPerformance>> {
        val all = db.exercises().allMuscles()
        val primary = all.filter { it.exerciseId == exerciseId && it.percent >= 100 }.map { it.muscle }.toSet()
        if (primary.isEmpty()) return emptyList()
        val candidates = all.filter { it.exerciseId != exerciseId && it.percent >= 100 && it.muscle in primary }
            .groupBy { it.exerciseId }.entries.sortedByDescending { it.value.size }.map { it.key }
        val out = mutableListOf<Pair<ExerciseEntity, LastPerformance>>()
        for (id in candidates) {
            val ex = db.exercises().get(id) ?: continue
            val last = lastPerformance(id, excludeSession) ?: continue
            out += ex to last
            if (out.size >= limit) break
        }
        return out.sortedByDescending { it.second.date }
    }

    suspend fun finish(sessionId: Long, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val session = sessions.get(sessionId) ?: return@withTransaction
        sessions.exercises(sessionId).forEach { se -> if (se.doneAt == null && se.firstInputAt != null) sessions.updateExercise(se.copy(doneAt = now)) }
        sessions.update(session.copy(endedAt = now))
    }.also { onFinished() }

    suspend fun discard(sessionId: Long) = sessions.delete(sessionId)
}
