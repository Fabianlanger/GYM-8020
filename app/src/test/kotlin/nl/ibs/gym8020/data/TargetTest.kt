package nl.ibs.gym8020.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
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
 * B-6, D-97, #52: the target for next time is yours. What you set is stored right away, survives leaving the
 * training screen, and moving on or correcting the training never replaces it with the default target.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TargetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private var exerciseId = 0L

    /** A fresh repository is what you get after leaving the training screen and coming back. */
    private fun repository() = TrainingRepository(db, SettingsStore(context), PrefsTargetMarks(context))

    private fun heavy(kg: Double) = Target(listOf(PlannedSet(SetRole.HEAVY, kg), PlannedSet(SetRole.LIGHT, kg / 2)))

    @Before
    fun setUp() = runBlocking {
        context.getSharedPreferences("target_marks", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        exerciseId = db.exercises().insert(
            ExerciseEntity(name = "Cable row", equipment = Equipment.CABLE, loadType = LoadType.EXTERNAL, progression = Progression.WEIGHT),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a target you set survives leaving the training screen`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = heavy(80.0))

        assertEquals(heavy(80.0), repository().targetSetIn(seId = 1, exerciseId = exerciseId))
    }

    @Test
    fun `moving on does not replace your target with the default target`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = heavy(80.0))

        repository().storeDefaultTarget(seId = 1, exerciseId = exerciseId, target = heavy(82.5))

        assertEquals(heavy(80.0), repository().target(exerciseId))
    }

    @Test
    fun `without a target of your own the default target is stored when moving on`() = runBlocking {
        repository().storeDefaultTarget(seId = 2, exerciseId = exerciseId, target = heavy(82.5))

        assertEquals(heavy(82.5), repository().target(exerciseId))
        assertNull(repository().targetSetIn(seId = 2, exerciseId = exerciseId))
    }

    @Test
    fun `a target set in an earlier session does not count as set in this one`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = heavy(80.0))

        assertNull(repository().targetSetIn(seId = 3, exerciseId = exerciseId))
    }

    /** A finished training with one heavy set of [kg] × 6 and a light set you did not do; returns (session, se). */
    private suspend fun finishedSession(kg: Double, startedAt: Long = 1_000L): Pair<Long, Long> {
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Pull", startedAt = startedAt, endedAt = startedAt + 1_000L))
        val seId = db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = 0))
        db.sessions().insertSets(
            listOf(
                SetEntity(sessionExerciseId = seId, position = 0, role = SetRole.HEAVY, weightKg = kg, reps = 6),
                SetEntity(sessionExerciseId = seId, position = 1, role = SetRole.LIGHT),
            ),
        )
        return sessionId to seId
    }

    @Test
    fun `correcting a training stores the default target from what you lifted`() = runBlocking {
        repository().storeDefaultTarget(seId = 9, exerciseId = exerciseId, target = heavy(80.0))
        val (sessionId, _) = finishedSession(85.0)

        repository().recomputeTarget(db.exercises().get(exerciseId)!!, sessionId)

        assertEquals(Target(listOf(PlannedSet(SetRole.HEAVY, 85.0), PlannedSet(SetRole.LIGHT, 40.0))), repository().target(exerciseId))
    }

    @Test
    fun `correcting a training keeps the target you set in it`() = runBlocking {
        val (sessionId, seId) = finishedSession(85.0)
        repository().setTarget(seId = seId, exerciseId = exerciseId, target = heavy(80.0))

        repository().recomputeTarget(db.exercises().get(exerciseId)!!, sessionId)

        assertEquals(heavy(80.0), repository().target(exerciseId))
    }

    // ---- #64: the edit screen ----

    private fun corrected(kg: Double) = Target(listOf(PlannedSet(SetRole.HEAVY, kg), PlannedSet(SetRole.LIGHT, 40.0)))

    @Test
    fun `correcting your latest training lets the default target follow`() = runBlocking {
        repository().storeDefaultTarget(seId = 9, exerciseId = exerciseId, target = heavy(80.0))
        val (sessionId, _) = finishedSession(85.0)

        repository().applyCorrection(sessionId, listOf(exerciseId))

        assertEquals(corrected(85.0), repository().target(exerciseId))
    }

    @Test
    fun `correcting your latest training keeps the target you set yourself`() = runBlocking {
        val (sessionId, seId) = finishedSession(85.0)
        repository().setTarget(seId = seId, exerciseId = exerciseId, target = heavy(80.0))

        repository().applyCorrection(sessionId, listOf(exerciseId))

        assertEquals(heavy(80.0), repository().target(exerciseId))
    }

    @Test
    fun `a target you change in the edit screen is yours`() = runBlocking {
        repository().storeDefaultTarget(seId = 9, exerciseId = exerciseId, target = heavy(80.0))
        val (sessionId, seId) = finishedSession(85.0)
        val exercise = db.exercises().get(exerciseId)!!
        assertEquals(corrected(85.0), repository().nextTargetIn(sessionId, exercise))

        repository().setTarget(seId = seId, exerciseId = exerciseId, target = heavy(90.0))
        repository().applyCorrection(sessionId, listOf(exerciseId))

        assertEquals(heavy(90.0), repository().targetSetIn(seId = seId, exerciseId = exerciseId))
        assertEquals(heavy(90.0), repository().nextTargetIn(sessionId, exercise))
        assertEquals(heavy(90.0), repository().target(exerciseId))
    }

    @Test
    fun `correcting an older training does not change the target`() = runBlocking {
        repository().storeDefaultTarget(seId = 9, exerciseId = exerciseId, target = heavy(80.0))
        val (older, _) = finishedSession(70.0, startedAt = 1_000L)
        finishedSession(80.0, startedAt = 10_000L)

        repository().applyCorrection(older, listOf(exerciseId))

        assertEquals(heavy(80.0), repository().target(exerciseId))
        assertNull(repository().nextTargetIn(older, db.exercises().get(exerciseId)!!))
    }
}
