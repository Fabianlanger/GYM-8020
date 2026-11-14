package nl.ibs.gym8020.data

import android.util.Log
import androidx.room.withTransaction
import nl.ibs.gym8020.core.logic.BodyShare
import nl.ibs.gym8020.core.logic.BodyweightTarget
import nl.ibs.gym8020.core.logic.Measurement
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.SessionEntity

/**
 * #87, ADR 0002: the target of a bodyweight exercise is an effective weight. Until then `plan_sets` and `target_sets`
 * held the signed load; the columns stay (no new database version), only their meaning for bodyweight exercises
 * changes. This app step converts the targets once, each with the body weight of the training it belongs to
 * (`BodyweightTarget.bodyweightFor`): a kept target with its own training, the current target with the training that
 * stored it last. Run at every start and after importing a backup; [AppSettings.bodyweightTargetsEffective] says it
 * is done, so a second run changes nothing. Without any known body weight there is nothing to convert with: the step
 * waits, and the targets stay signed loads until a body weight is known.
 */
class BodyweightTargets(private val db: AppDatabase, private val settings: SettingsStore) {

    /** Converts once; true when it converted now. */
    suspend fun convertIfNeeded(): Boolean {
        if (settings.current().bodyweightTargetsEffective) return false
        return runCatching {
            val converted = convert(db)
            if (converted) settings.update { it.copy(bodyweightTargetsEffective = true) }
            converted
        }.onFailure { Log.e(TAG, "converting bodyweight targets failed; retried on the next start", it) }.getOrDefault(false)
    }

    companion object {
        private const val TAG = "BodyweightTargets"

        /**
         * Converts every target of a bodyweight exercise from signed load to effective weight, in one transaction.
         * False, with nothing changed, when no body weight is known at all.
         */
        suspend fun convert(db: AppDatabase, now: Long = System.currentTimeMillis()): Boolean = db.withTransaction {
            val measurements = db.bodyweights().all().map { Measurement(it.epochDay, it.measuredAt, it.kg) }
            val sessions = db.sessions().all().associateBy { it.id }
            // A training's own body weight, as a last resort the most recent one any training kept.
            val anySession = sessions.values.filter { it.bodyweightKg != null }.maxByOrNull { it.startedAt }?.bodyweightKg
            if (measurements.isEmpty() && anySession == null) return@withTransaction false

            fun bodyweightOf(session: SessionEntity?): Double? {
                val startedAt = session?.startedAt ?: now
                return BodyweightTarget.bodyweightFor(session?.bodyweightKg, measurements, startedAt, startedAt.toLocalDate().toEpochDay())
                    ?: anySession
            }

            val exercises = db.exercises().all().filter { it.loadType == LoadType.BODYWEIGHT }.associateBy { it.id }
            val sessionExercises = db.sessions().allExercises().filter { it.exerciseId in exercises }.associateBy { it.id }
            val targets = db.targets().all().filter { it.sessionExerciseId in sessionExercises }

            // Kept targets: with the body weight of their own training.
            for (target in targets) {
                val se = sessionExercises.getValue(target.sessionExerciseId)
                val sets = db.targets().sets(se.id)
                if (sets.none { it.weightKg != null }) continue
                val share = bodyweightOf(sessions[se.sessionId])?.let { BodyShare(it, exercises.getValue(se.exerciseId).bodyweightFactor) }
                val converted = BodyweightTarget.toEffective(sets.map { PlannedSet(it.role, it.weightKg, it.reps) }, share)
                db.targets().replace(target, sets.zip(converted) { s, c -> s.copy(weightKg = c.weightKg) })
            }

            // Current targets: with the body weight of the training that stored it last (else the latest one).
            val planSets = db.plans().allSets().filter { it.exerciseId in exercises }.groupBy { it.exerciseId }
            for ((exerciseId, sets) in planSets) {
                if (sets.none { it.weightKg != null }) continue
                val ofExercise = sessionExercises.values.filter { it.exerciseId == exerciseId }
                val storedIn = targets.filter { t -> ofExercise.any { it.id == t.sessionExerciseId } && t.setAt != null }
                    .maxByOrNull { it.setAt!! }?.let { sessionExercises[it.sessionExerciseId]?.sessionId }
                val session = storedIn?.let(sessions::get) ?: ofExercise.mapNotNull { sessions[it.sessionId] }.maxByOrNull { it.startedAt }
                val share = bodyweightOf(session)?.let { BodyShare(it, exercises.getValue(exerciseId).bodyweightFactor) }
                val converted = BodyweightTarget.toEffective(sets.map { PlannedSet(it.role, it.weightKg, it.reps) }, share)
                db.plans().updateSets(sets.zip(converted) { s, c -> s.copy(weightKg = c.weightKg) })
            }
            true
        }
    }
}
