package nl.ibs.gym8020.core.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Full export of all user data (D-48) as one JSON file. Ids are local to the file; import
 * remaps them. Weights are always stored in kg; bodyweight extra is signed.
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
) {
    companion object {
        const val FORMAT = "gym-8020-backup"
        const val VERSION = 1
    }
}

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

/** [date] is ISO epoch day. */
@Serializable
data class BodyweightDto(val date: Long, val kg: Double)

@Serializable
data class SessionDto(
    val id: Long,
    val schemaId: Long? = null,
    val schemaName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val exercises: List<SessionExerciseDto> = emptyList(),
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
)

@Serializable
data class SetDto(
    val role: String,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val bodyweightKg: Double? = null,
    val goalWeightKg: Double? = null,
)

@Serializable
data class PlanDto(val exerciseId: Long, val note: String? = null, val sets: List<PlanSetDto> = emptyList())

@Serializable
data class PlanSetDto(val role: String, val weightKg: Double? = null)

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
