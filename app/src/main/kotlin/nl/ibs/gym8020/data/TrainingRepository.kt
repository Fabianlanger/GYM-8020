package nl.ibs.gym8020.data

import androidx.room.withTransaction
import nl.ibs.gym8020.core.logic.EffectiveWeight
import nl.ibs.gym8020.core.logic.GoalAdvisor
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.Strength
import nl.ibs.gym8020.core.logic.WeightSteps
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.PlanEntity
import nl.ibs.gym8020.data.db.PlanSetEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Default roles for a fresh exercise: heavy, light, and extra sets are heavy (D-10). */
fun defaultRoles(count: Int): List<SetRole> = List(count.coerceAtLeast(1)) { i -> if (i == 1) SetRole.LIGHT else SetRole.HEAVY }

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** Your target for next time for one exercise: per set the role and weight, plus a note. */
data class Target(val sets: List<PlannedSet>, val note: String? = null)

/** What was done last time for an exercise (reference and "vorige keer"). */
data class LastPerformance(val date: LocalDate, val sets: List<SetEntity>)

class TrainingRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val marks: TargetMarks,
    /** Called after a training is finished (automatic backup). */
    private val onFinished: () -> Unit = {},
) {

    private val sessions get() = db.sessions()

    suspend fun startSession(schemaId: Long, now: Long = System.currentTimeMillis()): Long = db.withTransaction {
        val schema = requireNotNull(db.schemas().get(schemaId))
        val method = settings.current().method
        val bodyweight = db.bodyweights().latestOnOrBefore(now.toLocalDate().toEpochDay())?.kg
        val sessionId = sessions.insert(SessionEntity(schemaId = schemaId, schemaName = schema.name, startedAt = now, bodyweightKg = bodyweight))
        db.schemas().entries(schemaId).forEachIndexed { i, entry ->
            val exercise = db.exercises().get(entry.exerciseId) ?: return@forEachIndexed
            if (exercise.archived) return@forEachIndexed
            addExerciseInternal(sessionId, exercise, i, entry.setCount, method, bodyweight)
        }
        sessionId
    }

    /**
     * Adds an exercise during the training (D-35), at the end. When correcting a finished training
     * ([withGoals] = false) the sets get no goal, so "goals reached" is not affected afterwards.
     */
    suspend fun addExercise(sessionId: Long, exerciseId: Long, withGoals: Boolean = true) = db.withTransaction {
        val exercise = requireNotNull(db.exercises().get(exerciseId))
        val session = requireNotNull(sessions.get(sessionId))
        val position = (sessions.exercises(sessionId).maxOfOrNull { it.position } ?: -1) + 1
        addExerciseInternal(sessionId, exercise, position, null, settings.current().method, session.bodyweightKg, withGoals)
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
        bodyweight: Double?,
        withGoals: Boolean = true,
    ) {
        val seId = sessions.insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exercise.id, position = position))
        val plan = db.plans().sets(exercise.id)
        val planned: List<PlannedSet> = if (plan.isNotEmpty()) {
            plan.map { PlannedSet(it.role, it.weightKg) }
        } else {
            val zero = if (exercise.loadType == LoadType.BODYWEIGHT) 0.0 else null
            defaultRoles(setCount ?: method.defaultSetCount).map { PlannedSet(it, zero) }
        }
        val isReps = exercise.progression == Progression.REPS
        sessions.insertSets(
            planned.mapIndexed { i, p ->
                val weight = if (p.role == SetRole.WARMUP) null else p.weightKg ?: if (isReps) 0.0 else null
                SetEntity(
                    sessionExerciseId = seId,
                    position = i,
                    role = p.role,
                    weightKg = weight,
                    bodyweightKg = if (exercise.loadType == LoadType.BODYWEIGHT) bodyweight else null,
                    goalWeightKg = if (withGoals && p.role.isWork && !isReps) p.weightKg else null,
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
            val lastWork = sets.lastOrNull { it.role.isWork }
            val weight = lastWork?.weightKg ?: if (exercise.loadType == LoadType.BODYWEIGHT) 0.0 else null
            sessions.insertSet(
                SetEntity(sessionExerciseId = seId, position = (sets.maxOfOrNull { it.position } ?: -1) + 1, role = role, weightKg = weight, bodyweightKg = bw),
            )
        }
    }

    suspend fun deleteSet(setId: Long) = sessions.deleteSet(setId)

    // ---- plan for next time ----

    suspend fun savePlan(exerciseId: Long, sets: List<PlannedSet>, note: String?) {
        db.plans().replace(
            PlanEntity(exerciseId = exerciseId, note = note?.takeIf { it.isNotBlank() }, updatedAt = System.currentTimeMillis()),
            sets.map { PlanSetEntity(exerciseId = exerciseId, position = 0, role = it.role, weightKg = if (it.role.isWork) it.weightKg else null) },
        )
    }

    suspend fun planNote(exerciseId: Long): String? = db.plans().get(exerciseId)?.note

    // ---- target for next time (B-6, D-97) ----

    /** The stored target for an exercise: what the next session starts from. */
    suspend fun target(exerciseId: Long): Target? {
        val plan = db.plans().get(exerciseId) ?: return null
        return Target(db.plans().sets(exerciseId).map { PlannedSet(it.role, it.weightKg) }, plan.note)
    }

    /** You set your target during session exercise [seId]: stored right away. */
    suspend fun setTarget(seId: Long, exerciseId: Long, target: Target) {
        savePlan(exerciseId, target.sets, target.note)
        marks.markSetByYou(seId)
    }

    /** The target you set yourself during session exercise [seId], or null if you did not. */
    suspend fun targetSetIn(seId: Long, exerciseId: Long): Target? =
        if (marks.isSetByYou(seId)) target(exerciseId) else null

    /** Stores the advice as target when moving on, unless you set one yourself during [seId]. */
    suspend fun storeAdvice(seId: Long, exerciseId: Long, advice: Target) {
        if (marks.isSetByYou(seId)) return
        savePlan(exerciseId, advice.sets, advice.note)
    }

    /** Whether [sessionId] is the most recent finished training with this exercise (then it drives the plan). */
    suspend fun isLatestFor(exerciseId: Long, sessionId: Long): Boolean =
        sessions.historyExercises(exerciseId, excludeSession = -1).firstOrNull()?.sessionId == sessionId

    /**
     * Plan for next time again from the (corrected) sets of a training, keeping the note.
     * [steps] are the achievable weights of the exercise's material.
     */
    suspend fun recomputePlan(exercise: ExerciseEntity, sessionId: Long, steps: WeightSteps?) {
        val se = sessions.exercises(sessionId).firstOrNull { it.exerciseId == exercise.id && !it.skipped } ?: return
        val sets = sessions.setsOf(se.id)
        if (sets.none { it.reps != null }) return
        val method = settings.current().method.let {
            it.copy(heavyTarget = exercise.heavyTarget ?: it.heavyTarget, lightTarget = exercise.lightTarget ?: it.lightTarget)
        }
        val plan = GoalAdvisor.planNext(
            sets.map { PerformedSet(it.role, it.weightKg, it.reps) },
            exercise.loadType, exercise.progression, method, steps,
        )
        savePlan(exercise.id, plan, planNote(exercise.id))
    }

    // ---- history ----

    suspend fun lastPerformance(exerciseId: Long, excludeSession: Long): LastPerformance? {
        val se = sessions.historyExercises(exerciseId, excludeSession).firstOrNull() ?: return null
        val session = sessions.get(se.sessionId) ?: return null
        return LastPerformance(session.startedAt.toLocalDate(), sessions.setsOf(se.id))
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

    /** Best heavy-set e1RM (effective weight) in earlier sessions. */
    suspend fun bestE1rm(exercise: ExerciseEntity, excludeSession: Long): Double? =
        sessions.historySets(exercise.id, excludeSession)
            .filter { it.role == SetRole.HEAVY && it.reps != null && it.weightKg != null }
            .mapNotNull { s -> effective(exercise, s)?.let { Strength.e1rm(it, s.reps!!) } }
            .maxOrNull()

    fun effective(exercise: ExerciseEntity, set: SetEntity): Double? =
        set.weightKg?.let { EffectiveWeight.of(exercise.loadType, it, set.bodyweightKg, exercise.bodyweightFactor) }

    suspend fun finish(sessionId: Long, now: Long = System.currentTimeMillis()) = db.withTransaction {
        val session = sessions.get(sessionId) ?: return@withTransaction
        sessions.exercises(sessionId).forEach { se -> if (se.doneAt == null && se.firstInputAt != null) sessions.updateExercise(se.copy(doneAt = now)) }
        sessions.update(session.copy(endedAt = now))
    }.also { onFinished() }

    suspend fun discard(sessionId: Long) = sessions.delete(sessionId)
}
