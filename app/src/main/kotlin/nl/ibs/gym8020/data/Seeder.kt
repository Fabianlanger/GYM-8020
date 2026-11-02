package nl.ibs.gym8020.data

import androidx.room.withTransaction
import nl.ibs.gym8020.core.library.DefaultLibrary
import nl.ibs.gym8020.core.library.ExerciseCodeTable
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.ExerciseMuscleEntity
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import nl.ibs.gym8020.data.db.SchemaEntity
import nl.ibs.gym8020.data.db.SchemaEntryEntity
import nl.ibs.gym8020.data.db.WeekPlanEntity
import nl.ibs.gym8020.data.db.WeekPlanSlotEntity

/** Inserts the generic standard content once (D-63). Everything is editable afterwards. */
class Seeder(private val db: AppDatabase, private val settings: SettingsStore, private val dutch: Boolean) {

    suspend fun seedIfNeeded() {
        if (!settings.current().seeded) {
            db.withTransaction {
                if (db.exercises().count() == 0) seed()
            }
            settings.update { it.copy(seeded = true) }
        }
        if (!settings.current().seededMaterials) {
            db.withTransaction { seedMaterials() }
            settings.update { it.copy(seededMaterials = true) }
        }
    }

    /** v0.2: standard material profiles; library exercises get their default material and movement icon. */
    private suspend fun seedMaterials() {
        val ids = HashMap<String, Long>()
        val existing = db.materials().all()
        for (m in DefaultLibrary.materials) {
            val found = existing.firstOrNull { it.libraryKey == m.key }
            ids[m.key] = found?.id ?: db.materials().insert(
                MaterialProfileEntity(
                    libraryKey = m.key, name = if (dutch) m.nameNl else m.nameEn, kind = m.profile.kind,
                    baseKg = m.profile.baseKg, loadingPoints = m.profile.loadingPoints,
                    minKg = m.profile.minKg, stepKg = m.profile.stepKg,
                ),
            )
        }
        val library = DefaultLibrary.exercises.associateBy { it.key }
        for (e in db.exercises().all()) {
            val lib = e.libraryKey?.let { library[it] } ?: continue
            db.exercises().update(
                e.copy(
                    materialProfileId = e.materialProfileId ?: DefaultLibrary.materialFor(lib)?.let { ids[it] },
                    movement = e.movement ?: DefaultLibrary.movementFor(lib.key),
                ),
            )
        }
    }

    private suspend fun seed() {
        val exerciseIds = HashMap<String, Long>()
        for (ex in DefaultLibrary.exercises) {
            val id = db.exercises().insert(
                ExerciseEntity(
                    libraryKey = ex.key,
                    name = ex.name,
                    equipment = ex.equipment,
                    loadType = ex.loadType,
                    progression = ex.progression,
                    bodyweightFactor = ex.bodyweightFactor,
                    healthConnectType = ExerciseCodeTable.of(ex.key)?.healthConnect,
                    garminCategory = ExerciseCodeTable.of(ex.key)?.garmin,
                ),
            )
            exerciseIds[ex.key] = id
            db.exercises().insertMuscles(ex.muscles.map { ExerciseMuscleEntity(id, it.muscle, it.percent) })
        }
        val schemaIds = HashMap<String, Long>()
        for (schema in DefaultLibrary.schemas) {
            val id = db.schemas().insert(SchemaEntity(libraryKey = schema.key, name = schema.name))
            schemaIds[schema.key] = id
            db.schemas().insertEntries(
                schema.exerciseKeys.mapIndexed { i, key -> SchemaEntryEntity(schemaId = id, exerciseId = exerciseIds.getValue(key), position = i) },
            )
        }
        for (plan in DefaultLibrary.weekPlans) {
            val id = db.weekPlans().insert(
                WeekPlanEntity(
                    libraryKey = plan.key,
                    name = plan.name,
                    targetPerWeek = plan.targetPerWeek,
                    active = plan.key == DefaultLibrary.DEFAULT_ACTIVE_WEEK_PLAN,
                ),
            )
            db.weekPlans().insertSlots(
                plan.slots.mapIndexed { i, (schemaKey, day) ->
                    WeekPlanSlotEntity(weekPlanId = id, schemaId = schemaIds.getValue(schemaKey), position = i, day = day?.value)
                },
            )
        }
    }
}
