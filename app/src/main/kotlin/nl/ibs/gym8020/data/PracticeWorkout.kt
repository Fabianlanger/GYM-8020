package nl.ibs.gym8020.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.ibs.gym8020.AppContainer
import nl.ibs.gym8020.R
import nl.ibs.gym8020.core.library.DefaultLibrary
import nl.ibs.gym8020.core.logic.DemoProgress
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.ExerciseMuscleEntity
import nl.ibs.gym8020.data.db.GymEntity
import nl.ibs.gym8020.data.db.MaterialProfileEntity
import nl.ibs.gym8020.data.db.PlanEntity
import nl.ibs.gym8020.data.db.PlanSetEntity
import nl.ibs.gym8020.data.db.SessionEntity

/**
 * The practice workout (#97): one exercise (back squat) with exactly what the tour shows. It lives in the practice
 * database in memory ([AppContainer.practice]); your own data is only read (the bar and the plates of your gym), so
 * the practice never changes your targets, log, statistics, backups or reminders.
 */
object PracticeWorkout {
    const val EXERCISE_KEY = "back_squat"
    private const val BAR_KEY = "olympic_bar"

    /** A short previous target, so stacking and advice show: heavy and light, snapped to your bar and plates. */
    const val HEAVY_KG = 60.0
    const val LIGHT_KG = 40.0

    /**
     * Empties the practice database and fills it for a new practice workout; returns its session. [real] is the app's
     * own container (read only). [progress] is what the demo did before the step it starts at (#99): the warm-up and GO
     * tapped, reps typed, as if the app had done it.
     */
    suspend fun start(real: AppContainer, progress: DemoProgress = DemoProgress(), now: Long = System.currentTimeMillis()): Long {
        check(!real.isPractice)
        val practice = real.practice
        val db = practice.db
        withContext(Dispatchers.IO) { db.clearAllTables() }
        val settings = real.settings.current()
        val texts = AppLanguage.wrap(real.app)
        val dutch = AppLanguage.get(real.app) == AppLanguage.DUTCH

        // Your bar and the plates of your gym, else the standard ones.
        val realBar = real.db.materials().all().firstOrNull { it.libraryKey == BAR_KEY && !it.archived }
        val stock = plateStockFor(null, real.db.gyms().all(), settings.plateStock)
        val libBar = DefaultLibrary.materials.first { it.key == BAR_KEY }
        val bar = realBar?.copy(id = 0) ?: MaterialProfileEntity(
            libraryKey = libBar.key, name = if (dutch) libBar.nameNl else libBar.nameEn, kind = libBar.profile.kind,
            baseKg = libBar.profile.baseKg, loadingPoints = libBar.profile.loadingPoints,
            minKg = libBar.profile.minKg, stepKg = libBar.profile.stepKg,
        )
        val calc = Calculators.of(bar.toProfile(), stock)
        val heavy = calc.snap(HEAVY_KG)
        val light = calc.snap(LIGHT_KG).coerceAtMost(heavy)

        val sessionId = db.withTransaction {
            val barId = db.materials().insert(bar)
            val gymId = db.gyms().insert(GymEntity(name = Gyms.defaultName(dutch), plates = stock.encode()))
            val lib = DefaultLibrary.exercise(EXERCISE_KEY)
            val exerciseId = db.exercises().insert(
                ExerciseEntity(
                    libraryKey = lib.key, name = lib.name, equipment = lib.equipment, loadType = lib.loadType,
                    progression = Progression.WEIGHT, materialProfileId = barId,
                    movement = DefaultLibrary.movementFor(lib.key),
                    // The default warm-up on the warm-up button: the empty bar.
                    defaultWarmupKg = bar.baseKg.takeIf { it > 0 },
                    defaultWarmupNote = texts.getString(R.string.practice_warmup_note),
                ),
            )
            db.exercises().insertMuscles(lib.muscles.map { ExerciseMuscleEntity(exerciseId, it.muscle, it.percent) })
            db.plans().replace(
                PlanEntity(exerciseId = exerciseId, updatedAt = now),
                listOf(
                    PlanSetEntity(exerciseId = exerciseId, position = 0, role = SetRole.HEAVY, weightKg = heavy),
                    PlanSetEntity(exerciseId = exerciseId, position = 1, role = SetRole.LIGHT, weightKg = light),
                ),
            )
            db.sessions().insert(
                SessionEntity(schemaName = texts.getString(R.string.practice_name), startedAt = now, gymId = gymId),
            )
        }
        // The heavy and the light set, aiming at the target above.
        practice.training.addExercise(sessionId, db.exercises().all().single().id)
        // Quick to fill in: the weights are already there, only the reps are left (the demo types them).
        val sets = db.sessions().sets(sessionId)
        db.sessions().updateSets(
            sets.map { set ->
                val reps = when (set.role) {
                    SetRole.HEAVY -> progress.heavyReps
                    SetRole.LIGHT -> progress.lightReps
                    SetRole.WARMUP -> null
                }
                set.copy(weightKg = set.goalWeightKg, reps = reps ?: set.reps)
            },
        )
        // Where the demo starts (#99): the warm-up and GO already tapped, the reps typed. The warm-up set the button
        // adds is left out, so the first empty work set is the open one, as when the demo got there itself.
        val se = db.sessions().exercises(sessionId).single()
        if (progress.warmup) practice.training.markWarmup(se.id, now)
        if (progress.go) practice.training.markGo(se.id, now)
        if (progress.heavyReps != null || progress.lightReps != null) practice.training.markFirstInput(se.id, now)
        return sessionId
    }
}
