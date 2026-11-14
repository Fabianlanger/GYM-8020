package nl.ibs.gym8020.core.logic

import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.Equipment
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the value of a default warm-up means, matching the exercise (#93). */
enum class WarmupMeasure {
    /** A weight in kg (bar, pin stack, dumbbells). */
    WEIGHT,

    /** A signed extra weight in kg for a bodyweight exercise (ADR 0001): negative = assistance, 0 = body weight only. */
    SIGNED_WEIGHT,

    /** A number of (mini-)reps, for an exercise with reps progression. */
    REPS,
}

/**
 * The default warm-up of an exercise (GLOSSARY "Standaard-warming-up", #80, #93): a value in the exercise's
 * [measure] and a short note. Both are optional; it never counts anywhere (volume, strength, records, comparison).
 *
 * Storage: `exercises.defaultWarmupKg` and `defaultWarmupNote` (schema v3). There is no separate column for reps:
 * with [WarmupMeasure.REPS] the stored "kg" holds the number of reps. Always read and write through [read] and
 * [storedValue], never the column directly, so that interpretation stays in this one place.
 */
data class DefaultWarmup(val measure: WarmupMeasure, val value: Double?, val note: String? = null) {

    /** The weight to load (signed for bodyweight exercises); null for a warm-up on reps or without a value. */
    val weightKg: Double? get() = value.takeIf { measure != WarmupMeasure.REPS }

    /** The number of reps for a warm-up on reps; null otherwise. */
    val reps: Int? get() = if (measure == WarmupMeasure.REPS) value?.roundToInt() else null

    /** The value as it goes into `exercises.defaultWarmupKg` (reps as a whole number with [WarmupMeasure.REPS]). */
    val storedValue: Double? get() = if (measure == WarmupMeasure.REPS) reps?.toDouble() else value

    /** The note as it is stored: trimmed, no blank note. */
    val storedNote: String? get() = note?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        /** Mini-reps for a warm-up of an exercise on reps. */
        const val MINI_REPS = 5

        /** An empty Olympic bar when the exercise has no material profile (D-44). */
        const val EMPTY_BAR_KG = 20.0

        /**
         * The measure of an exercise's default warm-up. Pull-ups, chin-ups and dips warm up with assistance (B-4),
         * also on reps; other exercises on reps warm up with mini-reps; other bodyweight exercises with a signed
         * extra; everything else with a weight.
         */
        fun measureOf(loadType: LoadType, progression: Progression, bodyweightFactor: Double): WarmupMeasure = when {
            Warmup.needsAssistance(loadType, bodyweightFactor) -> WarmupMeasure.SIGNED_WEIGHT
            progression == Progression.REPS -> WarmupMeasure.REPS
            loadType == LoadType.BODYWEIGHT -> WarmupMeasure.SIGNED_WEIGHT
            else -> WarmupMeasure.WEIGHT
        }

        /** The default warm-up as stored on the exercise; null when there is neither a value nor a note. */
        fun read(storedKg: Double?, storedNote: String?, measure: WarmupMeasure): DefaultWarmup? {
            val note = storedNote?.trim()?.takeIf { it.isNotEmpty() }
            if (storedKg == null && note == null) return null
            val value = if (measure == WarmupMeasure.REPS) storedKg?.roundToInt()?.toDouble() else storedKg
            return DefaultWarmup(measure, value, note)
        }

        /**
         * The value to prefill once, from the material, while the exercise has no default value yet: the empty bar,
         * the lightest pin or dumbbell, assistance for pull-ups and dips ([assistKg], see [Warmup.assistKg]; null
         * while the body weight is unknown), body weight only for other bodyweight exercises, or [MINI_REPS] on reps.
         * Null when the material says nothing (no profile and no bar).
         */
        fun prefill(
            measure: WarmupMeasure,
            loadType: LoadType,
            bodyweightFactor: Double,
            equipment: Equipment,
            calc: WeightCalculator?,
            assistKg: Double?,
        ): Double? = when (measure) {
            WarmupMeasure.REPS -> MINI_REPS.toDouble()
            WarmupMeasure.SIGNED_WEIGHT -> if (Warmup.needsAssistance(loadType, bodyweightFactor)) assistKg else 0.0
            WarmupMeasure.WEIGHT -> when {
                calc == null -> EMPTY_BAR_KG.takeIf { equipment == Equipment.BARBELL }
                calc.profile.kind == MaterialKind.PLATES -> calc.profile.baseKg
                calc.profile.kind == MaterialKind.BODYWEIGHT -> 0.0
                // The lightest pin or dumbbell: a stack that starts at 0 has its first pin one step up.
                else -> calc.warmupKg.takeIf { it > 0 } ?: calc.up(0.0)
            }
        }

        /**
         * The default as it is used: the [stored] one, or while it has no value yet the [prefill] (with the stored
         * note). Null when there is nothing to show.
         */
        fun effective(stored: DefaultWarmup?, measure: WarmupMeasure, prefill: Double?): DefaultWarmup? = when {
            stored?.value != null -> stored
            prefill != null -> DefaultWarmup(measure, if (measure == WarmupMeasure.REPS) prefill.roundToInt().toDouble() else prefill, stored?.note)
            else -> stored
        }

        /** The value of a warm-up set in [measure]: its reps on reps, else its weight. */
        fun valueOf(measure: WarmupMeasure, weightKg: Double?, reps: Int?): Double? =
            if (measure == WarmupMeasure.REPS) reps?.toDouble() else weightKg

        /**
         * Whether the link "Bewaar … als standaard" shows under a warm-up set (#93, variant B): the set has a value
         * ([entered]) and it differs from the [current] default (or there is no default value yet).
         */
        fun offersSave(current: DefaultWarmup?, measure: WarmupMeasure, entered: Double?): Boolean {
            if (entered == null) return false
            val now = current?.value ?: return true
            return if (measure == WarmupMeasure.REPS) entered.roundToInt() != now.roundToInt() else abs(entered - now) > 1e-6
        }
    }
}
