package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.material.WeightCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MaterialTest {
    private val bar = MaterialProfile(MaterialKind.PLATES, baseKg = 20.0, loadingPoints = 2)
    private val stock = PlateStock.DEFAULT
    private val calc = WeightCalculator(bar, stock)

    @Test
    fun `barbell achievable weights`() {
        assertEquals(20.0, calc.achievable.first())
        assertTrue(calc.isAchievable(22.5))
        assertTrue(calc.isAchievable(60.0))
        assertFalse(calc.isAchievable(61.0))
        assertEquals(62.5, calc.up(60.0))
        assertEquals(57.5, calc.down(60.0))
        assertEquals(60.0, calc.snap(60.9))
        assertEquals(20.0, calc.warmupKg)
    }

    @Test
    fun `heavy to light by taking one plate off per side (spec example)`() {
        val plan = assertNotNull(calc.plan(listOf(60.0, 40.0)))
        assertEquals(20.0, plan[0].loading.perSideKg)
        assertEquals(20.0 - 10.0, plan[1].loading.perSideKg)
        assertEquals(listOf(10.0), plan[1].remove)
        assertTrue(plan[1].add.isEmpty())
        // the plate that comes off is the outer one
        assertEquals(10.0, plan[0].loading.plates.last())
    }

    @Test
    fun `prefers a loading that only needs removal`() {
        // 100 kg heavy (40/side), 70 kg light (25/side): 25 + 15 → take off 15
        val plan = assertNotNull(calc.plan(listOf(100.0, 70.0)))
        assertEquals(listOf(15.0), plan[1].remove)
        assertTrue(plan[1].add.isEmpty())
        assertEquals(15.0, plan[0].loading.plates.last())
    }

    @Test
    fun `unachievable weight gives no plan`() {
        assertNull(calc.plan(listOf(61.0)))
    }

    @Test
    fun `stack and dumbbells`() {
        val stack = WeightCalculator(MaterialProfile(MaterialKind.STACK, minKg = 5.0, stepKg = 5.0, maxKg = 100.0), stock)
        assertEquals(5.0, stack.achievable.first())
        assertEquals(100.0, stack.achievable.last())
        assertEquals(50.0, stack.up(45.0))
        assertEquals(5.0, stack.warmupKg)
        assertNull(stack.plan(listOf(50.0)))
        val db = WeightCalculator(MaterialProfile(MaterialKind.DUMBBELLS, minKg = 2.0, stepKg = 2.0, maxKg = 40.0), stock)
        assertEquals(24.0, db.up(22.0))
        // Below the lightest dumbbell − still works (D-79: material never limits), but never below zero.
        assertEquals(0.0, db.down(2.0))
        assertEquals(0.0, db.down(0.0))
    }

    @Test
    fun `bodyweight assistance and belt`() {
        val bw = WeightCalculator(MaterialProfile(MaterialKind.BODYWEIGHT, stepKg = 5.0, maxKg = 50.0), stock)
        assertEquals(-50.0, bw.achievable.first())
        assertTrue(bw.isAchievable(0.0))
        assertTrue(bw.isAchievable(-20.0))
        assertTrue(bw.isAchievable(2.5))
        assertEquals(-15.0, bw.up(-20.0))
        assertEquals(1.25, bw.up(0.0))
        assertEquals(0.0, bw.warmupKg)
    }

    @Test
    fun `plate stock round trip`() {
        assertEquals(PlateStock.DEFAULT, PlateStock.decode(PlateStock.DEFAULT.encode()))
        assertEquals(PlateStock.DEFAULT, PlateStock.decode("garbage"))
    }
}

class MaterialGoalTest {
    @Test
    fun `suggestion uses the next achievable weight`() {
        val stack = WeightCalculator(MaterialProfile(MaterialKind.STACK, minKg = 5.0, stepKg = 5.0, maxKg = 100.0), PlateStock.DEFAULT)
        val plan = nl.ibs.gym8020.core.logic.GoalAdvisor.planNext(
            listOf(nl.ibs.gym8020.core.logic.PerformedSet(nl.ibs.gym8020.core.model.SetRole.HEAVY, 45.0, 10)),
            nl.ibs.gym8020.core.model.LoadType.EXTERNAL, nl.ibs.gym8020.core.model.Progression.WEIGHT,
            nl.ibs.gym8020.core.model.MethodSettings(), stack,
        )
        assertEquals(50.0, plan.single().weightKg)
    }
}
