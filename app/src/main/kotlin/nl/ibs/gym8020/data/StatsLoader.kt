package nl.ibs.gym8020.data

import nl.ibs.gym8020.core.logic.EffectiveWeight
import nl.ibs.gym8020.core.logic.PerformanceWeight
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.stats.SetRecord
import nl.ibs.gym8020.data.db.AppDatabase
import nl.ibs.gym8020.data.db.ExerciseEntity
import nl.ibs.gym8020.data.db.SessionEntity
import nl.ibs.gym8020.data.db.SessionExerciseEntity

/** Everything the statistics need, read once from the database. Only finished trainings; skipped exercises left out. */
class StatsData(
    val sessions: List<SessionEntity>,
    val sessionExercises: List<SessionExerciseEntity>,
    val exercises: Map<Long, ExerciseEntity>,
    val muscles: Map<Long, List<MuscleShare>>,
    val sets: List<SetRecord>,
)

suspend fun loadStats(db: AppDatabase): StatsData {
    val sessions = db.sessions().all().filter { it.endedAt != null }
    val sessionById = sessions.associateBy { it.id }
    val ses = db.sessions().allExercises().filter { it.sessionId in sessionById && !it.skipped }
    val seById = ses.associateBy { it.id }
    val exercises = db.exercises().all().associateBy { it.id }
    val muscles = db.exercises().allMuscles().groupBy { it.exerciseId }
        .mapValues { (_, l) -> l.map { MuscleShare(it.muscle, it.percent) } }
    val sets = db.sessions().allSets().mapNotNull { s ->
        val se = seById[s.sessionExerciseId] ?: return@mapNotNull null
        val session = sessionById[se.sessionId] ?: return@mapNotNull null
        val ex = exercises[se.exerciseId] ?: return@mapNotNull null
        val w = PerformanceWeight.orNone(s.weightKg, ex.progression, ex.loadType)
        SetRecord(
            sessionId = session.id,
            date = session.startedAt.toLocalDate(),
            exerciseId = ex.id,
            role = s.role,
            weightKg = s.weightKg,
            reps = s.reps,
            effectiveKg = w?.let { EffectiveWeight.of(ex.loadType, it, s.bodyweightKg, ex.bodyweightFactor) },
            goalWeightKg = s.goalWeightKg,
            progression = ex.progression,
        )
    }
    return StatsData(sessions, ses, exercises, muscles, sets)
}
