package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression

/**
 * "Volgende keer op gewicht / op reps" (#52, ADR 0001): switching an exercise's progression while preparing
 * your target. It applies from the next session.
 */
object ProgressionSwitch {

    /** The progression you can switch to, or null when there is no switch for this exercise. */
    fun offered(progression: Progression, loadType: LoadType): Progression? = when {
        progression == Progression.REPS -> Progression.WEIGHT
        loadType == LoadType.BODYWEIGHT -> Progression.REPS
        else -> null
    }

    /**
     * Your target right after switching to [to], with the sets of [sets]. To weight on a bodyweight exercise:
     * the signed load starts at 0 (body weight only). To reps: no weight, the reps of today are the target.
     */
    fun target(sets: List<PlannedSet>, today: List<PerformedSet>, to: Progression, loadType: LoadType): List<PlannedSet> =
        if (to == Progression.WEIGHT && loadType == LoadType.BODYWEIGHT) {
            sets.map { PlannedSet(it.role, if (it.role.isWork) 0.0 else null) }
        } else {
            DefaultTarget.of(sets, sets.map { it.role }, today, to, loadType)
        }
}
