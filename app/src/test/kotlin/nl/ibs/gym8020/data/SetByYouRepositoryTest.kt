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
import nl.ibs.gym8020.core.model.TargetOrigin
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * #92 (decision #90): "zelf gezet" per set, and the session target follows the role. The two situations of the
 * prototype, through the repository (schema v4). Synthetic data.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SetByYouRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private var squat = 0L

    /** A fresh repository is what you get after leaving the training screen and coming back. */
    private fun repository() = TrainingRepository(db, SettingsStore(context))

    private fun target(heavy: Double, light: Double) = Target(listOf(PlannedSet(SetRole.HEAVY, heavy), PlannedSet(SetRole.LIGHT, light)))

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        squat = db.exercises().insert(
            ExerciseEntity(name = "Back squat", equipment = Equipment.BARBELL, loadType = LoadType.EXTERNAL, progression = Progression.WEIGHT),
        )
    }

    @After
    fun tearDown() = db.close()

    /** A training of the squat that started at [startedAt]; returns (session, session exercise). */
    private suspend fun training(startedAt: Long): Pair<Long, Long> {
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Legs", startedAt = startedAt))
        return sessionId to db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = squat, position = 0))
    }

    @Test
    fun `prototype situation 1 - the heavy advice you took stays, the light set follows your performance`() = runBlocking {
        val (_, se) = training(5_000L)
        // Heavy 75 × 9: you take over the advice 77,5 for the heavy set only.
        repository().setTarget(se, squat, target(77.5, 40.0), yours = listOf(true, false))

        // Light 30 × 20, then moving on: the default target is H 75 · L 30.
        repository().storeDefaultTarget(se, squat, target(75.0, 30.0))

        assertEquals(target(77.5, 30.0), repository().target(squat))
        assertEquals(target(77.5, 30.0), repository().targetSetIn(se, squat))
        assertEquals(listOf(true, false), repository().yoursIn(se))
        assertEquals(listOf(TargetOrigin.YOURS, TargetOrigin.DEFAULT), db.targets().sets(se).map { it.origin })
        // The target as a whole counts as yours: at least one set is.
        assertEquals(TargetOrigin.YOURS, db.targets().get(se)?.origin)
    }

    @Test
    fun `a target you set as a whole stays when moving on`() = runBlocking {
        val (_, se) = training(5_000L)
        repository().setTarget(se, squat, target(80.0, 40.0))

        repository().storeDefaultTarget(se, squat, target(75.0, 30.0))

        assertEquals(target(80.0, 40.0), repository().target(squat))
        assertEquals(listOf(true, true), repository().yoursIn(se))
    }

    @Test
    fun `undoing the only set you set leaves no target of yours`() = runBlocking {
        val (_, se) = training(5_000L)
        repository().setTarget(se, squat, target(77.5, 40.0), yours = listOf(true, false))

        repository().setTarget(se, squat, target(75.0, 40.0), yours = listOf(false, false))

        assertNull(repository().targetSetIn(se, squat))
        assertEquals(TargetOrigin.DEFAULT, db.targets().get(se)?.origin)
        repository().storeDefaultTarget(se, squat, target(75.0, 30.0))
        assertEquals(target(75.0, 30.0), repository().target(squat))
    }

    @Test
    fun `prototype situation 2 - add a set and switch roles, the session target follows the role`() = runBlocking {
        // Last time you kept H 70 · L 40 as your target; today's training starts from it.
        val (_, earlier) = training(1_000L)
        repository().storeDefaultTarget(earlier, squat, target(70.0, 40.0))
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Legs", startedAt = 5_000L))
        repository().addExercise(sessionId, squat)
        val se = db.sessions().exercises(sessionId).single().id
        suspend fun goals() = db.sessions().setsOf(se).map { it.goalWeightKg }
        assertEquals(listOf(70.0, 40.0), goals())

        // ＋ Set: a heavy set at the end, for which there was no target.
        repository().addSet(se, SetRole.HEAVY, followRole = true)
        assertEquals(listOf(70.0, 40.0, null), goals())

        // The second set becomes heavy, the third light: heavy · heavy · light.
        repository().switchRole(db.sessions().setsOf(se)[1], SetRole.HEAVY)
        assertEquals(listOf(70.0, null, null), goals())
        repository().switchRole(db.sessions().setsOf(se)[2], SetRole.LIGHT)
        assertEquals(listOf(SetRole.HEAVY, SetRole.HEAVY, SetRole.LIGHT), db.sessions().setsOf(se).map { it.role })
        assertEquals(listOf(70.0, null, 40.0), goals())
    }

    @Test
    fun `the session target follows the target the training started with, not a target you set during it`() = runBlocking {
        val (_, earlier) = training(1_000L)
        repository().storeDefaultTarget(earlier, squat, target(70.0, 40.0))
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Legs", startedAt = 5_000L))
        repository().addExercise(sessionId, squat)
        val se = db.sessions().exercises(sessionId).single().id
        // During the training you set your target for next time.
        repository().setTarget(se, squat, target(80.0, 45.0))

        repository().addSet(se, SetRole.LIGHT, followRole = true)
        repository().switchRole(db.sessions().setsOf(se)[1], SetRole.HEAVY)

        assertEquals(listOf(70.0, null, 40.0), db.sessions().setsOf(se).map { it.goalWeightKg })
    }

    @Test
    fun `correcting afterwards, a new set gets no session target`() = runBlocking {
        val (_, earlier) = training(1_000L)
        repository().storeDefaultTarget(earlier, squat, target(70.0, 40.0))
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Legs", startedAt = 5_000L))
        repository().addExercise(sessionId, squat)
        val se = db.sessions().exercises(sessionId).single().id

        repository().addSet(se, SetRole.LIGHT)

        assertEquals(listOf(70.0, 40.0, null), db.sessions().setsOf(se).map { it.goalWeightKg })
    }
}
