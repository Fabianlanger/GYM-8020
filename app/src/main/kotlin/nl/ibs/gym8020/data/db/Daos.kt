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

@Dao
interface BodyweightDao {
    @Query("SELECT * FROM bodyweights ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<BodyweightEntity>>

    @Query("SELECT * FROM bodyweights ORDER BY epochDay")
    suspend fun all(): List<BodyweightEntity>

    @Query("SELECT * FROM bodyweights WHERE epochDay <= :epochDay ORDER BY epochDay DESC LIMIT 1")
    suspend fun latestOnOrBefore(epochDay: Long): BodyweightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: BodyweightEntity)

    @Query("DELETE FROM bodyweights WHERE id = :id")
    suspend fun delete(id: Long)
}

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
