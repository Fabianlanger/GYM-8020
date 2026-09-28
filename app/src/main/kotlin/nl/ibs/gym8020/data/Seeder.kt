package nl.ibs.gym8020.data

import androidx.room.withTransaction
import nl.ibs.gym8020.core.library.DefaultLibrary
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.ExerciseMuscleEntity
import nl.ibs.gym8020.data.db.SchemaEntity
import nl.ibs.gym8020.data.db.SchemaEntryEntity
import nl.ibs.gym8020.data.db.WeekPlanEntity
import nl.ibs.gym8020.data.db.WeekPlanSlotEntity

/** Inserts the generic standard content once (D-63). Everything is editable afterwards. */
class Seeder(private val db: AppDatabase, private val settings: SettingsStore) {

    suspend fun seedIfNeeded() {
        if (settings.current().seeded) return
        db.withTransaction {
            if (db.exercises().count() == 0) seed()
        }
        settings.update { it.copy(seeded = true) }
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
