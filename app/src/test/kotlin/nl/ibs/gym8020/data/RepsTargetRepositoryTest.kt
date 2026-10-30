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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * #69: with reps progression you set your target in reps per set. What you set is yours: stored right away,
 * it survives leaving the training screen, moving on and corrections. Without one, the reps of today are the
 * target. The next session aims at it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepsTargetRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private var exerciseId = 0L

    /** A fresh repository is what you get after leaving the training screen and coming back. */
    private fun repository() = TrainingRepository(db, SettingsStore(context), PrefsTargetMarks(context), PrefsRepsTargets(context))

    private fun reps(heavy: Int?, light: Int?) = Target(listOf(PlannedSet(SetRole.HEAVY, null, heavy), PlannedSet(SetRole.LIGHT, null, light)))

    @Before
    fun setUp() = runBlocking {
        context.getSharedPreferences("target_marks", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("reps_targets", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        exerciseId = db.exercises().insert(
            ExerciseEntity(
                name = "Crunch", equipment = Equipment.BODYWEIGHT, loadType = LoadType.BODYWEIGHT,
                progression = Progression.REPS, bodyweightFactor = 0.3,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    /** A finished training with a heavy set of [heavy] reps and a light set of [light] reps; returns (session, se). */
    private suspend fun finishedSession(heavy: Int?, light: Int?, startedAt: Long = 1_000L): Pair<Long, Long> {
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Core", startedAt = startedAt, endedAt = startedAt + 1_000L))
        val seId = db.sessions().insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = 0))
        db.sessions().insertSets(
            listOf(
                SetEntity(sessionExerciseId = seId, position = 0, role = SetRole.HEAVY, reps = heavy),
                SetEntity(sessionExerciseId = seId, position = 1, role = SetRole.LIGHT, reps = light),
            ),
        )
        return sessionId to seId
    }

    @Test
    fun `a reps target you set survives leaving the training screen`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = reps(22, 40))

        assertEquals(reps(22, 40), repository().targetSetIn(seId = 1, exerciseId = exerciseId))
        assertEquals(reps(22, 40), repository().target(exerciseId))
    }

    @Test
    fun `moving on does not replace your reps target with the reps of today`() = runBlocking {
        repository().setTarget(seId = 1, exerciseId = exerciseId, target = reps(22, 40))

        repository().storeDefaultTarget(seId = 1, exerciseId = exerciseId, target = reps(20, 35))

        assertEquals(reps(22, 40), repository().target(exerciseId))
    }

    @Test
    fun `without a reps target of your own the default target is the reps of today`() = runBlocking {
        repository().storeDefaultTarget(seId = 9, exerciseId = exerciseId, target = reps(18, 30))
        val (sessionId, _) = finishedSession(heavy = 20, light = 35)

        assertEquals(reps(20, 35), repository().nextTargetIn(sessionId, db.exercises().get(exerciseId)!!))
        repository().applyCorrection(sessionId, listOf(exerciseId))

        assertEquals(reps(20, 35), repository().target(exerciseId))
    }

    @Test
    fun `a set you did not do keeps its previous reps target`() = runBlocking {
        repository().storeDefaultTarget(seId = 9, exerciseId = exerciseId, target = reps(18, 30))
        val (sessionId, _) = finishedSession(heavy = 20, light = null)

        repository().applyCorrection(sessionId, listOf(exerciseId))

        assertEquals(reps(20, 30), repository().target(exerciseId))
    }

    @Test
    fun `correcting your latest training keeps the reps target you set yourself`() = runBlocking {
        val (sessionId, seId) = finishedSession(heavy = 20, light = 35)
        repository().setTarget(seId = seId, exerciseId = exerciseId, target = reps(25, 45))

        repository().applyCorrection(sessionId, listOf(exerciseId))

        assertEquals(reps(25, 45), repository().target(exerciseId))
        assertEquals(reps(25, 45), repository().nextTargetIn(sessionId, db.exercises().get(exerciseId)!!))
    }

    @Test
    fun `the next training aims at your reps target, also after you change it again`() = runBlocking {
        repository().storeDefaultTarget(seId = 99, exerciseId = exerciseId, target = reps(22, 40))
        val sessionId = db.sessions().insert(SessionEntity(schemaName = "Core", startedAt = 5_000L))

        repository().addExercise(sessionId, exerciseId)
        val seId = db.sessions().exercises(sessionId).single().id
        repository().setTarget(seId = seId, exerciseId = exerciseId, target = reps(25, 45))

        val sets = db.sessions().setsOf(seId)
        assertEquals(listOf(22, 40), sets.map { repository().sessionRepsGoal(it.id) })
    }

    @Test
    fun `a target without reps comes back without reps`() = runBlocking {
        val target = Target(listOf(PlannedSet(SetRole.HEAVY, 10.0), PlannedSet(SetRole.LIGHT, 0.0)))

        repository().setTarget(seId = 1, exerciseId = exerciseId, target = target)

        assertEquals(target, repository().target(exerciseId))
    }
}
