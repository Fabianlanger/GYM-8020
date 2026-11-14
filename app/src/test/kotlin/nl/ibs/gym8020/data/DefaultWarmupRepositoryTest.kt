package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.GymApp
import nl.ibs.gym8020.core.logic.DefaultWarmup
import nl.ibs.gym8020.core.logic.WarmupMeasure
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import nl.ibs.gym8020.data.db.SetEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * #93: the default warm-up is stored on the exercise and read back as you set it (reps for an exercise on reps, in
 * the same column); a warm-up set during a training starts from it, prefilled once from the material.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DefaultWarmupRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val settings get() = SettingsStore(context)
    private var squat = 0L
    private var pullUp = 0L
    private var crunch = 0L

    private fun repository() = TrainingRepository(db, settings)

    @Before
    fun setUp() = runBlocking<Unit> {
        // The app runs its own start steps; let them finish first. The settings store lives for the whole test process.
        (context as GymApp).seeding.join()
        settings.update { AppSettings() }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val bar = db.materials().insert(MaterialProfileEntity(name = "Bar 20", kind = MaterialKind.PLATES, baseKg = 20.0, loadingPoints = 2))
        val assisted = db.materials().insert(MaterialProfileEntity(name = "Assist / belt", kind = MaterialKind.BODYWEIGHT, stepKg = 5.0))
        squat = db.exercises().insert(
            ExerciseEntity(name = "Squat", equipment = Equipment.BARBELL, loadType = LoadType.EXTERNAL, progression = Progression.WEIGHT, materialProfileId = bar),
        )
        pullUp = db.exercises().insert(
            ExerciseEntity(
                name = "Pull-up", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT, progression = Progression.WEIGHT,
                bodyweightFactor = 1.0, materialProfileId = assisted,
            ),
        )
        crunch = db.exercises().insert(
            ExerciseEntity(name = "Crunch", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT, progression = Progression.REPS, bodyweightFactor = 0.3),
        )
    }

    @After
    fun tearDown() = db.close()

    /** A training at [bodyweightKg] with [exerciseId] in it, without sets; returns the session exercise. */
    private suspend fun training(exerciseId: Long, bodyweightKg: Double? = 80.0): Long {
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Test", startedAt = 1_000, bodyweightKg = bodyweightKg))
        return db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = 0))
    }

    private suspend fun warmupSet(seId: Long): SetEntity = db.sessions().setsOf(seId).single { it.role == SetRole.WARMUP }

    @Test
    fun `a default warm-up set at the exercise is read back as you set it`() = runBlocking<Unit> {
        repository().setDefaultWarmup(squat, 40.0, "  slow, full range ")

        assertEquals(DefaultWarmup(WarmupMeasure.WEIGHT, 40.0, "slow, full range"), repository().defaultWarmup(squat))
        val stored = db.exercises().get(squat)!!
        assertEquals(40.0, stored.defaultWarmupKg!!, 0.0)
        assertEquals("slow, full range", stored.defaultWarmupNote)
    }

    @Test
    fun `on reps the default is a number of reps, in the same column`() = runBlocking<Unit> {
        repository().setDefaultWarmup(crunch, 8.0, null)

        val warmup = repository().defaultWarmup(crunch)!!
        assertEquals(8, warmup.reps)
        assertNull(warmup.weightKg)
        assertEquals(8.0, db.exercises().get(crunch)!!.defaultWarmupKg!!, 0.0)
    }

    @Test
    fun `assistance is stored as a negative weight`() = runBlocking<Unit> {
        repository().setDefaultWarmup(pullUp, -30.0, null)

        assertEquals(-30.0, repository().defaultWarmup(pullUp)!!.weightKg!!, 0.0)
    }

    @Test
    fun `the first warm-up is prefilled once from the material - the empty bar - and then it is yours`() = runBlocking<Unit> {
        val first = training(squat)
        repository().addSet(first, SetRole.WARMUP, withDefaultWarmup = true)

        assertEquals(20.0, warmupSet(first).weightKg!!, 0.0)
        assertEquals(20.0, repository().defaultWarmup(squat)!!.value!!, 0.0)

        // "Bewaar 30 kg als standaard": the next warm-up starts from 30, the bar no longer applies.
        repository().setDefaultWarmup(squat, 20.0, "slow")
        repository().saveDefaultWarmupValue(squat, 30.0)
        val second = training(squat)
        repository().addSet(second, SetRole.WARMUP, withDefaultWarmup = true)

        assertEquals(30.0, warmupSet(second).weightKg!!, 0.0)
        assertEquals(DefaultWarmup(WarmupMeasure.WEIGHT, 30.0, "slow"), repository().defaultWarmup(squat))
    }

    @Test
    fun `pull-ups warm up with assistance - half the body weight by default, rounded to the machine`() = runBlocking<Unit> {
        val se = training(pullUp, bodyweightKg = 80.0)
        repository().addSet(se, SetRole.WARMUP, withDefaultWarmup = true)

        assertEquals(-40.0, warmupSet(se).weightKg!!, 0.0)
        assertEquals(-40.0, db.exercises().get(pullUp)!!.defaultWarmupKg!!, 0.0)
    }

    @Test
    fun `without a body weight there is no assistance to prefill - nothing is stored`() = runBlocking<Unit> {
        val se = training(pullUp, bodyweightKg = null)
        repository().addSet(se, SetRole.WARMUP, withDefaultWarmup = true)

        assertNull(warmupSet(se).weightKg)
        assertNull(db.exercises().get(pullUp)!!.defaultWarmupKg)
    }

    @Test
    fun `an exercise on reps warms up with mini-reps`() = runBlocking<Unit> {
        val se = training(crunch)
        repository().addSet(se, SetRole.WARMUP, withDefaultWarmup = true)

        val set = warmupSet(se)
        assertEquals(DefaultWarmup.MINI_REPS, set.reps)
        assertNull(set.weightKg)
    }

    @Test
    fun `tapping Warm-up adds the warm-up set filled in, or fills the empty one from your target, never one you filled in`() = runBlocking<Unit> {
        repository().setDefaultWarmup(squat, 30.0, null)
        val none = training(squat)
        repository().startWarmupSet(none)
        assertEquals(30.0, warmupSet(none).weightKg!!, 0.0)

        // A warm-up set from your target, still empty.
        val planned = training(squat)
        db.sessions().insertSet(SetEntity(sessionExerciseId = planned, position = 0, role = SetRole.WARMUP))
        repository().startWarmupSet(planned)
        assertEquals(30.0, warmupSet(planned).weightKg!!, 0.0)

        // Already filled in: it stays as it is.
        val filled = training(squat)
        db.sessions().insertSet(SetEntity(sessionExerciseId = filled, position = 0, role = SetRole.WARMUP, weightKg = 25.0))
        repository().startWarmupSet(filled)
        assertEquals(25.0, warmupSet(filled).weightKg!!, 0.0)
    }

    @Test
    fun `correcting a finished training adds an empty warm-up and stores no default`() = runBlocking<Unit> {
        val se = training(squat)
        repository().addSet(se, SetRole.WARMUP)

        assertNull(warmupSet(se).weightKg)
        assertNull(db.exercises().get(squat)!!.defaultWarmupKg)
    }

    @Test
    fun `switching to reps drops the value, which no longer means the same, and keeps the note`() = runBlocking<Unit> {
        repository().setDefaultWarmup(squat, 40.0, "slow")

        repository().setProgression(squat, Progression.REPS)

        val exercise = db.exercises().get(squat)!!
        assertNull(exercise.defaultWarmupKg)
        assertEquals("slow", exercise.defaultWarmupNote)
    }
}
