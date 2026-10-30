package nl.ibs.gym8020.data

import android.content.Context

/**
 * Reps targets for exercises with reps progression (#69), kept outside the database until schema v3 gives the
 * target and the session target a reps column (A-1); then these move into the database and this store goes.
 *
 * Two things are kept:
 * - the reps per set of your stored target, by set position, next to the target's sets in the database;
 * - the session target in reps per set (what you aimed at that day), next to the set's `goalWeightKg`.
 */
interface RepsTargets {
    /** The reps per set of the stored target of [exerciseId], by position; empty when it has none. */
    fun target(exerciseId: Long): List<Int?>

    /** Stores [reps] per set position for [exerciseId]; without any reps the entry is removed. */
    fun setTarget(exerciseId: Long, reps: List<Int?>)

    /** The reps you aimed at in set [setId] when its training started, or null. */
    fun sessionGoal(setId: Long): Int?

    /** Stores the reps aimed at in set [setId]; null removes it (set ids can be reused after a delete). */
    fun setSessionGoal(setId: Long, reps: Int?)

    /** Forgets everything: importing a backup replaces all data. */
    fun clear()
}

class PrefsRepsTargets(context: Context) : RepsTargets {
    private val prefs = context.getSharedPreferences("reps_targets", Context.MODE_PRIVATE)

    override fun target(exerciseId: Long): List<Int?> =
        prefs.getString("target_$exerciseId", null)?.split(",")?.map { it.toIntOrNull() }.orEmpty()

    override fun setTarget(exerciseId: Long, reps: List<Int?>) {
        val key = "target_$exerciseId"
        if (reps.all { it == null }) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, reps.joinToString(",") { it?.toString().orEmpty() }).apply()
        }
    }

    override fun sessionGoal(setId: Long): Int? {
        val key = "goal_$setId"
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    override fun setSessionGoal(setId: Long, reps: Int?) {
        val key = "goal_$setId"
        if (reps == null) prefs.edit().remove(key).apply() else prefs.edit().putInt(key, reps).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
