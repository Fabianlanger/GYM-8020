package nl.ibs.gym8020

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.model.MeasurementOrigin
import nl.ibs.gym8020.core.model.SessionOrigin
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.TargetOrigin
import nl.ibs.gym8020.data.InterimStores
import nl.ibs.gym8020.data.V3Upgrade
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.BodyweightEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * F-4: existing users keep their data when the database schema changes. Opening with Room runs the migrations and
 * validates the result against the current entities (columns, types, NOT NULL, indices, foreign keys).
 * All data here is synthetic.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private fun open(): AppDatabase =
        Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

    private val day = 86_400_000L

    @Test
    fun migrate1To3KeepsData() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO exercises (id, libraryKey, name, equipment, loadType, progression, bodyweightFactor, archived) " +
                    "VALUES (7, 'back_squat', 'Back squat', 'BARBELL', 'EXTERNAL', 'WEIGHT', 1.0, 0)",
            )
            execSQL("INSERT INTO bodyweights (id, epochDay, kg) VALUES (1, 20000, 80.5)")
            close()
        }
        val db = open()
        runBlocking {
            val ex = db.exercises().get(7)!!
            assertEquals("Back squat", ex.name)
            assertNull(ex.materialProfileId)
            assertNull(ex.movement)
            val bw = db.bodyweights().all().single()
            assertEquals(80.5, bw.kg, 0.0)
            assertEquals(20000 * day + day / 2, bw.measuredAt)
            assertEquals(0, db.materials().count())
        }
        db.close()
    }

    /** A version 2 database as the app had it before schema v3: two trainings of the squat, a plan, material. */
    private fun createVersion2() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO material_profiles (id, libraryKey, name, kind, baseKg, loadingPoints, minKg, stepKg, maxKg, archived) " +
                    "VALUES (3, 'cable', 'Cable', 'STACK', 0.0, 1, 2.5, 2.5, 80.0, 0)",
            )
            execSQL(
                "INSERT INTO exercises (id, libraryKey, name, equipment, loadType, progression, bodyweightFactor, heavyTarget, " +
                    "lightTarget, archived, materialProfileId, movement) " +
                    "VALUES (7, 'back_squat', 'Back squat', 'BARBELL', 'EXTERNAL', 'WEIGHT', 1.0, 5, NULL, 0, 3, 'SQUAT')",
            )
            execSQL(
                "INSERT INTO exercises (id, libraryKey, name, equipment, loadType, progression, bodyweightFactor, archived) " +
                    "VALUES (8, NULL, 'Own crunch', 'BODYWEIGHT', 'BODYWEIGHT', 'REPS', 0.3, 0)",
            )
            execSQL("INSERT INTO exercise_muscles (exerciseId, muscle, percent) VALUES (7, 'QUADS', 100)")
            execSQL("INSERT INTO schemas (id, libraryKey, name, archived) VALUES (4, NULL, 'Legs', 0)")
            execSQL("INSERT INTO schema_entries (id, schemaId, exerciseId, position, setCount) VALUES (1, 4, 7, 0, NULL)")
            execSQL("INSERT INTO bodyweights (id, epochDay, kg) VALUES (1, 20000, 80.5)")
            execSQL("INSERT INTO bodyweights (id, epochDay, kg) VALUES (2, 20010, 81.0)")
            execSQL("INSERT INTO sessions (id, schemaId, schemaName, startedAt, endedAt, bodyweightKg) VALUES (1, 4, 'Legs', 1000, 2000, 80.5)")
            execSQL("INSERT INTO sessions (id, schemaId, schemaName, startedAt, endedAt, bodyweightKg) VALUES (2, 4, 'Legs', 5000, 6000, 81.0)")
            execSQL(
                "INSERT INTO session_exercises (id, sessionId, exerciseId, position, shownAt, warmupAt, goAt, firstInputAt, doneAt, skipped) " +
                    "VALUES (11, 1, 7, 0, 1100, 1200, 1300, 1400, 1500, 0)",
            )
            execSQL(
                "INSERT INTO session_exercises (id, sessionId, exerciseId, position, shownAt, warmupAt, goAt, firstInputAt, doneAt, skipped) " +
                    "VALUES (12, 2, 7, 0, NULL, NULL, NULL, NULL, NULL, 0)",
            )
            execSQL("INSERT INTO sets VALUES (101, 11, 0, 'HEAVY', 100.0, 6, NULL, 97.5)")
            execSQL("INSERT INTO sets VALUES (102, 11, 1, 'LIGHT', 80.0, 15, NULL, 77.5)")
            execSQL("INSERT INTO sets VALUES (103, 12, 0, 'HEAVY', 102.5, 5, NULL, 102.5)")
            execSQL("INSERT INTO sets VALUES (104, 12, 1, 'LIGHT', 82.5, 14, NULL, 82.5)")
            execSQL("INSERT INTO plans (exerciseId, note, updatedAt) VALUES (7, 'deeper', 9000)")
            execSQL("INSERT INTO plan_sets (id, exerciseId, position, role, weightKg) VALUES (201, 7, 0, 'HEAVY', 105.0)")
            execSQL("INSERT INTO plan_sets (id, exerciseId, position, role, weightKg) VALUES (202, 7, 1, 'LIGHT', 85.0)")
            close()
        }
    }

    @Test
    fun migrate2To3KeepsEveryRow() {
        createVersion2()
        val db = open()
        runBlocking {
            // Exercises: every column kept, the new ones empty.
            val squat = db.exercises().get(7)!!
            assertEquals("Back squat", squat.name)
            assertEquals(5, squat.heavyTarget)
            assertEquals(3L, squat.materialProfileId)
            assertEquals("SQUAT", squat.movement?.name)
            assertNull(squat.defaultWarmupKg)
            assertNull(squat.defaultWarmupNote)
            assertNull(squat.healthConnectType)
            assertNull(squat.garminCategory)
            assertEquals("Own crunch", db.exercises().get(8)!!.name)
            assertEquals(100, db.exercises().muscles(7).single().percent)
            assertEquals(7L, db.schemas().entries(4).single().exerciseId)

            // Material profile without maxKg, the rest kept.
            val cable = db.materials().get(3)!!
            assertEquals("Cable", cable.name)
            assertEquals(MaterialKind.STACK, cable.kind)
            assertEquals(2.5, cable.minKg, 0.0)
            assertEquals(2.5, cable.stepKg, 0.0)
            assertEquals(1, cable.loadingPoints)

            // Body weight: moment at noon of the day, origin manual.
            val weights = db.bodyweights().all()
            assertEquals(listOf(80.5, 81.0), weights.map { it.kg })
            assertEquals(listOf(20000 * day + day / 2, 20010 * day + day / 2), weights.map { it.measuredAt })
            assertEquals(listOf(MeasurementOrigin.MANUAL, MeasurementOrigin.MANUAL), weights.map { it.origin })

            // One gym; every training is live and in it.
            val gym = db.gyms().all().single()
            assertEquals(AppDatabase.FIRST_GYM_ID, gym.id)
            assertNull(gym.plates)
            val sessions = db.sessions().all()
            assertEquals(listOf(1000L, 5000L), sessions.map { it.startedAt })
            assertEquals(listOf(2000L, 6000L), sessions.map { it.endedAt })
            assertEquals(listOf(80.5, 81.0), sessions.map { it.bodyweightKg })
            assertEquals(listOf(SessionOrigin.LIVE, SessionOrigin.LIVE), sessions.map { it.origin })
            assertEquals(listOf(gym.id, gym.id), sessions.map { it.gymId })

            // Session exercises and sets.
            val se = db.sessions().exercise(11)!!
            assertEquals(listOf(1100L, 1200L, 1300L, 1400L, 1500L), listOf(se.shownAt, se.warmupAt, se.goAt, se.firstInputAt, se.doneAt))
            assertNull(se.materialProfileId)
            val sets = db.sessions().allSets().sortedBy { it.id }
            assertEquals(listOf(101L, 102L, 103L, 104L), sets.map { it.id })
            assertEquals(listOf(100.0, 80.0, 102.5, 82.5), sets.map { it.weightKg })
            assertEquals(listOf(6, 15, 5, 14), sets.map { it.reps })
            assertEquals(listOf(97.5, 77.5, 102.5, 82.5), sets.map { it.goalWeightKg })
            assertEquals(listOf(null, null, null, null), sets.map { it.goalReps })
            assertEquals(listOf(null, null, null, null), sets.map { it.repsInReserve })

            // The current target, with an empty reps column.
            assertEquals("deeper", db.plans().get(7)!!.note)
            val plan = db.plans().sets(7)
            assertEquals(listOf(105.0, 85.0), plan.map { it.weightKg })
            assertEquals(listOf(SetRole.HEAVY, SetRole.LIGHT), plan.map { it.role })
            assertEquals(listOf(null, null), plan.map { it.reps })

            // No target per training yet: that is derived after opening (V3Upgrade).
            assertEquals(emptyList<Any>(), db.targets().all())
        }
        db.close()
    }

    @Test
    fun migrate2To3AllowsTheNewData() {
        createVersion2()
        val db = open()
        runBlocking {
            // Several measurements per day; the last one counts.
            db.bodyweights().insert(BodyweightEntity(epochDay = 20010, kg = 81.6, measuredAt = 20010 * day + day * 3 / 4))
            assertEquals(81.6, db.bodyweights().latestOnOrBefore(20010)!!.kg, 0.0)
            assertEquals(80.5, db.bodyweights().latestOnOrBefore(20005)!!.kg, 0.0)

            // Material per session: deleting the profile empties it (SET NULL), the training stays.
            db.sessions().updateExercise(db.sessions().exercise(11)!!.copy(materialProfileId = 3))
            assertEquals(3L, db.sessions().exercise(11)!!.materialProfileId)
            db.materials().delete(3)
            assertNull(db.sessions().exercise(11)!!.materialProfileId)
            assertNull(db.exercises().get(7)!!.materialProfileId)
        }
        db.close()
    }

    @Test
    fun migrate2To3ThenTheAppMovesAndDerivesTargets() {
        createVersion2()
        val db = open()
        runBlocking {
            // What the interim stores held: you set the target in the latest training, with reps.
            val stores = InterimStores(setByYou = setOf(12L), planReps = mapOf(7L to listOf(5, 14)), goalReps = mapOf(103L to 5))

            V3Upgrade.moveInto(db, stores)

            assertEquals(listOf(5, 14), db.plans().sets(7).map { it.reps })
            assertEquals(5, db.sessions().allSets().single { it.id == 103L }.goalReps)
            // The older training: the session target of the next one, origin unknown, no time.
            val older = db.targets().get(11)!!
            assertEquals(TargetOrigin.UNKNOWN, older.origin)
            assertNull(older.setAt)
            assertEquals(listOf(102.5, 82.5), db.targets().sets(11).map { it.weightKg })
            assertEquals(listOf(5, null), db.targets().sets(11).map { it.reps })
            // The latest training: the current target, set by you.
            val latest = db.targets().get(12)!!
            assertEquals(TargetOrigin.YOURS, latest.origin)
            assertEquals("deeper", latest.note)
            assertEquals(listOf(105.0, 85.0), db.targets().sets(12).map { it.weightKg })
            assertEquals(listOf(5, 14), db.targets().sets(12).map { it.reps })
            // Library exercises got their codes; own exercises stay empty.
            assertEquals("SQUAT", db.exercises().get(7)!!.healthConnectType)
            assertEquals("squat", db.exercises().get(7)!!.garminCategory)
            assertNull(db.exercises().get(8)!!.healthConnectType)

            // Running it again changes nothing.
            V3Upgrade.moveInto(db, stores)
            assertEquals(2, db.targets().all().size)
            assertEquals(4, db.targets().allSets().size)
        }
        db.close()
    }
}
