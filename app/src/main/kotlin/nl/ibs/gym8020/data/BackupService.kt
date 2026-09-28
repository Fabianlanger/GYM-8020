package nl.ibs.gym8020.data

import androidx.room.withTransaction
import nl.ibs.gym8020.core.backup.Backup
import nl.ibs.gym8020.core.backup.BackupJson
import nl.ibs.gym8020.core.backup.BodyweightDto
import nl.ibs.gym8020.core.backup.ExerciseDto
import nl.ibs.gym8020.core.backup.MuscleShareDto
import nl.ibs.gym8020.core.backup.PlanDto
import nl.ibs.gym8020.core.backup.PlanSetDto
import nl.ibs.gym8020.core.backup.SchemaDto
import nl.ibs.gym8020.core.backup.SchemaEntryDto
import nl.ibs.gym8020.core.backup.SessionDto
import nl.ibs.gym8020.core.backup.SessionExerciseDto
import nl.ibs.gym8020.core.backup.SetDto
import nl.ibs.gym8020.core.backup.WeekPlanDto
import nl.ibs.gym8020.core.backup.WeekPlanSlotDto
import nl.ibs.gym8020.core.backup.MaterialDto
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.BodyweightEntity
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.ExerciseMuscleEntity
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import nl.ibs.gym8020.data.db.PlanEntity
import nl.ibs.gym8020.data.db.PlanSetEntity
import nl.ibs.gym8020.data.db.SchemaEntity
import nl.ibs.gym8020.data.db.SchemaEntryEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.data.db.WeekPlanEntity
import nl.ibs.gym8020.data.db.WeekPlanSlotEntity

/** Export/import of all data as one JSON file (D-48). Import replaces everything. */
class BackupService(private val db: AppDatabase, private val settings: SettingsStore) {

    suspend fun export(): String {
        val exercises = db.exercises().all()
        val muscles = db.exercises().allMuscles().groupBy { it.exerciseId }
        val entries = db.schemas().allEntries().groupBy { it.schemaId }
        val slots = db.weekPlans().allSlots().groupBy { it.weekPlanId }
        val seByS = db.sessions().allExercises().groupBy { it.sessionId }
        val setsBySe = db.sessions().allSets().groupBy { it.sessionExerciseId }
        val planSets = db.plans().allSets().groupBy { it.exerciseId }
        val backup = Backup(
            exportedAt = System.currentTimeMillis(),
            exercises = exercises.map { e ->
                ExerciseDto(
                    id = e.id, libraryKey = e.libraryKey, name = e.name, equipment = e.equipment.name,
                    loadType = e.loadType.name, progression = e.progression.name, bodyweightFactor = e.bodyweightFactor,
                    heavyTarget = e.heavyTarget, lightTarget = e.lightTarget, archived = e.archived,
                    muscles = muscles[e.id].orEmpty().map { MuscleShareDto(it.muscle.name, it.percent) },
                    materialId = e.materialProfileId, movement = e.movement?.name,
                )
            },
            schemas = db.schemas().all().map { s ->
                SchemaDto(s.id, s.libraryKey, s.name, s.archived, entries[s.id].orEmpty().map { SchemaEntryDto(it.exerciseId, it.setCount) })
            },
            weekPlans = db.weekPlans().all().map { p ->
                WeekPlanDto(p.id, p.libraryKey, p.name, p.targetPerWeek, p.active, slots[p.id].orEmpty().map { WeekPlanSlotDto(it.schemaId, it.day) })
            },
            bodyweights = db.bodyweights().all().map { BodyweightDto(it.epochDay, it.kg) },
            sessions = db.sessions().all().map { s ->
                SessionDto(
                    id = s.id, schemaId = s.schemaId, schemaName = s.schemaName, startedAt = s.startedAt, endedAt = s.endedAt,
                    exercises = seByS[s.id].orEmpty().sortedBy { it.position }.map { se ->
                        SessionExerciseDto(
                            exerciseId = se.exerciseId, shownAt = se.shownAt, warmupAt = se.warmupAt, goAt = se.goAt,
                            firstInputAt = se.firstInputAt, doneAt = se.doneAt, skipped = se.skipped,
                            sets = setsBySe[se.id].orEmpty().sortedBy { it.position }.map {
                                SetDto(it.role.name, it.weightKg, it.reps, it.bodyweightKg, it.goalWeightKg)
                            },
                        )
                    },
                )
            },
            materials = db.materials().all().map { m ->
                MaterialDto(m.id, m.libraryKey, m.name, m.kind.name, m.baseKg, m.loadingPoints, m.minKg, m.stepKg, m.maxKg)
            },
            plateStock = settings.current().plateStock.encode(),
            plans = db.plans().all().map { p ->
                PlanDto(p.exerciseId, p.note, planSets[p.exerciseId].orEmpty().map { PlanSetDto(it.role.name, it.weightKg) })
            },
        )
        return BackupJson.encode(backup)
    }

    /** Replaces all data with the backup. Throws on an invalid file before anything is changed. */
    suspend fun import(text: String) {
        val b = BackupJson.decode(text)
        db.withTransaction {
            val sql = db.openHelper.writableDatabase
            listOf(
                "sets", "session_exercises", "sessions", "plan_sets", "plans", "week_plan_slots", "week_plans",
                "schema_entries", "schemas", "exercise_muscles", "exercises", "bodyweights", "material_profiles",
            ).forEach { sql.execSQL("DELETE FROM $it") }
            val materialIds = HashMap<Long, Long>()
            b.materials.forEach { m ->
                materialIds[m.id] = db.materials().insert(
                    MaterialProfileEntity(
                        libraryKey = m.libraryKey, name = m.name, kind = enumOr(m.kind, MaterialKind.PLATES), baseKg = m.baseKg,
                        loadingPoints = m.loadingPoints, minKg = m.minKg, stepKg = m.stepKg, maxKg = m.maxKg,
                    ),
                )
            }
            val exIds = HashMap<Long, Long>()
            b.exercises.forEach { e ->
                val id = db.exercises().insert(
                    ExerciseEntity(
                        libraryKey = e.libraryKey, name = e.name, equipment = enumOr(e.equipment, Equipment.MACHINE),
                        loadType = enumOr(e.loadType, LoadType.EXTERNAL), progression = enumOr(e.progression, Progression.WEIGHT),
                        bodyweightFactor = e.bodyweightFactor, heavyTarget = e.heavyTarget, lightTarget = e.lightTarget, archived = e.archived,
                        materialProfileId = e.materialId?.let { materialIds[it] },
                        movement = e.movement?.let { runCatching { Movement.valueOf(it) }.getOrNull() },
                    ),
                )
                exIds[e.id] = id
                db.exercises().insertMuscles(
                    e.muscles.mapNotNull { m -> runCatching { Muscle.valueOf(m.muscle) }.getOrNull()?.let { ExerciseMuscleEntity(id, it, m.percent.coerceIn(1, 100)) } },
                )
            }
            val schemaIds = HashMap<Long, Long>()
            b.schemas.forEach { s ->
                val id = db.schemas().insert(SchemaEntity(libraryKey = s.libraryKey, name = s.name, archived = s.archived))
                schemaIds[s.id] = id
                db.schemas().insertEntries(
                    s.entries.mapNotNull { en -> exIds[en.exerciseId]?.let { SchemaEntryEntity(schemaId = id, exerciseId = it, position = 0, setCount = en.setCount) } }
                        .mapIndexed { i, en -> en.copy(position = i) },
                )
            }
            b.weekPlans.forEach { p ->
                val id = db.weekPlans().insert(WeekPlanEntity(libraryKey = p.libraryKey, name = p.name, targetPerWeek = p.targetPerWeek, active = p.active))
                db.weekPlans().insertSlots(
                    p.slots.mapNotNull { sl -> schemaIds[sl.schemaId]?.let { WeekPlanSlotEntity(weekPlanId = id, schemaId = it, position = 0, day = sl.day) } }
                        .mapIndexed { i, sl -> sl.copy(position = i) },
                )
            }
            b.bodyweights.forEach { db.bodyweights().upsert(BodyweightEntity(epochDay = it.date, kg = it.kg)) }
            b.sessions.forEach { s ->
                val sid = db.sessions().insert(
                    SessionEntity(schemaId = s.schemaId?.let { schemaIds[it] }, schemaName = s.schemaName, startedAt = s.startedAt, endedAt = s.endedAt),
                )
                s.exercises.forEachIndexed { i, se ->
                    val exId = exIds[se.exerciseId] ?: return@forEachIndexed
                    val seId = db.sessions().insertExercise(
                        SessionExerciseEntity(
                            sessionId = sid, exerciseId = exId, position = i, shownAt = se.shownAt, warmupAt = se.warmupAt,
                            goAt = se.goAt, firstInputAt = se.firstInputAt, doneAt = se.doneAt, skipped = se.skipped,
                        ),
                    )
                    db.sessions().insertSets(
                        se.sets.mapIndexed { j, st ->
                            SetEntity(
                                sessionExerciseId = seId, position = j, role = enumOr(st.role, SetRole.HEAVY), weightKg = st.weightKg,
                                reps = st.reps, bodyweightKg = st.bodyweightKg, goalWeightKg = st.goalWeightKg,
                            )
                        },
                    )
                }
            }
            b.plans.forEach { p ->
                val exId = exIds[p.exerciseId] ?: return@forEach
                db.plans().replace(
                    PlanEntity(exerciseId = exId, note = p.note, updatedAt = System.currentTimeMillis()),
                    p.sets.map { PlanSetEntity(exerciseId = exId, position = 0, role = enumOr(it.role, SetRole.HEAVY), weightKg = it.weightKg) },
                )
            }
        }
    }

    /** Called after [import]: the plate stock lives in the settings, not in the database. */
    suspend fun importSettings(text: String) {
        val b = BackupJson.decode(text)
        // A backup without materials (v1) gets the standard profiles again via the seeder.
        settings.update { it.copy(plateStock = b.plateStock?.let(PlateStock::decode) ?: it.plateStock, seededMaterials = b.materials.isNotEmpty()) }
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
}
