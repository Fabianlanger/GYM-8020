package nl.ibs.gym8020.core.model

/** Body side a muscle is drawn on in the heatmap. */
enum class BodySide { FRONT, BACK }

/** A larger group of muscles, used for roll-up statistics. */
enum class MuscleGroup { CHEST, SHOULDERS, BACK, ARMS, CORE, LEGS }

/** Individual muscles. Statistics are kept per muscle and rolled up to [MuscleGroup]. */
enum class Muscle(val group: MuscleGroup, val side: BodySide) {
    CHEST(MuscleGroup.CHEST, BodySide.FRONT),
    FRONT_DELT(MuscleGroup.SHOULDERS, BodySide.FRONT),
    SIDE_DELT(MuscleGroup.SHOULDERS, BodySide.FRONT),
    REAR_DELT(MuscleGroup.SHOULDERS, BodySide.BACK),
    LATS(MuscleGroup.BACK, BodySide.BACK),
    TRAPS(MuscleGroup.BACK, BodySide.BACK),
    MID_BACK(MuscleGroup.BACK, BodySide.BACK),
    LOWER_BACK(MuscleGroup.BACK, BodySide.BACK),
    BICEPS(MuscleGroup.ARMS, BodySide.FRONT),
    TRICEPS(MuscleGroup.ARMS, BodySide.BACK),
    FOREARMS(MuscleGroup.ARMS, BodySide.FRONT),
    ABS(MuscleGroup.CORE, BodySide.FRONT),
    OBLIQUES(MuscleGroup.CORE, BodySide.FRONT),
    QUADS(MuscleGroup.LEGS, BodySide.FRONT),
    HAMSTRINGS(MuscleGroup.LEGS, BodySide.BACK),
    GLUTES(MuscleGroup.LEGS, BodySide.BACK),
    ADDUCTORS(MuscleGroup.LEGS, BodySide.FRONT),
    CALVES(MuscleGroup.LEGS, BodySide.BACK),
}

/**
 * How the moved weight is determined.
 * - [EXTERNAL]: the entered weight is the moved weight.
 * - [BODYWEIGHT]: body weight × factor plus a signed extra (negative = assistance).
 */
enum class LoadType { EXTERNAL, BODYWEIGHT }

/** Whether goals and progress are tracked on weight or, for weightless exercises, on reps. */
enum class Progression { WEIGHT, REPS }

/** Where the weight comes from; used for the warm-up hint (and material profiles in v0.2). */
enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT }

/** Movement pattern; drives the exercise icon. */
enum class Movement {
    SQUAT, HINGE, LUNGE, LEG_MACHINE, CALF,
    HORIZONTAL_PUSH, VERTICAL_PUSH, FLY, DIP, RAISE,
    VERTICAL_PULL, HORIZONTAL_PULL, CURL, TRICEPS, SHRUG, CORE,
}

enum class SetRole { HEAVY, LIGHT, WARMUP;

    val isWork: Boolean get() = this != WARMUP
}

/** Where a training comes from (A-2): live in the app, entered afterwards, imported, or from a wearable. */
enum class SessionOrigin { LIVE, AFTERWARDS, IMPORTED, WEARABLE }

/** Where a body weight measurement comes from (A-14): entered by you, or from a wearable or scale. */
enum class MeasurementOrigin { MANUAL, WEARABLE }

/**
 * Who set the target for next time in a session exercise: you ([YOURS]), the default target when you set
 * nothing ([DEFAULT]), or not known because it was derived afterwards from older data ([UNKNOWN], schema v3).
 */
enum class TargetOrigin { YOURS, DEFAULT, UNKNOWN }

/** Result of a work set compared to its rep target. */
enum class Signal { TOO_LIGHT, GOOD, TOO_HEAVY }

/** Share of an exercise's load that a muscle receives, in percent (primary 100, secondary 50). */
data class MuscleShare(val muscle: Muscle, val percent: Int) {
    init {
        require(percent in 1..100) { "percent must be 1..100, was $percent" }
    }
}

/** Method settings. Defaults follow the method; all are user-adjustable. */
data class MethodSettings(
    val heavyTarget: Int = 6,
    val lightTarget: Int = 16,
    val defaultSetCount: Int = 2,
    /** Reps within ±goodMargin of the target count as GOOD. */
    val goodMargin: Int = 1,
    /** Weight step used for suggestions until material profiles exist (v0.2). */
    val weightStepKg: Double = 2.5,
) {
    fun targetFor(role: SetRole): Int? = when (role) {
        SetRole.HEAVY -> heavyTarget
        SetRole.LIGHT -> lightTarget
        SetRole.WARMUP -> null
    }
}
