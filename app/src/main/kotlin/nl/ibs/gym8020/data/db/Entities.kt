package nl.ibs.gym8020.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MeasurementOrigin
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SessionOrigin
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.TargetOrigin

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
    /** Default warm-up (schema v3): a weight, signed for bodyweight exercises (ADR 0001); null = none. */
    val defaultWarmupKg: Double? = null,
    /** Short note with the default warm-up (schema v3). */
    val defaultWarmupNote: String? = null,
    /** Health Connect exercise segment type without the `SEGMENT_TYPE_` prefix (schema v3, A-5); null = unknown. */
    val healthConnectType: String? = null,
    /** Garmin FIT exercise category (schema v3, A-5); null = unknown. */
    val garminCategory: String? = null,
)

/** Reusable material profile (D-27). Added in database version 2; `maxKg` removed in version 3 (#6). */
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
    val archived: Boolean = false,
) {
    fun toProfile() = MaterialProfile(kind, baseKg, loadingPoints, minKg, stepKg)
}

/** A gym (A-4, schema v3) with its plate stock. Until gyms can be chosen (E4) the first one is yours. */
@Entity(tableName = "gyms")
data class GymEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Plate stock as `PlateStock.encode()`; null = not moved from the settings yet (the settings value applies). */
    val plates: String? = null,
    val archived: Boolean = false,
)

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

/**
 * A body weight measurement. Since schema v3 a day can have several (V-14): the last one on or before a day counts
 * (`BodyweightLog`). [epochDay] is the local day, [measuredAt] the moment (epoch millis).
 */
@Entity(tableName = "bodyweights", indices = [Index("epochDay")])
data class BodyweightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kg: Double,
    val measuredAt: Long,
    val origin: MeasurementOrigin = MeasurementOrigin.MANUAL,
)

@Entity(
    tableName = "sessions",
    indices = [Index("gymId")],
    foreignKeys = [ForeignKey(GymEntity::class, ["id"], ["gymId"], onDelete = ForeignKey.SET_NULL)],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schemaId: Long? = null,
    val schemaName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    /** Body weight at the start of the training, copied to bodyweight sets (history must not shift). */
    val bodyweightKg: Double? = null,
    /** Where the training comes from (schema v3, A-2). */
    val origin: SessionOrigin = SessionOrigin.LIVE,
    /** The gym of this training (schema v3, A-4); null = unknown. */
    val gymId: Long? = null,
)

@Entity(
    tableName = "session_exercises",
    indices = [Index("sessionId"), Index("exerciseId"), Index("materialProfileId")],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MaterialProfileEntity::class, ["id"], ["materialProfileId"], onDelete = ForeignKey.SET_NULL),
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
    /** Material for this session only (schema v3, #48); null = the exercise's own profile. */
    val materialProfileId: Long? = null,
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
    /** Session target in reps (schema v3, #69): the reps aimed at as the training started. */
    val goalReps: Int? = null,
    /** "Nog over" (RIR, schema v3, D-20); null = to failure. */
    val repsInReserve: Int? = null,
)

/**
 * The current target for next time per exercise (D-18): what the next session starts from. Every stored target is
 * also kept per session exercise in [TargetEntity] (schema v3).
 */
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
    /** Reps target (schema v3, #69). */
    val reps: Int? = null,
)

/**
 * The target for next time set in a session exercise (schema v3): kept for every session, also when a later
 * session replaces it. [origin] says who set it: YOURS when you set at least one of its sets yourself (schema v4,
 * #92; who set each set is in [TargetSetEntity.origin]). [setAt] is null when unknown (derived afterwards, A-6).
 */
@Entity(
    tableName = "targets",
    foreignKeys = [ForeignKey(SessionExerciseEntity::class, ["id"], ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class TargetEntity(
    @PrimaryKey val sessionExerciseId: Long,
    val origin: TargetOrigin,
    val note: String? = null,
    val setAt: Long? = null,
)

/**
 * One set of a [TargetEntity]: role, weight and/or reps, and who set this set (schema v4, #92): YOURS when you changed
 * it or took over its advice, DEFAULT when it follows your performance, UNKNOWN when derived afterwards.
 */
@Entity(
    tableName = "target_sets",
    indices = [Index("sessionExerciseId")],
    foreignKeys = [ForeignKey(TargetEntity::class, ["sessionExerciseId"], ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class TargetSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int,
    val role: SetRole,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val origin: TargetOrigin = TargetOrigin.DEFAULT,
)
