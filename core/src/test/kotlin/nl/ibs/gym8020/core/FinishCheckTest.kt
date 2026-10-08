package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.FinishCheck
import nl.ibs.gym8020.core.logic.FinishExercise
import nl.ibs.gym8020.core.logic.Finding
import nl.ibs.gym8020.core.logic.FindingKind
import nl.ibs.gym8020.core.logic.Missing
import nl.ibs.gym8020.core.logic.PlannedSet
import nl.ibs.gym8020.core.logic.FinishSet
import nl.ibs.gym8020.core.model.LoadType
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import kotlin.test.Test
import kotlin.test.assertEquals

class FinishCheckTest {

    private fun squat(vararg sets: FinishSet) = FinishExercise(
        sessionExerciseId = 10, exerciseId = 1,
        progression = Progression.WEIGHT, loadType = LoadType.EXTERNAL,
        sets = sets.toList(),
    )

    @Test
    fun `weight without reps blocks finishing`() {
        val findings = FinishCheck.check(listOf(squat(FinishSet(SetRole.HEAVY, 100.0, null))))
        assertEquals(listOf(Finding(FindingKind.BLOCK, 10, 1, SetRole.HEAVY, 0, Missing.REPS)), findings)
    }

    @Test
    fun `reps without weight blocks finishing`() {
        val findings = FinishCheck.check(listOf(squat(FinishSet(SetRole.HEAVY, 100.0, 6), FinishSet(SetRole.LIGHT, null, 15))))
        assertEquals(listOf(Finding(FindingKind.BLOCK, 10, 1, SetRole.LIGHT, 1, Missing.WEIGHT)), findings)
    }

    @Test
    fun `reps progression and bodyweight need only reps`() {
        val pushUp = squat(FinishSet(SetRole.HEAVY, null, 20)).copy(progression = Progression.REPS)
        val pullUp = squat(FinishSet(SetRole.HEAVY, null, 8)).copy(loadType = LoadType.BODYWEIGHT)
        assertEquals(emptyList(), FinishCheck.check(listOf(pushUp, pullUp)))
    }

    @Test
    fun `reps progression still needs reps when a weight is entered`() {
        val pushUp = squat(FinishSet(SetRole.HEAVY, 10.0, null)).copy(progression = Progression.REPS)
        assertEquals(listOf(Finding(FindingKind.BLOCK, 10, 1, SetRole.HEAVY, 0, Missing.REPS)), FinishCheck.check(listOf(pushUp)))
    }

    @Test
    fun `an empty work set will be skipped`() {
        val findings = FinishCheck.check(listOf(squat(FinishSet(SetRole.HEAVY, 100.0, 6), FinishSet(SetRole.LIGHT, null, null))))
        assertEquals(listOf(Finding(FindingKind.SKIP, 10, 1, SetRole.LIGHT, 1, Missing.ALL)), findings)
    }

    @Test
    fun `an empty warm-up is not listed`() {
        assertEquals(emptyList(), FinishCheck.check(listOf(squat(FinishSet(SetRole.WARMUP, null, null), FinishSet(SetRole.HEAVY, 100.0, 6)))))
    }

    @Test
    fun `a weight equal to your target without reps blocks`() {
        val findings = FinishCheck.check(listOf(squat(FinishSet(SetRole.HEAVY, 100.0, null))))
        assertEquals(listOf(Finding(FindingKind.BLOCK, 10, 1, SetRole.HEAVY, 0, Missing.REPS)), findings)
    }

    @Test
    fun `an empty bodyweight set will be skipped`() {
        val pullUp = squat(FinishSet(SetRole.HEAVY, null, null)).copy(loadType = LoadType.BODYWEIGHT)
        assertEquals(listOf(Finding(FindingKind.SKIP, 10, 1, SetRole.HEAVY, 0, Missing.ALL)), FinishCheck.check(listOf(pullUp)))
    }

    @Test
    fun `an entered extra of zero without reps blocks`() {
        val pullUp = squat(FinishSet(SetRole.HEAVY, 0.0, null)).copy(loadType = LoadType.BODYWEIGHT)
        assertEquals(listOf(Finding(FindingKind.BLOCK, 10, 1, SetRole.HEAVY, 0, Missing.REPS)), FinishCheck.check(listOf(pullUp)))
    }

    @Test
    fun `a skipped exercise is ignored`() {
        val skipped = squat(FinishSet(SetRole.HEAVY, 100.0, null), FinishSet(SetRole.LIGHT, null, null)).copy(skipped = true)
        assertEquals(emptyList(), FinishCheck.check(listOf(skipped)))
    }

    @Test
    fun `a work set without weight in your target warns`() {
        val ex = squat(FinishSet(SetRole.HEAVY, 100.0, 6), FinishSet(SetRole.LIGHT, 70.0, 15)).copy(
            target = listOf(PlannedSet(SetRole.WARMUP, null), PlannedSet(SetRole.HEAVY, 102.5), PlannedSet(SetRole.LIGHT, null)),
        )
        assertEquals(listOf(Finding(FindingKind.WARN, 10, 1, SetRole.LIGHT, 2, Missing.WEIGHT)), FinishCheck.check(listOf(ex)))
    }

    @Test
    fun `a target without weight is fine when weight is optional`() {
        val pushUp = squat(FinishSet(SetRole.HEAVY, null, 20)).copy(progression = Progression.REPS, target = listOf(PlannedSet(SetRole.HEAVY, null)))
        assertEquals(emptyList(), FinishCheck.check(listOf(pushUp)))
    }
}
