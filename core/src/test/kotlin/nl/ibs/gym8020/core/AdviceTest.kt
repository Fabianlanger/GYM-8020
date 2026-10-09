package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Advice
import nl.ibs.gym8020.core.logic.PerformedSet
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.WeightSteps
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.MethodSettings
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole.HEAVY
import nl.ibs.gym8020.core.model.SetRole.LIGHT
import nl.ibs.gym8020.core.model.SetRole.WARMUP
import kotlin.test.Test
import kotlin.test.assertEquals

/** #63: the advice next to your target, only where it differs from it. */
class AdviceTest {
    private val method = MethodSettings()

    private fun squat(target: List<PlannedSet>, vararg today: PerformedSet) =
        Advice.perSet(target, today.toList(), Progression.WEIGHT, LoadType.EXTERNAL, method::targetFor, method.goodMargin, method.weightStepKg)

    @Test
    fun `a set that was too light gets advice one step heavier`() {
        val advice = squat(
            listOf(PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 60.0)),
            PerformedSet(HEAVY, 80.0, 9), PerformedSet(LIGHT, 60.0, 16),
        )
        assertEquals(listOf(82.5, null), advice)
    }

    @Test
    fun `a set that was too heavy gets advice one step lighter on the material`() {
        // Dumbbells in 2 kg steps: one step down from 24 is 22.
        val dumbbells = object : WeightSteps {
            override fun up(weightKg: Double) = weightKg + 2.0
            override fun down(weightKg: Double) = weightKg - 2.0
        }
        val advice = Advice.perSet(
            listOf(PlannedSet(HEAVY, 24.0), PlannedSet(LIGHT, 14.0)),
            listOf(PerformedSet(HEAVY, 24.0, 4), PerformedSet(LIGHT, 14.0, 13)),
            Progression.WEIGHT, LoadType.EXTERNAL, method::targetFor, method.goodMargin, method.weightStepKg, dumbbells,
        )
        assertEquals(listOf(22.0, 12.0), advice)
    }

    @Test
    fun `no advice before the reps of that set are entered`() {
        val advice = squat(
            listOf(PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 60.0)),
            PerformedSet(HEAVY, 80.0, 9), PerformedSet(LIGHT, 60.0, null),
        )
        assertEquals(listOf(82.5, null), advice)
    }

    @Test
    fun `no advice when your target already is the advice`() {
        val advice = squat(
            listOf(PlannedSet(HEAVY, 82.5), PlannedSet(LIGHT, 60.0)),
            PerformedSet(HEAVY, 80.0, 9), PerformedSet(LIGHT, 60.0, 16),
        )
        assertEquals(listOf(null, null), advice)
    }

    @Test
    fun `warm-ups get no advice and work sets match by role`() {
        val advice = squat(
            listOf(PlannedSet(WARMUP, null), PlannedSet(HEAVY, 80.0), PlannedSet(LIGHT, 60.0), PlannedSet(HEAVY, 75.0)),
            PerformedSet(WARMUP, null, 10), PerformedSet(HEAVY, 80.0, 6), PerformedSet(LIGHT, 60.0, 19), PerformedSet(HEAVY, 75.0, 3),
        )
        assertEquals(listOf(null, null, 62.5, 72.5), advice)
    }

    @Test
    fun `advice compares with your target, not with what you lifted`() {
        // You lifted 80 × 6 (good) but set 85 as your target: the advice is 80.
        val advice = squat(listOf(PlannedSet(HEAVY, 85.0)), PerformedSet(HEAVY, 80.0, 6))
        assertEquals(listOf(80.0), advice)
    }

    @Test
    fun `bodyweight exercises advise on the signed load`() {
        // Pull-up with 20 kg assistance, too light: one step less assistance.
        val advice = Advice.perSet(
            listOf(PlannedSet(HEAVY, -20.0)),
            listOf(PerformedSet(HEAVY, -20.0, 10)),
            Progression.WEIGHT, LoadType.BODYWEIGHT, method::targetFor, method.goodMargin, method.weightStepKg,
        )
        assertEquals(listOf(-17.5), advice)
    }

    @Test
    fun `reps progression gets no advice`() {
        val advice = Advice.perSet(
            listOf(PlannedSet(HEAVY, null), PlannedSet(LIGHT, null)),
            listOf(PerformedSet(HEAVY, null, 12), PerformedSet(LIGHT, null, 25)),
            Progression.REPS, LoadType.BODYWEIGHT, method::targetFor, method.goodMargin, method.weightStepKg,
        )
        assertEquals(listOf(null, null), advice)
    }
}
