package nl.ibs.gym8020.data

import android.content.Context

/**
 * Remembers in which session exercise you set your own target (B-6), so moving on never replaces it with
 * advice. Kept outside the database until schema v3 gives the target its own model (A-1).
 */
interface TargetMarks {
    fun isSetByYou(sessionExerciseId: Long): Boolean
    fun markSetByYou(sessionExerciseId: Long)
    fun unmarkSetByYou(sessionExerciseId: Long)
}

class PrefsTargetMarks(context: Context) : TargetMarks {
    private val prefs = context.getSharedPreferences("target_marks", Context.MODE_PRIVATE)

    override fun isSetByYou(sessionExerciseId: Long): Boolean = prefs.getBoolean(sessionExerciseId.toString(), false)

    override fun markSetByYou(sessionExerciseId: Long) {
        prefs.edit().putBoolean(sessionExerciseId.toString(), true).apply()
    }

    override fun unmarkSetByYou(sessionExerciseId: Long) {
        prefs.edit().remove(sessionExerciseId.toString()).apply()
    }
}
