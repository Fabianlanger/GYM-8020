package nl.ibs.gym8020.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole

@Entity(tableName = "exercises", indices = [Index("libraryKey", unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val libraryKey: String? = null,
    val name: String,
    val equipment: Equipment,
    val loadType: LoadType,
    val progression: Progression,
    val bodyweightFactor: Double = 1.0,
    /** Per-exercise override of the global rep targets (D-09). */
    val heavyTarget: Int? = null,
    val lightTarget: Int? = null,
    val archived: Boolean = false,
    /** Material profile (v0.2); null = no rounding to achievable weights. */
    val materialProfileId: Long? = null,
    /** Movement pattern for the icon; null = derived from the library or equipment. */
    val movement: Movement? = null,
)

/** Reusable material profile (D-27). Added in database version 2. */
@Entity(tableName = "material_profiles")
data class MaterialProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val libraryKey: String? = null,
    val name: String,
    val kind: MaterialKind,
    val baseKg: Double = 0.0,
    val loadingPoints: Int = 2,
    val minKg: Double = 0.0,
    val stepKg: Double = 5.0,
    val maxKg: Double = 100.0,
    val archived: Boolean = false,
) {
    fun toProfile() = MaterialProfile(kind, baseKg, loadingPoints, minKg, stepKg, maxKg)
}

@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exerciseId", "muscle"],
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class ExerciseMuscleEntity(val exerciseId: Long, val muscle: Muscle, val percent: Int)

@Entity(tableName = "schemas", indices = [Index("libraryKey")])
data class SchemaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val libraryKey: String? = null,
    val name: String,
    val archived: Boolean = false,
)

@Entity(
    tableName = "schema_entries",
    indices = [Index("schemaId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(SchemaEntity::class, ["id"], ["schemaId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class SchemaEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schemaId: Long,
    val exerciseId: Long,
    val position: Int,
    /** Overrides the default number of sets for this schema (null = default). */
    val setCount: Int? = null,
)

@Entity(tableName = "week_plans")
data class WeekPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val libraryKey: String? = null,
    val name: String,
    val targetPerWeek: Int,
    val active: Boolean = false,
)

@Entity(
    tableName = "week_plan_slots",
    indices = [Index("weekPlanId"), Index("schemaId")],
    foreignKeys = [
        ForeignKey(WeekPlanEntity::class, ["id"], ["weekPlanId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SchemaEntity::class, ["id"], ["schemaId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class WeekPlanSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekPlanId: Long,
    val schemaId: Long,
    val position: Int,
    /** ISO day of week 1..7 or null. */
    val day: Int? = null,
)

@Entity(tableName = "bodyweights", indices = [Index("epochDay", unique = true)])
data class BodyweightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kg: Double,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schemaId: Long? = null,
    val schemaName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    /** Body weight at the start of the training, copied to bodyweight sets (history must not shift). */
    val bodyweightKg: Double? = null,
)

@Entity(
    tableName = "session_exercises",
    indices = [Index("sessionId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val shownAt: Long? = null,
    val warmupAt: Long? = null,
    val goAt: Long? = null,
    val firstInputAt: Long? = null,
    val doneAt: Long? = null,
    val skipped: Boolean = false,
)

@Entity(
    tableName = "sets",
    indices = [Index("sessionExerciseId")],
    foreignKeys = [ForeignKey(SessionExerciseEntity::class, ["id"], ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int,
    val role: SetRole,
    /** Entered weight in kg; signed extra for bodyweight exercises. */
    val weightKg: Double? = null,
    val reps: Int? = null,
    val bodyweightKg: Double? = null,
    val goalWeightKg: Double? = null,
)

/** The plan for next time per exercise (D-18). */
@Entity(
    tableName = "plans",
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class PlanEntity(
    @PrimaryKey val exerciseId: Long,
    val note: String? = null,
    val updatedAt: Long,
)

@Entity(
    tableName = "plan_sets",
    indices = [Index("exerciseId")],
    foreignKeys = [ForeignKey(PlanEntity::class, ["exerciseId"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class PlanSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val position: Int,
    val role: SetRole,
    val weightKg: Double? = null,
)
