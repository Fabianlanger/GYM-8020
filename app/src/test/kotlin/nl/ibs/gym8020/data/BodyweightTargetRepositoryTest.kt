package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.model.TargetOrigin
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.BodyweightEntity
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import nl.ibs.gym8020.data.db.PlanEntity
import nl.ibs.gym8020.data.db.PlanSetEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.TargetEntity
import nl.ibs.gym8020.data.db.TargetSetEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * #87, ADR 0002: the target of a bodyweight exercise is kept as an effective weight, so a change of your body weight
 * does not change how heavy you train. A training reads and sets it as the signed load of its own day; the next
 * training aims at what you hang that day, rounded to the material.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BodyweightTargetRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val settings get() = SettingsStore(context)
    private var pullUp = 0L

    private fun repository() = TrainingRepository(db, settings)

    private fun target(heavy: Double, light: Double) = Target(listOf(PlannedSet(SetRole.HEAVY, heavy), PlannedSet(SetRole.LIGHT, light)))

    @Before
    fun setUp() = runBlocking<Unit> {
        // The app runs its own start steps; let them finish first. The settings store lives for the whole test process.
        (context as GymApp).seeding.join()
        settings.update { AppSettings(bodyweightTargetsEffective = true) }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val assisted = db.materials().insert(MaterialProfileEntity(name = "Assist / belt", kind = MaterialKind.BODYWEIGHT, stepKg = 5.0))
        pullUp = db.exercises().insert(
            ExerciseEntity(
                name = "Pull-up", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT, progression = Progression.WEIGHT,
                bodyweightFactor = 1.0, materialProfileId = assisted,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    /** A training at [bodyweightKg] with the pull-up in it, without sets; returns (session, session exercise). */
    private suspend fun training(bodyweightKg: Double?, startedAt: Long, exerciseId: Long = pullUp): Pair<Long, Long> {
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Pull", startedAt = startedAt, bodyweightKg = bodyweightKg))
        return sessionId to db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = 0))
    }

    @Test
    fun `a target set at 80 kg body weight is kept as an effective weight and read back as you set it`() = runBlocking<Unit> {
        val (_, se) = training(80.0, startedAt = 1_000)

        repository().setTarget(se, pullUp, target(0.0, -20.0))

        assertEquals(listOf(80.0, 60.0), db.plans().sets(pullUp).map { it.weightKg })
        assertEquals(listOf(80.0, 60.0), db.targets().sets(se).map { it.weightKg })
        assertEquals(target(0.0, -20.0), repository().targetSetIn(se, pullUp))
    }

    @Test
    fun `5 kg lighter at the next training - you aim at hanging 5 kg more, rounded to the material`() = runBlocking<Unit> {
        val (_, first) = training(80.0, startedAt = 1_000)
        repository().setTarget(first, pullUp, target(0.0, -20.0))
        val (next, _) = training(75.0, startedAt = 100_000)

        repository().addExercise(next, pullUp)

        val se = db.sessions().exercises(next).last()
        assertEquals(listOf(5.0, -15.0), db.sessions().setsOf(se.id).map { it.goalWeightKg })
        assertEquals(target(5.0, -15.0), repository().target(pullUp, next))
    }

    @Test
    fun `without material the load of today is rounded to the weight step`() = runBlocking<Unit> {
        val dips = db.exercises().insert(
            ExerciseEntity(name = "Dip", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT, progression = Progression.WEIGHT),
        )
        val (_, first) = training(80.0, startedAt = 1_000, exerciseId = dips)
        repository().setTarget(first, dips, target(1.25, -20.0))
        val (next, _) = training(75.3, startedAt = 100_000, exerciseId = dips)

        repository().addExercise(next, dips)

        // 81,25 − 75,3 = 5,95 → 5; 60 − 75,3 = −15,3 → −15.
        val se = db.sessions().exercises(next).last()
        assertEquals(listOf(5.0, -15.0), db.sessions().setsOf(se.id).map { it.goalWeightKg })
    }

    @Test
    fun `the default target of a lighter day is kept in effective weight`() = runBlocking<Unit> {
        val (_, se) = training(75.0, startedAt = 1_000)

        repository().storeDefaultTarget(se, pullUp, target(7.5, -15.0))

        assertEquals(listOf(82.5, 60.0), db.plans().sets(pullUp).map { it.weightKg })
        assertEquals(TargetOrigin.DEFAULT, db.targets().get(se)?.origin)
    }

    @Test
    fun `undoing brings back the target as it was, in effective weight`() = runBlocking<Unit> {
        val (_, first) = training(80.0, startedAt = 1_000)
        repository().setTarget(first, pullUp, target(0.0, -20.0))
        val (next, se) = training(75.0, startedAt = 100_000)
        val before = repository().target(pullUp, next)!!
        repository().setTarget(se, pullUp, target(10.0, -10.0))

        repository().withdrawTarget(se, pullUp, before)

        assertEquals(listOf(80.0, 60.0), db.plans().sets(pullUp).map { it.weightKg })
    }

    @Test
    fun `the app step converts signed targets once, each with the body weight of its training`() = runBlocking<Unit> {
        settings.update { it.copy(bodyweightTargetsEffective = false) }
        val (_, older) = training(80.0, startedAt = 1_000)
        val (_, latest) = training(76.0, startedAt = 100_000)
        db.targets().replace(TargetEntity(older, TargetOrigin.YOURS, setAt = 2_000), listOf(TargetSetEntity(sessionExerciseId = older, position = 0, role = SetRole.HEAVY, weightKg = 5.0)))
        db.targets().replace(TargetEntity(latest, TargetOrigin.DEFAULT, setAt = 101_000), listOf(TargetSetEntity(sessionExerciseId = latest, position = 0, role = SetRole.HEAVY, weightKg = 2.5)))
        db.plans().replace(PlanEntity(pullUp, updatedAt = 101_000), listOf(PlanSetEntity(exerciseId = pullUp, position = 0, role = SetRole.HEAVY, weightKg = 2.5)))

        assertTrue(BodyweightTargets(db, settings).convertIfNeeded())

        assertEquals(listOf(85.0), db.targets().sets(older).map { it.weightKg })
        assertEquals(listOf(78.5), db.targets().sets(latest).map { it.weightKg })
        // The current target with the body weight of the training that stored it last.
        assertEquals(listOf(78.5), db.plans().sets(pullUp).map { it.weightKg })
        assertTrue(settings.current().bodyweightTargetsEffective)

        // Once: a second start changes nothing.
        assertFalse(BodyweightTargets(db, settings).convertIfNeeded())
        assertEquals(listOf(85.0), db.targets().sets(older).map { it.weightKg })
        assertEquals(listOf(78.5), db.plans().sets(pullUp).map { it.weightKg })
    }

    @Test
    fun `without any known body weight the step waits and the targets stay signed loads`() = runBlocking<Unit> {
        settings.update { it.copy(bodyweightTargetsEffective = false) }
        val (_, se) = training(null, startedAt = 1_000)
        repository().setTarget(se, pullUp, target(5.0, -20.0))

        assertFalse(BodyweightTargets(db, settings).convertIfNeeded())

        assertFalse(settings.current().bodyweightTargetsEffective)
        assertEquals(listOf(5.0, -20.0), db.plans().sets(pullUp).map { it.weightKg })

        // Your first measurement: now the step converts, with that body weight.
        db.bodyweights().insert(BodyweightEntity(epochDay = 0, kg = 82.0, measuredAt = 1_000))
        assertTrue(BodyweightTargets(db, settings).convertIfNeeded())
        assertEquals(listOf(87.0, 62.0), db.plans().sets(pullUp).map { it.weightKg })
        assertEquals(target(5.0, -20.0), repository().targetOf(se))
    }

    @Test
    fun `export then import keeps effective targets as they are, an older backup is converted`() = runBlocking<Unit> {
        val (_, se) = training(80.0, startedAt = 1_000)
        repository().setTarget(se, pullUp, target(5.0, -20.0))
        val service = BackupService(db, settings)
        val text = service.export()

        service.import(text)

        assertEquals(listOf(85.0, 60.0), db.plans().all().flatMap { db.plans().sets(it.exerciseId) }.map { it.weightKg })

        // The same file as an app before #87 wrote it: signed loads.
        service.import(text.replace("\"bodyweightTargetsEffective\": true", "\"bodyweightTargetsEffective\": false"))

        assertTrue(settings.current().bodyweightTargetsEffective)
        assertEquals(listOf(165.0, 140.0), db.plans().all().flatMap { db.plans().sets(it.exerciseId) }.map { it.weightKg })
    }
}
