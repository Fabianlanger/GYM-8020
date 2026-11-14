package nl.ibs.gym8020.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import nl.ibs.gym8020.core.library.ExerciseCodeTable
import nl.ibs.gym8020.core.logic.BackfillEntry
import nl.ibs.gym8020.core.logic.FromCurrentTarget
import nl.ibs.gym8020.core.logic.FromNextSession
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.TargetBackfill
import nl.ibs.gym8020.core.model.TargetOrigin
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.TargetEntity
import nl.ibs.gym8020.data.db.TargetSetEntity

/**
 * What the interim stores outside the database held before schema v3 (B-6, #69): in which session exercises you
 * set the target yourself ("target_marks"), and the reps targets ("reps_targets": `target_<exerciseId>` reps per
 * set of the current target, `goal_<setId>` the session target in reps). Read once to move them into the database.
 */
data class InterimStores(
    val setByYou: Set<Long> = emptySet(),
    val planReps: Map<Long, List<Int?>> = emptyMap(),
    val goalReps: Map<Long, Int> = emptyMap(),
) {
    companion object {
        const val MARKS = "target_marks"
        const val REPS = "reps_targets"

        fun read(context: Context): InterimStores {
            val marks = context.getSharedPreferences(MARKS, Context.MODE_PRIVATE).all
            val reps = context.getSharedPreferences(REPS, Context.MODE_PRIVATE).all
            return InterimStores(
                setByYou = marks.filterValues { it == true }.keys.mapNotNull { it.toLongOrNull() }.toSet(),
                planReps = reps.mapNotNull { (key, value) ->
                    val id = key.removePrefix("target_").takeIf { key.startsWith("target_") }?.toLongOrNull() ?: return@mapNotNull null
                    id to (value as? String ?: return@mapNotNull null).split(",").map { it.toIntOrNull() }
                }.toMap(),
                goalReps = reps.mapNotNull { (key, value) ->
                    val id = key.removePrefix("goal_").takeIf { key.startsWith("goal_") }?.toLongOrNull() ?: return@mapNotNull null
                    id to (value as? Int ?: return@mapNotNull null)
                }.toMap(),
            )
        }

        /** Removes both stores, only after their data is safely in the database. */
        fun delete(context: Context) {
            listOf(MARKS, REPS).forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
                context.deleteSharedPreferences(name)
            }
        }
    }
}

/**
 * The one-time steps of schema v3 (#11) that the database migration cannot do, run after the database is open:
 * - your gym gets the plate stock from the settings (every start, idempotent; A-4);
 * - once: the interim stores move into the database, older trainings get their derived target for next time
 *   (owner decision Q3), and library exercises get their standard codes (A-5). The interim store files are deleted
 *   only after that succeeded. Every step is idempotent, so an interrupted run is simply repeated.
 */
class V3Upgrade(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val gyms: Gyms,
    private val dutch: Boolean,
) {
    suspend fun run() {
        runCatching { gyms.ensureDefault(dutch) }.onFailure { Log.e(TAG, "gym step failed", it) }
        if (settings.current().v3DataMoved) return
        runCatching {
            moveInto(db, InterimStores.read(context))
            settings.update { it.copy(v3DataMoved = true) }
            InterimStores.delete(context)
        }.onFailure { Log.e(TAG, "moving data into schema v3 failed; retried on the next start", it) }
    }

    companion object {
        private const val TAG = "V3Upgrade"

        /** Moves [stores] into the database, then derives missing targets and fills library codes; one transaction. */
        suspend fun moveInto(db: AppDatabase, stores: InterimStores) = db.withTransaction {
            // Reps of the current target, by position (they were kept next to the plan's sets).
            stores.planReps.forEach { (exerciseId, reps) ->
                val sets = db.plans().sets(exerciseId)
                val updated = sets.mapIndexed { i, s -> s.copy(reps = (if (s.role.isWork) reps.getOrNull(i) else null) ?: s.reps) }
                if (updated != sets) db.plans().updateSets(updated)
            }
            // The session target in reps.
            if (stores.goalReps.isNotEmpty()) {
                val sets = db.sessions().allSets().filter { it.id in stores.goalReps }
                db.sessions().updateSets(sets.map { it.copy(goalReps = stores.goalReps.getValue(it.id)) })
            }
            // Older trainings: their target for next time (origin unknown).
            backfillTargets(db)
            // Targets you set yourself: the kept target becomes yours; without one, the current target is it.
            stores.setByYou.forEach { seId ->
                val se = db.sessions().exercise(seId) ?: return@forEach
                val kept = db.targets().get(seId)
                if (kept != null) {
                    // Every set of it is yours as well (schema v4, #92).
                    if (kept.origin != TargetOrigin.YOURS || db.targets().sets(seId).any { it.origin != TargetOrigin.YOURS }) {
                        db.targets().replace(kept.copy(origin = TargetOrigin.YOURS), db.targets().sets(seId).map { it.copy(origin = TargetOrigin.YOURS) })
                    }
                } else {
                    val plan = db.plans().get(se.exerciseId) ?: return@forEach
                    db.targets().replace(
                        TargetEntity(sessionExerciseId = seId, origin = TargetOrigin.YOURS, note = plan.note, setAt = null),
                        db.plans().sets(se.exerciseId).map {
                            TargetSetEntity(
                                sessionExerciseId = seId, position = 0, role = it.role, weightKg = it.weightKg, reps = it.reps,
                                origin = TargetOrigin.YOURS,
                            )
                        },
                    )
                }
            }
            fillLibraryCodes(db)
        }

        /**
         * Gives every session exercise without a kept target the one [TargetBackfill] derives: the session target of
         * the next training with that exercise, or for the latest finished training the current target. Origin
         * unknown, no time (A-6). Idempotent: session exercises with a target are left alone.
         */
        suspend fun backfillTargets(db: AppDatabase): Int = db.withTransaction {
            val sessions = db.sessions().all().associateBy { it.id }
            val sets = db.sessions().allSets().groupBy { it.sessionExerciseId }
            val entries = db.sessions().allExercises().mapNotNull { se ->
                val session = sessions[se.sessionId] ?: return@mapNotNull null
                BackfillEntry(
                    sessionExerciseId = se.id,
                    exerciseId = se.exerciseId,
                    sessionId = session.id,
                    startedAt = session.startedAt,
                    finished = session.endedAt != null,
                    skipped = se.skipped,
                    sessionTarget = sets[se.id].orEmpty().sortedBy { it.position }.map { PlannedSet(it.role, it.goalWeightKg, it.goalReps) },
                )
            }
            val withTarget = db.targets().all().map { it.sessionExerciseId }.toSet()
            var stored = 0
            for (b in TargetBackfill.derive(entries, withTarget)) {
                val (target, note) = when (b) {
                    is FromNextSession -> b.sets to null
                    is FromCurrentTarget -> {
                        val plan = db.plans().get(b.exerciseId) ?: continue
                        db.plans().sets(b.exerciseId).map { PlannedSet(it.role, it.weightKg, it.reps) } to plan.note
                    }
                }
                if (target.isEmpty()) continue
                db.targets().replace(
                    TargetEntity(sessionExerciseId = b.sessionExerciseId, origin = TargetOrigin.UNKNOWN, note = note, setAt = null),
                    target.map {
                        TargetSetEntity(
                            sessionExerciseId = b.sessionExerciseId, position = 0, role = it.role, weightKg = it.weightKg, reps = it.reps,
                            origin = TargetOrigin.UNKNOWN,
                        )
                    },
                )
                stored++
            }
            stored
        }

        /** Library exercises get their standard codes (A-5) where they have none. */
        suspend fun fillLibraryCodes(db: AppDatabase) {
            db.exercises().all().forEach { e ->
                val codes = ExerciseCodeTable.of(e.libraryKey) ?: return@forEach
                val next = e.copy(
                    healthConnectType = e.healthConnectType ?: codes.healthConnect,
                    garminCategory = e.garminCategory ?: codes.garmin,
                )
                if (next != e) db.exercises().update(next)
            }
        }
    }
}
