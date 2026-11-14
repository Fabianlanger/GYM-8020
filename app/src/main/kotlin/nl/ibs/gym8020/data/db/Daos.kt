package nl.ibs.gym8020.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun all(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observe(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert
    suspend fun insert(e: ExerciseEntity): Long

    @Update
    suspend fun update(e: ExerciseEntity)

    @Query("SELECT * FROM exercise_muscles")
    fun observeAllMuscles(): Flow<List<ExerciseMuscleEntity>>

    @Query("SELECT * FROM exercise_muscles")
    suspend fun allMuscles(): List<ExerciseMuscleEntity>

    @Query("SELECT * FROM exercise_muscles WHERE exerciseId = :id")
    suspend fun muscles(id: Long): List<ExerciseMuscleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscles(list: List<ExerciseMuscleEntity>)

    @Query("DELETE FROM exercise_muscles WHERE exerciseId = :id")
    suspend fun deleteMuscles(id: Long)

    @Transaction
    suspend fun replaceMuscles(id: Long, list: List<ExerciseMuscleEntity>) {
        deleteMuscles(id)
        insertMuscles(list)
    }

    @Query("SELECT COUNT(*) FROM session_exercises WHERE exerciseId = :id")
    suspend fun historyCount(id: Long): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SchemaDao {
    @Query("SELECT * FROM schemas WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<SchemaEntity>>

    @Query("SELECT * FROM schemas")
    suspend fun all(): List<SchemaEntity>

    @Query("SELECT * FROM schemas WHERE id = :id")
    suspend fun get(id: Long): SchemaEntity?

    @Query("SELECT * FROM schemas WHERE id = :id")
    fun observe(id: Long): Flow<SchemaEntity?>

    @Insert
    suspend fun insert(s: SchemaEntity): Long

    @Update
    suspend fun update(s: SchemaEntity)

    @Query("SELECT * FROM schema_entries WHERE schemaId = :schemaId ORDER BY position")
    fun observeEntries(schemaId: Long): Flow<List<SchemaEntryEntity>>

    @Query("SELECT * FROM schema_entries WHERE schemaId = :schemaId ORDER BY position")
    suspend fun entries(schemaId: Long): List<SchemaEntryEntity>

    @Query("SELECT * FROM schema_entries ORDER BY schemaId, position")
    fun observeAllEntries(): Flow<List<SchemaEntryEntity>>

    @Query("SELECT * FROM schema_entries ORDER BY schemaId, position")
    suspend fun allEntries(): List<SchemaEntryEntity>

    @Insert
    suspend fun insertEntries(list: List<SchemaEntryEntity>)

    @Query("DELETE FROM schema_entries WHERE schemaId = :schemaId")
    suspend fun deleteEntries(schemaId: Long)

    @Transaction
    suspend fun replaceEntries(schemaId: Long, list: List<SchemaEntryEntity>) {
        deleteEntries(schemaId)
        insertEntries(list.mapIndexed { i, e -> e.copy(id = 0, schemaId = schemaId, position = i) })
    }
}

@Dao
interface WeekPlanDao {
    @Query("SELECT * FROM week_plans ORDER BY id")
    fun observeAll(): Flow<List<WeekPlanEntity>>

    @Query("SELECT * FROM week_plans ORDER BY id")
    suspend fun all(): List<WeekPlanEntity>

    @Query("SELECT * FROM week_plans WHERE active = 1 LIMIT 1")
    fun observeActive(): Flow<WeekPlanEntity?>

    @Query("SELECT * FROM week_plans WHERE id = :id")
    suspend fun get(id: Long): WeekPlanEntity?

    @Insert
    suspend fun insert(p: WeekPlanEntity): Long

    @Update
    suspend fun update(p: WeekPlanEntity)

    @Query("DELETE FROM week_plans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE week_plans SET active = (id = :id)")
    suspend fun activate(id: Long)

    @Query("SELECT * FROM week_plan_slots WHERE weekPlanId = :planId ORDER BY position")
    fun observeSlots(planId: Long): Flow<List<WeekPlanSlotEntity>>

    @Query("SELECT * FROM week_plan_slots ORDER BY weekPlanId, position")
    suspend fun allSlots(): List<WeekPlanSlotEntity>

    @Query("SELECT * FROM week_plan_slots ORDER BY weekPlanId, position")
    fun observeAllSlots(): Flow<List<WeekPlanSlotEntity>>

    @Insert
    suspend fun insertSlots(list: List<WeekPlanSlotEntity>)

    @Query("DELETE FROM week_plan_slots WHERE weekPlanId = :planId")
    suspend fun deleteSlots(planId: Long)

    @Transaction
    suspend fun replaceSlots(planId: Long, list: List<WeekPlanSlotEntity>) {
        deleteSlots(planId)
        insertSlots(list.mapIndexed { i, s -> s.copy(id = 0, weekPlanId = planId, position = i) })
    }
}

/**
 * Body weight measurements; a day can have several (schema v3, V-14). The last one on or before a day counts
 * (`BodyweightLog`): latest day first, then latest moment.
 */
@Dao
interface BodyweightDao {
    /** Newest first. */
    @Query("SELECT * FROM bodyweights ORDER BY epochDay DESC, measuredAt DESC, id DESC")
    fun observeAll(): Flow<List<BodyweightEntity>>

    /** Oldest first. */
    @Query("SELECT * FROM bodyweights ORDER BY epochDay, measuredAt, id")
    suspend fun all(): List<BodyweightEntity>

    /** The body weight of [epochDay]: its last measurement, or else the last one before it. */
    @Query("SELECT * FROM bodyweights WHERE epochDay <= :epochDay ORDER BY epochDay DESC, measuredAt DESC, id DESC LIMIT 1")
    suspend fun latestOnOrBefore(epochDay: Long): BodyweightEntity?

    @Insert
    suspend fun insert(b: BodyweightEntity): Long

    @Query("DELETE FROM bodyweights WHERE epochDay = :epochDay AND origin = 'MANUAL'")
    suspend fun deleteManualOfDay(epochDay: Long)

    /**
     * Your measurement of a day as the screens enter it today: it replaces your earlier manual measurement of that
     * day; measurements from a wearable stay. Adding several per day comes with the body weight screen (V-14).
     */
    @Transaction
    suspend fun replaceManualOfDay(b: BodyweightEntity) {
        deleteManualOfDay(b.epochDay)
        insert(b)
    }

    @Query("DELETE FROM bodyweights WHERE id = :id")
    suspend fun delete(id: Long)
}

/** Row of [SessionDao.observeWorkSetCounts]. */
data class SessionSetCount(val sessionId: Long, val workSets: Int)

/** Row of [SessionDao.historyDates]: a session exercise with the training date of its session. */
data class SessionExerciseDate(val sessionExerciseId: Long, val startedAt: Long, val skipped: Boolean)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpen(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt")
    suspend fun all(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observe(id: Long): Flow<SessionEntity?>

    @Insert
    suspend fun insert(s: SessionEntity): Long

    @Update
    suspend fun update(s: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    fun observeExercises(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun exercises(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_exercises")
    suspend fun allExercises(): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun exercise(id: Long): SessionExerciseEntity?

    @Insert
    suspend fun insertExercise(e: SessionExerciseEntity): Long

    @Update
    suspend fun updateExercise(e: SessionExerciseEntity)

    @Update
    suspend fun updateExercises(list: List<SessionExerciseEntity>)

    @Query("DELETE FROM session_exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    /** Number of performed work sets per training, for the log. */
    @Query(
        "SELECT se.sessionId AS sessionId, COUNT(s.id) AS workSets FROM sets s JOIN session_exercises se ON s.sessionExerciseId = se.id " +
            "WHERE s.reps IS NOT NULL AND s.role != 'WARMUP' AND se.skipped = 0 GROUP BY se.sessionId",
    )
    fun observeWorkSetCounts(): Flow<List<SessionSetCount>>

    @Query(
        "SELECT s.* FROM sets s JOIN session_exercises se ON s.sessionExerciseId = se.id " +
            "WHERE se.sessionId = :sessionId ORDER BY se.position, s.position",
    )
    fun observeSets(sessionId: Long): Flow<List<SetEntity>>

    @Query(
        "SELECT s.* FROM sets s JOIN session_exercises se ON s.sessionExerciseId = se.id " +
            "WHERE se.sessionId = :sessionId ORDER BY se.position, s.position",
    )
    suspend fun sets(sessionId: Long): List<SetEntity>

    @Query("SELECT * FROM sets WHERE sessionExerciseId = :id ORDER BY position")
    suspend fun setsOf(id: Long): List<SetEntity>

    @Query("SELECT * FROM sets")
    suspend fun allSets(): List<SetEntity>

    @Insert
    suspend fun insertSet(s: SetEntity): Long

    @Insert
    suspend fun insertSets(list: List<SetEntity>)

    @Update
    suspend fun updateSet(s: SetEntity)

    @Update
    suspend fun updateSets(list: List<SetEntity>)

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    /** Sets of all finished sessions for an exercise, newest session first. */
    @Query(
        "SELECT s.* FROM sets s JOIN session_exercises se ON s.sessionExerciseId = se.id " +
            "JOIN sessions ss ON se.sessionId = ss.id " +
            "WHERE se.exerciseId = :exerciseId AND ss.endedAt IS NOT NULL AND ss.id != :excludeSession " +
            "ORDER BY ss.startedAt DESC, s.position",
    )
    suspend fun historySets(exerciseId: Long, excludeSession: Long): List<SetEntity>

    /** Session exercise rows of finished sessions for an exercise, newest first. */
    @Query(
        "SELECT se.* FROM session_exercises se JOIN sessions ss ON se.sessionId = ss.id " +
            "WHERE se.exerciseId = :exerciseId AND ss.endedAt IS NOT NULL AND ss.id != :excludeSession AND se.skipped = 0 " +
            "ORDER BY ss.startedAt DESC",
    )
    suspend fun historyExercises(exerciseId: Long, excludeSession: Long): List<SessionExerciseEntity>

    /** Session exercises of finished sessions for an exercise with their training date, skipped ones too, newest first. */
    @Query(
        "SELECT se.id AS sessionExerciseId, ss.startedAt AS startedAt, se.skipped AS skipped " +
            "FROM session_exercises se JOIN sessions ss ON se.sessionId = ss.id " +
            "WHERE se.exerciseId = :exerciseId AND ss.endedAt IS NOT NULL AND ss.id != :excludeSession " +
            "ORDER BY ss.startedAt DESC",
    )
    suspend fun historyDates(exerciseId: Long, excludeSession: Long): List<SessionExerciseDate>
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans WHERE exerciseId = :exerciseId")
    suspend fun get(exerciseId: Long): PlanEntity?

    @Query("SELECT * FROM plan_sets WHERE exerciseId = :exerciseId ORDER BY position")
    suspend fun sets(exerciseId: Long): List<PlanSetEntity>

    @Query("SELECT * FROM plans")
    suspend fun all(): List<PlanEntity>

    @Query("SELECT * FROM plan_sets ORDER BY exerciseId, position")
    suspend fun allSets(): List<PlanSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: PlanEntity)

    @Query("DELETE FROM plan_sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSets(exerciseId: Long)

    @Insert
    suspend fun insertSets(list: List<PlanSetEntity>)

    @Update
    suspend fun updateSets(list: List<PlanSetEntity>)

    @Transaction
    suspend fun replace(plan: PlanEntity, sets: List<PlanSetEntity>) {
        upsert(plan)
        deleteSets(plan.exerciseId)
        insertSets(sets.mapIndexed { i, s -> s.copy(id = 0, exerciseId = plan.exerciseId, position = i) })
    }
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material_profiles WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<MaterialProfileEntity>>

    @Query("SELECT * FROM material_profiles")
    fun observeAll(): Flow<List<MaterialProfileEntity>>

    @Query("SELECT * FROM material_profiles")
    suspend fun all(): List<MaterialProfileEntity>

    @Query("SELECT * FROM material_profiles WHERE id = :id")
    suspend fun get(id: Long): MaterialProfileEntity?

    @Query("SELECT COUNT(*) FROM material_profiles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(m: MaterialProfileEntity): Long

    @Update
    suspend fun update(m: MaterialProfileEntity)

    @Query("UPDATE exercises SET materialProfileId = NULL WHERE materialProfileId = :id")
    suspend fun detach(id: Long)

    @Query("DELETE FROM material_profiles WHERE id = :id")
    suspend fun deleteRow(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        detach(id)
        deleteRow(id)
    }
}

@Dao
interface GymDao {
    @Query("SELECT * FROM gyms ORDER BY id")
    fun observeAll(): Flow<List<GymEntity>>

    @Query("SELECT * FROM gyms ORDER BY id")
    suspend fun all(): List<GymEntity>

    @Insert
    suspend fun insert(g: GymEntity): Long

    @Update
    suspend fun update(g: GymEntity)
}

/** The target for next time per session exercise (schema v3). */
@Dao
interface TargetDao {
    @Query("SELECT * FROM targets WHERE sessionExerciseId = :seId")
    suspend fun get(seId: Long): TargetEntity?

    @Query("SELECT * FROM target_sets WHERE sessionExerciseId = :seId ORDER BY position")
    suspend fun sets(seId: Long): List<TargetSetEntity>

    /**
     * The target kept by the latest other training with this exercise that started no later than [startedAt]: the
     * target a training started at that moment started from (#92). Null when there is none.
     */
    @Query(
        "SELECT targets.* FROM targets " +
            "INNER JOIN session_exercises ON session_exercises.id = targets.sessionExerciseId " +
            "INNER JOIN sessions ON sessions.id = session_exercises.sessionId " +
            "WHERE session_exercises.exerciseId = :exerciseId AND sessions.id != :sessionId AND sessions.startedAt <= :startedAt " +
            "ORDER BY sessions.startedAt DESC, session_exercises.id DESC LIMIT 1",
    )
    suspend fun latestBefore(exerciseId: Long, sessionId: Long, startedAt: Long): TargetEntity?

    @Query("SELECT * FROM targets")
    suspend fun all(): List<TargetEntity>

    @Query("SELECT * FROM target_sets ORDER BY sessionExerciseId, position")
    suspend fun allSets(): List<TargetSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: TargetEntity)

    @Update
    suspend fun update(t: TargetEntity)

    @Query("DELETE FROM target_sets WHERE sessionExerciseId = :seId")
    suspend fun deleteSets(seId: Long)

    @Insert
    suspend fun insertSets(list: List<TargetSetEntity>)

    @Query("DELETE FROM targets WHERE sessionExerciseId = :seId")
    suspend fun delete(seId: Long)

    /** Stores the target of a session exercise, replacing an earlier one of that session exercise. */
    @Transaction
    suspend fun replace(target: TargetEntity, sets: List<TargetSetEntity>) {
        deleteSets(target.sessionExerciseId)
        upsert(target)
        insertSets(sets.mapIndexed { i, s -> s.copy(id = 0, sessionExerciseId = target.sessionExerciseId, position = i) })
    }
}
