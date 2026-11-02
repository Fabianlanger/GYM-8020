package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.core.backup.BackupJson
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MeasurementOrigin
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SessionOrigin
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.TargetOrigin
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.GymEntity
import nl.ibs.gym8020.data.db.PlanEntity
import nl.ibs.gym8020.data.db.PlanSetEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import nl.ibs.gym8020.data.db.PreMigrationCopy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Schema v3 (#11): what the app moves after opening the migrated database, the backup format v3 with older backups
 * still readable (A-7), and the safety copy before the migration (Q4). All data is synthetic.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class V3UpgradeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val settings get() = SettingsStore(context)
    private var crunch = 0L

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        // The settings store lives for the whole test process: start from the state before the move.
        settings.update { AppSettings(plateStock = PlateStock(mapOf(20.0 to 4, 5.0 to 2))) }
        crunch = db.exercises().insert(
            ExerciseEntity(
                libraryKey = "crunch", name = "Crunch", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT,
                progression = Progression.REPS, bodyweightFactor = 0.3,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun upgrade() = V3Upgrade(context, db, settings, Gyms(db, settings), dutch = true)

    /** A finished training of the crunch with two work sets; returns (session exercise, set ids). */
    private suspend fun training(startedAt: Long, goalReps: List<Int?> = listOf(null, null), gymId: Long? = null): Pair<Long, List<Long>> {
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Core", startedAt = startedAt, endedAt = startedAt + 1_000, gymId = gymId))
        val seId = db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = crunch, position = 0))
        val ids = listOf(SetRole.HEAVY, SetRole.LIGHT).mapIndexed { i, role ->
            db.sessions().insertSet(SetEntity(sessionExerciseId = seId, position = i, role = role, reps = 20 + i, goalReps = goalReps[i]))
        }
        return seId to ids
    }

    @Test
    fun `the interim stores move into the database once, then their files are gone`() = runBlocking {
        val (older, _) = training(1_000)
        val (latest, latestSets) = training(5_000)
        db.plans().replace(
            PlanEntity(exerciseId = crunch, note = "slow", updatedAt = 0),
            listOf(PlanSetEntity(exerciseId = crunch, position = 0, role = SetRole.HEAVY), PlanSetEntity(exerciseId = crunch, position = 1, role = SetRole.LIGHT)),
        )
        context.getSharedPreferences(InterimStores.MARKS, Context.MODE_PRIVATE).edit().putBoolean("$latest", true).commit()
        context.getSharedPreferences(InterimStores.REPS, Context.MODE_PRIVATE).edit()
            .putString("target_$crunch", "22,40").putInt("goal_${latestSets[0]}", 21).putInt("goal_${latestSets[1]}", 38).commit()

        upgrade().run()

        // Reps targets and session targets in reps are in the database.
        assertEquals(listOf(22, 40), db.plans().sets(crunch).map { it.reps })
        assertEquals(listOf(21, 38), db.sessions().setsOf(latest).map { it.goalReps })
        // "Set by you" is now the origin of the kept target.
        val repo = TrainingRepository(db, settings)
        assertEquals(Target(listOf(PlannedSet(SetRole.HEAVY, null, 22), PlannedSet(SetRole.LIGHT, null, 40)), "slow"), repo.targetSetIn(latest, crunch))
        // The older training: the session target of the next one, origin unknown.
        assertEquals(TargetOrigin.UNKNOWN, db.targets().get(older)?.origin)
        assertEquals(Target(listOf(PlannedSet(SetRole.HEAVY, null, 21), PlannedSet(SetRole.LIGHT, null, 38))), repo.targetOf(older))
        assertNull(repo.targetSetIn(older, crunch))
        // Codes for the library exercise.
        assertEquals("CRUNCH", db.exercises().get(crunch)!!.healthConnectType)
        // Done once; the stores are gone.
        assertTrue(settings.current().v3DataMoved)
        assertTrue(context.getSharedPreferences(InterimStores.MARKS, Context.MODE_PRIVATE).all.isEmpty())
        assertTrue(context.getSharedPreferences(InterimStores.REPS, Context.MODE_PRIVATE).all.isEmpty())

        // A second start does not move again.
        context.getSharedPreferences(InterimStores.REPS, Context.MODE_PRIVATE).edit().putString("target_$crunch", "1,1").commit()
        upgrade().run()
        assertEquals(listOf(22, 40), db.plans().sets(crunch).map { it.reps })
        context.getSharedPreferences(InterimStores.REPS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `the plate stock moves from the settings into the migrated gym`() = runBlocking {
        val gymId = db.gyms().insert(GymEntity(name = AppDatabase.MIGRATED_GYM_NAME, plates = null))
        training(1_000, gymId = gymId)

        upgrade().run()
        upgrade().run()

        val gym = db.gyms().all().single()
        assertEquals("Mijn gym", gym.name)
        assertEquals(PlateStock(mapOf(20.0 to 4, 5.0 to 2)), PlateStock.decode(gym.plates))
        // From now on the plate settings change your gym.
        Gyms(db, settings).updateStock { PlateStock(it.plates + (2.5 to 2)) }
        assertEquals(PlateStock(mapOf(20.0 to 4, 5.0 to 2, 2.5 to 2)), plateStockFor(gymId, db.gyms().all(), PlateStock.DEFAULT))
    }

    @Test
    fun `a fresh install gets a gym with the plates from the settings`() = runBlocking {
        upgrade().run()

        val gym = db.gyms().all().single()
        assertEquals("Mijn gym", gym.name)
        assertEquals(PlateStock(mapOf(20.0 to 4, 5.0 to 2)), PlateStock.decode(gym.plates))
    }

    /** A version 2 backup as the app wrote it before schema v3 (synthetic data). */
    private val v2 = """
        {"format":"gym-8020-backup","version":2,"exportedAt":1,
         "exercises":[{"id":1,"libraryKey":"crunch","name":"Crunch","equipment":"BODYWEIGHT","loadType":"BODYWEIGHT",
                       "progression":"REPS","bodyweightFactor":0.3}],
         "bodyweights":[{"date":20000,"kg":80.5}],
         "sessions":[
           {"id":5,"schemaName":"Core","startedAt":1000,"endedAt":2000,
            "exercises":[{"exerciseId":1,"sets":[{"role":"HEAVY","reps":20,"goalWeightKg":0.0}]}]},
           {"id":6,"schemaName":"Core","startedAt":5000,"endedAt":6000,
            "exercises":[{"exerciseId":1,"sets":[{"role":"HEAVY","reps":22,"goalWeightKg":2.5}]}]}],
         "plans":[{"exerciseId":1,"sets":[{"role":"HEAVY","weightKg":2.5,"reps":24}]}],
         "materials":[{"id":3,"name":"Cable","kind":"STACK","baseKg":0.0,"loadingPoints":1,"minKg":2.5,"stepKg":2.5,"maxKg":80.0}],
         "plateStock":"20.0:2,10.0:2"}
    """.trimIndent()

    @Test
    fun `a v2 backup imports with defaults, a gym and derived targets`() = runBlocking {
        BackupService(db, settings).import(v2)

        val gym = db.gyms().all().single()
        assertEquals(PlateStock(mapOf(20.0 to 2, 10.0 to 2)), PlateStock.decode(gym.plates))
        val sessions = db.sessions().all()
        assertEquals(listOf(gym.id, gym.id), sessions.map { it.gymId })
        assertEquals(listOf(SessionOrigin.LIVE, SessionOrigin.LIVE), sessions.map { it.origin })
        val bw = db.bodyweights().all().single()
        assertEquals(20000 * 86_400_000L + 43_200_000L, bw.measuredAt)
        assertEquals(MeasurementOrigin.MANUAL, bw.origin)
        val exId = db.exercises().all().single().id
        assertEquals(listOf(24), db.plans().sets(exId).map { it.reps })
        assertEquals("Cable", db.materials().all().single().name)
        // Older trainings get their target like the migration does.
        val (first, second) = sessions.map { s -> db.sessions().exercises(s.id).single().id }
        assertEquals(listOf(2.5), db.targets().sets(first).map { it.weightKg })
        assertEquals(TargetOrigin.UNKNOWN, db.targets().get(second)?.origin)
        assertEquals("CRUNCH", db.exercises().get(exId)!!.healthConnectType)
    }

    @Test
    fun `export then import gives the same v3 data back`() = runBlocking {
        val gymId = db.gyms().insert(GymEntity(name = "My gym", plates = "20.0:4"))
        val (seId, _) = training(1_000, goalReps = listOf(20, 35), gymId = gymId)
        db.sessions().updateSet(db.sessions().setsOf(seId).first().copy(repsInReserve = 2))
        TrainingRepository(db, settings).setTarget(seId, crunch, Target(listOf(PlannedSet(SetRole.HEAVY, null, 22)), "slow"))
        db.bodyweights().insert(manualMeasurement(80.0, now = 1_000_000))
        db.bodyweights().insert(manualMeasurement(80.4, now = 1_000_000 + 3_600_000))
        val service = BackupService(db, settings)
        val text = service.export()
        assertEquals(3, BackupJson.decode(text).version)

        service.import(text)

        val se = db.sessions().allExercises().single()
        assertEquals(listOf(20, 35), db.sessions().setsOf(se.id).map { it.goalReps })
        assertEquals(listOf(2, null), db.sessions().setsOf(se.id).map { it.repsInReserve })
        assertEquals(TargetOrigin.YOURS, db.targets().get(se.id)?.origin)
        assertEquals(Target(listOf(PlannedSet(SetRole.HEAVY, null, 22)), "slow"), TrainingRepository(db, settings).targetSetIn(se.id, crunch))
        assertEquals(listOf(80.0, 80.4), db.bodyweights().all().map { it.kg })
        assertEquals("20.0:4", db.gyms().all().single().plates)
        assertEquals(db.gyms().all().single().id, db.sessions().all().single().gymId)
    }

    // ---- Q4: the safety copy before the migration ----

    private fun fakeDatabase(file: File, version: Int) {
        val header = ByteArray(100)
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(header)
        header[60] = (version ushr 24).toByte()
        header[61] = (version ushr 16).toByte()
        header[62] = (version ushr 8).toByte()
        header[63] = version.toByte()
        file.writeBytes(header + ByteArray(924))
    }

    @Test
    fun `before the migration the database files are copied once`() {
        val dbFile = File(folder.root, "gym8020.db")
        fakeDatabase(dbFile, 2)
        File(dbFile.path + "-wal").writeText("wal")
        val target = File(folder.root, "pre-v3")

        assertTrue(PreMigrationCopy.copyIfNeeded(dbFile, target))
        assertEquals(dbFile.readBytes().toList(), File(target, "gym8020.db").readBytes().toList())
        assertEquals("wal", File(target, "gym8020.db-wal").readText())
        assertFalse(File(target, "gym8020.db-shm").exists())

        // Never again, never overwritten.
        File(dbFile.path + "-wal").writeText("newer")
        assertFalse(PreMigrationCopy.copyIfNeeded(dbFile, target))
        assertEquals("wal", File(target, "gym8020.db-wal").readText())
    }

    @Test
    fun `no copy for a new install or a database that is already version 3`() {
        val target = File(folder.root, "pre-v3")
        assertFalse(PreMigrationCopy.copyIfNeeded(File(folder.root, "missing.db"), target))
        val current = File(folder.root, "current.db")
        fakeDatabase(current, 3)
        assertFalse(PreMigrationCopy.copyIfNeeded(current, target))
        assertFalse(target.exists())
    }
}
