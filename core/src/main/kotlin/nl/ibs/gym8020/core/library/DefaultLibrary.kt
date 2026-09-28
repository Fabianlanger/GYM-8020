package nl.ibs.gym8020.core.library

import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Movement
import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.Muscle.*
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression
import java.time.DayOfWeek
import java.time.DayOfWeek.*

/** A standard exercise shipped with the app. [key] is stable and used by default schemas and backups. */
data class LibraryExercise(
    val key: String,
    val name: String,
    val equipment: Equipment,
    val loadType: LoadType = if (equipment == Equipment.BODYWEIGHT) LoadType.BODYWEIGHT else LoadType.EXTERNAL,
    val progression: Progression = Progression.WEIGHT,
    /** Share of body weight moved (bodyweight exercises only). */
    val bodyweightFactor: Double = 1.0,
    val muscles: List<MuscleShare>,
)

/** A standard material profile (D-27). Names in English and Dutch; the app picks by language. */
data class LibraryMaterial(val key: String, val nameEn: String, val nameNl: String, val profile: MaterialProfile)

data class LibrarySchema(val key: String, val name: String, val exerciseKeys: List<String>)

data class LibraryWeekPlan(
    val key: String,
    val name: String,
    val targetPerWeek: Int,
    /** Schema key per slot with its default weekday. */
    val slots: List<Pair<String, DayOfWeek?>>,
)

private fun p(vararg m: Muscle) = m.map { MuscleShare(it, 100) }
private fun s(vararg m: Muscle) = m.map { MuscleShare(it, 50) }

/**
 * Generic standard content (D-47, D-63): ~55 common exercises, default schemas and week plans.
 * No weights, no personal data. Muscle shares: primary 100 %, secondary 50 % (to be refined, R-1).
 * Bodyweight factors for push-up (0.65) and dips (0.95) follow literature; others are estimates.
 */
object DefaultLibrary {

    val exercises: List<LibraryExercise> = listOf(
        // Chest
        LibraryExercise("bench_press", "Bench press", Equipment.BARBELL, muscles = p(CHEST) + s(FRONT_DELT, TRICEPS)),
        LibraryExercise("incline_bench_press", "Incline bench press", Equipment.BARBELL, muscles = p(CHEST) + s(FRONT_DELT, TRICEPS)),
        LibraryExercise("dumbbell_bench_press", "Dumbbell bench press", Equipment.DUMBBELL, muscles = p(CHEST) + s(FRONT_DELT, TRICEPS)),
        LibraryExercise("incline_dumbbell_press", "Incline dumbbell press", Equipment.DUMBBELL, muscles = p(CHEST) + s(FRONT_DELT, TRICEPS)),
        LibraryExercise("chest_press_machine", "Chest press machine", Equipment.MACHINE, muscles = p(CHEST) + s(FRONT_DELT, TRICEPS)),
        LibraryExercise("pec_deck", "Pec deck", Equipment.MACHINE, muscles = p(CHEST) + s(FRONT_DELT)),
        LibraryExercise("cable_crossover", "Cable crossover", Equipment.CABLE, muscles = p(CHEST) + s(FRONT_DELT)),
        LibraryExercise("push_up", "Push-up", Equipment.BODYWEIGHT, bodyweightFactor = 0.65, muscles = p(CHEST) + s(FRONT_DELT, TRICEPS)),
        LibraryExercise("dips", "Dips", Equipment.BODYWEIGHT, bodyweightFactor = 0.95, muscles = p(CHEST, TRICEPS) + s(FRONT_DELT)),
        // Shoulders
        LibraryExercise("overhead_press", "Military press", Equipment.BARBELL, muscles = p(FRONT_DELT) + s(SIDE_DELT, TRICEPS)),
        LibraryExercise("dumbbell_shoulder_press", "Dumbbell shoulder press", Equipment.DUMBBELL, muscles = p(FRONT_DELT) + s(SIDE_DELT, TRICEPS)),
        LibraryExercise("shoulder_press_machine", "Shoulder press machine", Equipment.MACHINE, muscles = p(FRONT_DELT) + s(SIDE_DELT, TRICEPS)),
        LibraryExercise("lateral_raise", "Lateral raise", Equipment.DUMBBELL, muscles = p(SIDE_DELT)),
        LibraryExercise("cable_lateral_raise", "Cable lateral raise", Equipment.CABLE, muscles = p(SIDE_DELT)),
        LibraryExercise("reverse_pec_deck", "Reverse pec deck", Equipment.MACHINE, muscles = p(REAR_DELT) + s(MID_BACK)),
        LibraryExercise("face_pull", "Face pull", Equipment.CABLE, muscles = p(REAR_DELT) + s(MID_BACK, TRAPS)),
        LibraryExercise("shrug", "Shrug", Equipment.DUMBBELL, muscles = p(TRAPS) + s(FOREARMS)),
        // Back
        LibraryExercise("pull_up", "Pull-up", Equipment.BODYWEIGHT, bodyweightFactor = 1.0, muscles = p(LATS) + s(BICEPS, MID_BACK)),
        LibraryExercise("chin_up", "Chin-up", Equipment.BODYWEIGHT, bodyweightFactor = 1.0, muscles = p(LATS, BICEPS) + s(MID_BACK)),
        LibraryExercise("lat_pulldown", "Lat pulldown", Equipment.CABLE, muscles = p(LATS) + s(BICEPS, MID_BACK)),
        LibraryExercise("barbell_row", "Barbell row", Equipment.BARBELL, muscles = p(LATS, MID_BACK) + s(BICEPS, REAR_DELT, LOWER_BACK)),
        LibraryExercise("seated_cable_row", "Seated cable row", Equipment.CABLE, muscles = p(MID_BACK, LATS) + s(BICEPS, REAR_DELT)),
        LibraryExercise("dumbbell_row", "Dumbbell row", Equipment.DUMBBELL, muscles = p(LATS, MID_BACK) + s(BICEPS, REAR_DELT)),
        LibraryExercise("t_bar_row", "T-bar row", Equipment.BARBELL, muscles = p(MID_BACK, LATS) + s(BICEPS, REAR_DELT)),
        LibraryExercise("deadlift", "Deadlift", Equipment.BARBELL, muscles = p(GLUTES, HAMSTRINGS, LOWER_BACK) + s(QUADS, TRAPS, FOREARMS)),
        LibraryExercise("romanian_deadlift", "Romanian deadlift", Equipment.BARBELL, muscles = p(HAMSTRINGS, GLUTES) + s(LOWER_BACK)),
        LibraryExercise("back_extension", "Back extension", Equipment.BODYWEIGHT, bodyweightFactor = 0.55, muscles = p(LOWER_BACK) + s(GLUTES, HAMSTRINGS)),
        // Arms
        LibraryExercise("barbell_curl", "Barbell curl", Equipment.BARBELL, muscles = p(BICEPS) + s(FOREARMS)),
        LibraryExercise("dumbbell_curl", "Dumbbell curl", Equipment.DUMBBELL, muscles = p(BICEPS) + s(FOREARMS)),
        LibraryExercise("hammer_curl", "Hammer curl", Equipment.DUMBBELL, muscles = p(BICEPS, FOREARMS)),
        LibraryExercise("cable_biceps_curl", "Cable biceps curl", Equipment.CABLE, muscles = p(BICEPS) + s(FOREARMS)),
        LibraryExercise("preacher_curl", "Preacher curl", Equipment.MACHINE, muscles = p(BICEPS)),
        LibraryExercise("cable_triceps_pushdown", "Cable triceps pushdown", Equipment.CABLE, muscles = p(TRICEPS)),
        LibraryExercise("overhead_cable_triceps_extension", "Overhead cable triceps extension", Equipment.CABLE, muscles = p(TRICEPS)),
        LibraryExercise("skull_crusher", "Skull crusher", Equipment.BARBELL, muscles = p(TRICEPS)),
        LibraryExercise("close_grip_bench_press", "Close-grip bench press", Equipment.BARBELL, muscles = p(TRICEPS) + s(CHEST, FRONT_DELT)),
        LibraryExercise("wrist_curl", "Wrist curl", Equipment.DUMBBELL, muscles = p(FOREARMS)),
        // Legs
        LibraryExercise("back_squat", "Back squat", Equipment.BARBELL, muscles = p(QUADS, GLUTES) + s(ADDUCTORS, LOWER_BACK)),
        LibraryExercise("front_squat", "Front squat", Equipment.BARBELL, muscles = p(QUADS) + s(GLUTES, ABS)),
        LibraryExercise("leg_press", "Leg press", Equipment.MACHINE, muscles = p(QUADS) + s(GLUTES, ADDUCTORS)),
        LibraryExercise("hack_squat", "Hack squat", Equipment.MACHINE, muscles = p(QUADS) + s(GLUTES)),
        LibraryExercise("bulgarian_split_squat", "Bulgarian split squat", Equipment.DUMBBELL, muscles = p(QUADS, GLUTES) + s(ADDUCTORS)),
        LibraryExercise("walking_lunge", "Walking lunge", Equipment.DUMBBELL, muscles = p(QUADS, GLUTES) + s(HAMSTRINGS)),
        LibraryExercise("leg_extension", "Leg extension", Equipment.MACHINE, muscles = p(QUADS)),
        LibraryExercise("lying_leg_curl", "Lying leg curl", Equipment.MACHINE, muscles = p(HAMSTRINGS)),
        LibraryExercise("seated_leg_curl", "Seated leg curl", Equipment.MACHINE, muscles = p(HAMSTRINGS)),
        LibraryExercise("hip_thrust", "Hip thrust", Equipment.BARBELL, muscles = p(GLUTES) + s(HAMSTRINGS)),
        LibraryExercise("adductor_machine", "Adductor machine", Equipment.MACHINE, muscles = p(ADDUCTORS)),
        LibraryExercise("abductor_machine", "Abductor machine", Equipment.MACHINE, muscles = p(GLUTES)),
        LibraryExercise("standing_calf_raise", "Standing calf raise", Equipment.MACHINE, muscles = p(CALVES)),
        LibraryExercise("seated_calf_raise", "Seated calf raise", Equipment.MACHINE, muscles = p(CALVES)),
        // Core (bodyweight with progression on reps; factor 0.30 is an estimate, D-12a)
        LibraryExercise("crunch", "Crunch", Equipment.BODYWEIGHT, progression = Progression.REPS, bodyweightFactor = 0.30, muscles = p(ABS)),
        LibraryExercise("decline_crunch", "Decline crunch", Equipment.BODYWEIGHT, progression = Progression.REPS, bodyweightFactor = 0.30, muscles = p(ABS) + s(OBLIQUES)),
        LibraryExercise("hanging_leg_raise", "Hanging leg raise", Equipment.BODYWEIGHT, progression = Progression.REPS, bodyweightFactor = 0.30, muscles = p(ABS) + s(OBLIQUES)),
        LibraryExercise("russian_twist", "Russian twist", Equipment.BODYWEIGHT, progression = Progression.REPS, bodyweightFactor = 0.30, muscles = p(OBLIQUES) + s(ABS)),
        LibraryExercise("cable_crunch", "Cable crunch", Equipment.CABLE, muscles = p(ABS) + s(OBLIQUES)),
    )

    val schemas: List<LibrarySchema> = listOf(
        LibrarySchema(
            "full_body", "Full body",
            listOf(
                "back_squat", "incline_bench_press", "pull_up", "seated_calf_raise", "overhead_press",
                "seated_cable_row", "decline_crunch", "cable_biceps_curl", "cable_triceps_pushdown",
            ),
        ),
        LibrarySchema("push", "Push", listOf("bench_press", "overhead_press", "incline_dumbbell_press", "lateral_raise", "dips", "cable_triceps_pushdown")),
        LibrarySchema("pull", "Pull", listOf("pull_up", "barbell_row", "lat_pulldown", "seated_cable_row", "face_pull", "barbell_curl", "hammer_curl")),
        LibrarySchema("legs", "Legs", listOf("back_squat", "romanian_deadlift", "leg_press", "lying_leg_curl", "standing_calf_raise", "hanging_leg_raise")),
        LibrarySchema("upper_a", "Upper A", listOf("bench_press", "barbell_row", "overhead_press", "lat_pulldown", "cable_biceps_curl", "cable_triceps_pushdown")),
        LibrarySchema("lower_a", "Lower A", listOf("back_squat", "romanian_deadlift", "leg_extension", "standing_calf_raise", "crunch")),
        LibrarySchema(
            "upper_b", "Upper B",
            listOf("incline_dumbbell_press", "pull_up", "dumbbell_shoulder_press", "seated_cable_row", "lateral_raise", "hammer_curl", "overhead_cable_triceps_extension"),
        ),
        LibrarySchema("lower_b", "Lower B", listOf("deadlift", "leg_press", "lying_leg_curl", "seated_calf_raise", "hanging_leg_raise")),
    )

    val weekPlans: List<LibraryWeekPlan> = listOf(
        LibraryWeekPlan("full_body_3x", "Full body", 3, listOf("full_body" to MONDAY, "full_body" to WEDNESDAY, "full_body" to FRIDAY)),
        LibraryWeekPlan("ppl", "Push / Pull / Legs", 3, listOf("push" to MONDAY, "pull" to WEDNESDAY, "legs" to FRIDAY)),
        LibraryWeekPlan("upper_lower", "Upper / Lower", 4, listOf("upper_a" to MONDAY, "lower_a" to TUESDAY, "upper_b" to THURSDAY, "lower_b" to FRIDAY)),
    )

    val materials: List<LibraryMaterial> = listOf(
        LibraryMaterial("olympic_bar", "Olympic bar 20 kg", "Olympische stang 20 kg", MaterialProfile(MaterialKind.PLATES, baseKg = 20.0, loadingPoints = 2)),
        LibraryMaterial("ez_bar", "EZ bar 10 kg", "EZ-stang 10 kg", MaterialProfile(MaterialKind.PLATES, baseKg = 10.0, loadingPoints = 2)),
        LibraryMaterial("plate_machine", "Plate-loaded machine", "Plate-loaded machine", MaterialProfile(MaterialKind.PLATES, baseKg = 0.0, loadingPoints = 2)),
        LibraryMaterial("pin_stack", "Pin stack 5 kg steps", "Pin-stack per 5 kg", MaterialProfile(MaterialKind.STACK, minKg = 5.0, stepKg = 5.0, maxKg = 120.0)),
        LibraryMaterial("cable", "Cable 2.5 kg steps", "Kabel per 2,5 kg", MaterialProfile(MaterialKind.STACK, minKg = 2.5, stepKg = 2.5, maxKg = 80.0)),
        LibraryMaterial("dumbbells", "Dumbbells 2–40 kg", "Dumbbells 2–40 kg", MaterialProfile(MaterialKind.DUMBBELLS, minKg = 2.0, stepKg = 2.0, maxKg = 40.0)),
        LibraryMaterial("bodyweight", "Assisted machine + dip belt", "Assisted machine + dipriem", MaterialProfile(MaterialKind.BODYWEIGHT, stepKg = 5.0, maxKg = 60.0)),
    )

    /** Movement pattern per library exercise (for the icon). */
    fun movementFor(key: String): Movement = when (key) {
        "back_squat", "front_squat", "hack_squat" -> Movement.SQUAT
        "deadlift", "romanian_deadlift", "hip_thrust", "back_extension" -> Movement.HINGE
        "bulgarian_split_squat", "walking_lunge" -> Movement.LUNGE
        "leg_press", "leg_extension", "lying_leg_curl", "seated_leg_curl", "adductor_machine", "abductor_machine" -> Movement.LEG_MACHINE
        "standing_calf_raise", "seated_calf_raise" -> Movement.CALF
        "bench_press", "incline_bench_press", "dumbbell_bench_press", "incline_dumbbell_press", "chest_press_machine",
        "push_up", "close_grip_bench_press" -> Movement.HORIZONTAL_PUSH
        "overhead_press", "dumbbell_shoulder_press", "shoulder_press_machine" -> Movement.VERTICAL_PUSH
        "pec_deck", "cable_crossover" -> Movement.FLY
        "dips" -> Movement.DIP
        "lateral_raise", "cable_lateral_raise", "reverse_pec_deck", "face_pull" -> Movement.RAISE
        "pull_up", "chin_up", "lat_pulldown" -> Movement.VERTICAL_PULL
        "barbell_row", "seated_cable_row", "dumbbell_row", "t_bar_row" -> Movement.HORIZONTAL_PULL
        "barbell_curl", "dumbbell_curl", "hammer_curl", "cable_biceps_curl", "preacher_curl", "wrist_curl" -> Movement.CURL
        "cable_triceps_pushdown", "overhead_cable_triceps_extension", "skull_crusher" -> Movement.TRICEPS
        "shrug" -> Movement.SHRUG
        else -> Movement.CORE
    }

    /** Default material per library exercise; null = no rounding (e.g. crunches). */
    fun materialFor(exercise: LibraryExercise): String? = when (exercise.key) {
        "skull_crusher", "barbell_curl" -> "ez_bar"
        "leg_press", "hack_squat" -> "plate_machine"
        "crunch", "decline_crunch", "hanging_leg_raise", "russian_twist" -> null
        else -> when (exercise.equipment) {
            Equipment.BARBELL -> "olympic_bar"
            Equipment.DUMBBELL -> "dumbbells"
            Equipment.MACHINE -> "pin_stack"
            Equipment.CABLE -> "cable"
            Equipment.BODYWEIGHT -> "bodyweight"
        }
    }

    /** The week plan that is active on first start. */
    const val DEFAULT_ACTIVE_WEEK_PLAN = "full_body_3x"

    fun exercise(key: String): LibraryExercise = exercises.first { it.key == key }
}
