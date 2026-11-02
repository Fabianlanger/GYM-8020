package nl.ibs.gym8020.core.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Full export of all user data (D-48) as one JSON file. Ids are local to the file; import
 * remaps them. Weights are always stored in kg; bodyweight extra is signed.
 *
 * Versions (A-7): every older version stays readable; fields added later have a default.
 * - 1: first format. 2: material profiles and the plate stock.
 * - 3 (schema v3, #11): gyms, the target per session exercise, session target in reps, "nog over" (RIR),
 *   origin of a training and of a body weight measurement, default warm-up and codes per exercise, material per
 *   session. `maxKg` of a material profile is gone (older files still have it; it is ignored).
 */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val exercises: List<ExerciseDto> = emptyList(),
    val schemas: List<SchemaDto> = emptyList(),
    val weekPlans: List<WeekPlanDto> = emptyList(),
    val bodyweights: List<BodyweightDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val materials: List<MaterialDto> = emptyList(),
    /** The plate stock of the first gym; kept for older app versions (v2). */
    val plateStock: String? = null,
    val gyms: List<GymDto> = emptyList(),
) {
    companion object {
        const val FORMAT = "gym-8020-backup"
        const val VERSION = 3
    }
}

/** A gym (A-4) with its plate stock as `PlateStock.encode()`; null = not set. */
@Serializable
data class GymDto(val id: Long, val name: String, val plates: String? = null, val archived: Boolean = false)

@Serializable
data class ExerciseDto(
    val id: Long,
    val libraryKey: String? = null,
    val name: String,
    val equipment: String,
    val loadType: String,
    val progression: String,
    val bodyweightFactor: Double,
    val heavyTarget: Int? = null,
    val lightTarget: Int? = null,
    val archived: Boolean = false,
    val muscles: List<MuscleShareDto> = emptyList(),
    val materialId: Long? = null,
    val movement: String? = null,
    /** Default warm-up (v3): signed for bodyweight exercises, and a short note. */
    val defaultWarmupKg: Double? = null,
    val defaultWarmupNote: String? = null,
    /** Standard codes (v3, A-5). */
    val healthConnectType: String? = null,
    val garminCategory: String? = null,
)

@Serializable
data class MaterialDto(
    val id: Long,
    val libraryKey: String? = null,
    val name: String,
    val kind: String,
    val baseKg: Double,
    val loadingPoints: Int,
    val minKg: Double,
    val stepKg: Double,
)

@Serializable
data class MuscleShareDto(val muscle: String, val percent: Int)

@Serializable
data class SchemaDto(
    val id: Long,
    val libraryKey: String? = null,
    val name: String,
    val archived: Boolean = false,
    val entries: List<SchemaEntryDto> = emptyList(),
)

@Serializable
data class SchemaEntryDto(val exerciseId: Long, val setCount: Int? = null)

@Serializable
data class WeekPlanDto(
    val id: Long,
    val libraryKey: String? = null,
    val name: String,
    val targetPerWeek: Int,
    val active: Boolean,
    val slots: List<WeekPlanSlotDto> = emptyList(),
)

/** [day] is ISO-8601 day of week 1 (Monday) … 7, or null. */
@Serializable
data class WeekPlanSlotDto(val schemaId: Long, val day: Int? = null)

/**
 * [date] is ISO epoch day. [measuredAt] (epoch millis, v3) orders several measurements on one day; null in older
 * backups (then noon of the day). [origin] is MANUAL or WEARABLE.
 */
@Serializable
data class BodyweightDto(val date: Long, val kg: Double, val measuredAt: Long? = null, val origin: String = "MANUAL")

@Serializable
data class SessionDto(
    val id: Long,
    val schemaId: Long? = null,
    val schemaName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val exercises: List<SessionExerciseDto> = emptyList(),
    /** LIVE, AFTERWARDS, IMPORTED or WEARABLE (v3, A-2); older trainings are live. */
    val origin: String = "LIVE",
    val gymId: Long? = null,
    /** Body weight at the start of the training (v3). */
    val bodyweightKg: Double? = null,
)

@Serializable
data class SessionExerciseDto(
    val exerciseId: Long,
    val shownAt: Long? = null,
    val warmupAt: Long? = null,
    val goAt: Long? = null,
    val firstInputAt: Long? = null,
    val doneAt: Long? = null,
    val skipped: Boolean = false,
    val note: String? = null,
    val sets: List<SetDto> = emptyList(),
    /** Material for this session only (v3, #48); null = the exercise's own. */
    val materialId: Long? = null,
    /** The target for next time set in this session exercise (v3). */
    val target: TargetDto? = null,
)

/** The target for next time of a session exercise: [origin] is YOURS, DEFAULT or UNKNOWN. */
@Serializable
data class TargetDto(val origin: String, val note: String? = null, val setAt: Long? = null, val sets: List<PlanSetDto> = emptyList())

@Serializable
data class SetDto(
    val role: String,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val bodyweightKg: Double? = null,
    val goalWeightKg: Double? = null,
    /** Session target in reps (v3). */
    val goalReps: Int? = null,
    /** "Nog over" (RIR, v3); null = to failure. */
    val repsInReserve: Int? = null,
)

@Serializable
data class PlanDto(val exerciseId: Long, val note: String? = null, val sets: List<PlanSetDto> = emptyList())

/** One set of a target; [reps] is the reps target with reps progression (#69), absent in older backups. */
@Serializable
data class PlanSetDto(val role: String, val weightKg: Double? = null, val reps: Int? = null)

object BackupJson {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    fun decode(text: String): Backup {
        val backup = json.decodeFromString(Backup.serializer(), text)
        require(backup.format == Backup.FORMAT) { "Not a GYM-8020 backup" }
        require(backup.version <= Backup.VERSION) { "Backup is from a newer app version" }
        return backup
    }
}
