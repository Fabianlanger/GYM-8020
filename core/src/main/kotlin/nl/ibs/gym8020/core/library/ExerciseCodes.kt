package nl.ibs.gym8020.core.library

/**
 * Standard codes per library exercise (A-5, #18), so wearables and imports point at the same exercise:
 * - [healthConnect]: the Health Connect exercise segment type (`ExerciseSegment.SEGMENT_TYPE_*`), without the
 *   `SEGMENT_TYPE_` prefix, e.g. "BENCH_PRESS". The session itself is a strength training.
 * - [garmin]: the Garmin FIT exercise category (`exercise_category`), e.g. "bench_press".
 * Null where there is no clear match: a code is left empty rather than guessed.
 */
data class ExerciseCodes(val healthConnect: String?, val garmin: String?)

object ExerciseCodeTable {
    /** Health Connect segment types used below (the ones that fit strength training). */
    val healthConnectTypes: Set<String> = setOf(
        "ARM_CURL", "BACK_EXTENSION", "BARBELL_SHOULDER_PRESS", "BENCH_PRESS", "CRUNCH", "DEADLIFT",
        "DOUBLE_ARM_TRICEPS_EXTENSION", "DUMBBELL_LATERAL_RAISE", "DUMBBELL_ROW", "HIP_THRUST", "LATERAL_RAISE",
        "LAT_PULL_DOWN", "LEG_CURL", "LEG_EXTENSION", "LEG_PRESS", "LEG_RAISE", "LUNGE", "PULL_UP", "SHOULDER_PRESS",
        "SQUAT",
    )

    /** Garmin FIT exercise categories used below. */
    val garminCategories: Set<String> = setOf(
        "bench_press", "calf_raise", "crunch", "curl", "deadlift", "flye", "hip_raise", "hyperextension",
        "lateral_raise", "leg_curl", "leg_raise", "lunge", "pull_up", "push_up", "row", "shoulder_press", "shrug",
        "squat", "triceps_extension",
    )

    private fun c(healthConnect: String?, garmin: String?) = ExerciseCodes(healthConnect, garmin)

    /** Per library key; a key that is missing has no codes. */
    val byKey: Map<String, ExerciseCodes> = mapOf(
        // Chest
        "bench_press" to c("BENCH_PRESS", "bench_press"),
        "incline_bench_press" to c("BENCH_PRESS", "bench_press"),
        "dumbbell_bench_press" to c("BENCH_PRESS", "bench_press"),
        "incline_dumbbell_press" to c("BENCH_PRESS", "bench_press"),
        "chest_press_machine" to c(null, "bench_press"),
        "pec_deck" to c(null, "flye"),
        "cable_crossover" to c(null, "flye"),
        "push_up" to c(null, "push_up"),
        "dips" to c(null, "triceps_extension"),
        // Shoulders
        "overhead_press" to c("BARBELL_SHOULDER_PRESS", "shoulder_press"),
        "dumbbell_shoulder_press" to c("SHOULDER_PRESS", "shoulder_press"),
        "shoulder_press_machine" to c("SHOULDER_PRESS", "shoulder_press"),
        "lateral_raise" to c("DUMBBELL_LATERAL_RAISE", "lateral_raise"),
        "cable_lateral_raise" to c("LATERAL_RAISE", "lateral_raise"),
        "shrug" to c(null, "shrug"),
        // Back
        "pull_up" to c("PULL_UP", "pull_up"),
        "chin_up" to c("PULL_UP", "pull_up"),
        "lat_pulldown" to c("LAT_PULL_DOWN", "pull_up"),
        "barbell_row" to c(null, "row"),
        "seated_cable_row" to c(null, "row"),
        "dumbbell_row" to c("DUMBBELL_ROW", "row"),
        "t_bar_row" to c(null, "row"),
        "deadlift" to c("DEADLIFT", "deadlift"),
        "romanian_deadlift" to c("DEADLIFT", "deadlift"),
        "back_extension" to c("BACK_EXTENSION", "hyperextension"),
        // Arms
        "barbell_curl" to c("ARM_CURL", "curl"),
        "dumbbell_curl" to c("ARM_CURL", "curl"),
        "hammer_curl" to c("ARM_CURL", "curl"),
        "cable_biceps_curl" to c("ARM_CURL", "curl"),
        "preacher_curl" to c("ARM_CURL", "curl"),
        "cable_triceps_pushdown" to c(null, "triceps_extension"),
        "overhead_cable_triceps_extension" to c("DOUBLE_ARM_TRICEPS_EXTENSION", "triceps_extension"),
        "skull_crusher" to c(null, "triceps_extension"),
        "close_grip_bench_press" to c("BENCH_PRESS", "bench_press"),
        // Legs
        "back_squat" to c("SQUAT", "squat"),
        "front_squat" to c("SQUAT", "squat"),
        "leg_press" to c("LEG_PRESS", "squat"),
        "hack_squat" to c("SQUAT", "squat"),
        "bulgarian_split_squat" to c("LUNGE", "lunge"),
        "walking_lunge" to c("LUNGE", "lunge"),
        "leg_extension" to c("LEG_EXTENSION", null),
        "lying_leg_curl" to c("LEG_CURL", "leg_curl"),
        "seated_leg_curl" to c("LEG_CURL", "leg_curl"),
        "hip_thrust" to c("HIP_THRUST", "hip_raise"),
        "standing_calf_raise" to c(null, "calf_raise"),
        "seated_calf_raise" to c(null, "calf_raise"),
        // Core
        "crunch" to c("CRUNCH", "crunch"),
        "decline_crunch" to c("CRUNCH", "crunch"),
        "hanging_leg_raise" to c("LEG_RAISE", "leg_raise"),
        "cable_crunch" to c("CRUNCH", "crunch"),
    )

    fun of(libraryKey: String?): ExerciseCodes? = libraryKey?.let(byKey::get)
}
