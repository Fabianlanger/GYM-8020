package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.logic.Warmup
import nl.ibs.gym8020.core.material.MaterialKind
import nl.ibs.gym8020.core.material.MaterialProfile
import nl.ibs.gym8020.core.material.PlateStock
import nl.ibs.gym8020.core.material.WeightCalculator
import nl.ibs.gym8020.core.model.LoadType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Rules that must always hold (TESTING.md §2), plus the bugs they came from (TESTING.md §1).
 * Material supports, it never limits (D-79).
 */
class MaterialRulesTest {
    private val cable = MaterialProfile(MaterialKind.STACK, minKg = 2.5, stepKg = 2.5)

    private val profiles = listOf(
        MaterialProfile(MaterialKind.PLATES, baseKg = 20.0, loadingPoints = 2),
        MaterialProfile(MaterialKind.PLATES, baseKg = 0.0, loadingPoints = 1),
        cable,
        MaterialProfile(MaterialKind.STACK, minKg = 5.0, stepKg = 5.0),
        MaterialProfile(MaterialKind.DUMBBELLS, minKg = 2.0, stepKg = 2.0),
        MaterialProfile(MaterialKind.BODYWEIGHT, stepKg = 5.0),
    )
    private val stocks = listOf(
        PlateStock.DEFAULT,
        PlateStock(mapOf(20.0 to 2, 10.0 to 2)), // a small home set
    )

    @Test
    fun `O-2 plus above the cable maximum keeps going up (85 must not become 80)`() {
        val calc = WeightCalculator(cable, PlateStock.DEFAULT)
        assertEquals(87.5, calc.up(85.0))
        assertEquals(82.5, calc.up(80.0))
        assertEquals(82.5, calc.down(85.0))
    }

    @Test
    fun `rule - plus never lowers and minus never raises, for any weight and material`() {
        val random = Random(8020)
        for (profile in profiles) for (stock in stocks) {
            val calc = WeightCalculator(profile, stock)
            repeat(2_000) {
                val w = (random.nextDouble(-80.0, 400.0) * 4).toInt() / 4.0
                val up = calc.up(w)
                val down = calc.down(w)
                assertTrue(up > w - 1e-9, "up($w) = $up for $profile")
                assertTrue(down < w + 1e-9, "down($w) = $down for $profile")
                if (profile.kind != MaterialKind.BODYWEIGHT && w >= 0.0) assertTrue(down >= 0.0, "down($w) = $down below zero for $profile")
            }
        }
    }

    @Test
    fun `rule #71 - stacked plates, heavy to light only takes off, for any reachable weights`() {
        val random = Random(71)
        // Six of each per side: enough for up to five different weights, each step needing at most one of a size.
        val plenty = PlateStock(mapOf(20.0 to 12, 15.0 to 12, 10.0 to 12, 5.0 to 12, 2.5 to 12, 1.25 to 12))
        val bars = profiles.filter { it.kind == MaterialKind.PLATES }
        for (profile in bars) for (stock in stocks + plenty) {
            val calc = WeightCalculator(profile, stock)
            // Weights up to what the stock can stack in any case: half the plates per side.
            val reachable = calc.achievable.filter { it <= profile.baseKg + (calc.achievable.last() - profile.baseKg) / 2 }
            repeat(300) {
                val weights = List(random.nextInt(1, 6)) { reachable[random.nextInt(reachable.size)] }
                val plan = assertNotNull(calc.plan(weights), "$weights $profile $stock")
                plan.forEachIndexed { i, step ->
                    assertEquals(weights[i], profile.baseKg + step.loading.perSideKg * profile.loadingPoints, 1e-6, "$weights")
                    if (i > 0 && weights[i] < weights[i - 1] && step.stacked) {
                        assertTrue(step.add.isEmpty(), "heavy → light puts on ${step.add} for $weights with $stock")
                        // What comes off is the outside of the previous stack, outer first.
                        assertEquals(plan[i - 1].loading.plates.reversed().take(step.remove.size), step.remove, "$weights")
                    }
                }
                if (stock == plenty) assertTrue(plan.all { it.stacked }, "not stacked: $weights")
            }
        }
    }

    @Test
    fun `rule - inside the range plus and minus move to an achievable weight`() {
        for (profile in profiles) for (stock in stocks) {
            val calc = WeightCalculator(profile, stock)
            val range = calc.achievable
            for (w in range.dropLast(1)) assertTrue(calc.isAchievable(calc.up(w)), "up($w) for $profile")
            for (w in range.drop(1)) assertTrue(calc.isAchievable(calc.down(w)), "down($w) for $profile")
        }
    }

    @Test
    fun `B-4 pull-ups warm up with assistance, half the body weight by default`() {
        val assisted = MaterialProfile(MaterialKind.BODYWEIGHT, stepKg = 5.0)
        val calc = WeightCalculator(assisted, PlateStock.DEFAULT)
        assertTrue(Warmup.needsAssistance(LoadType.BODYWEIGHT, 1.0))
        assertTrue(Warmup.needsAssistance(LoadType.BODYWEIGHT, 0.95))
        assertFalse(Warmup.needsAssistance(LoadType.BODYWEIGHT, 0.65)) // push-ups: bodyweight only is fine
        assertFalse(Warmup.needsAssistance(LoadType.EXTERNAL, 1.0))
        // 80 kg × 1.0 × 50% = 40 kg assistance, on the machine's 5 kg steps
        assertEquals(-40.0, Warmup.assistKg(80.0, 1.0, 50, calc))
        // 83 kg → 41.5 → nearest step 40
        assertEquals(-40.0, Warmup.assistKg(83.0, 1.0, 50, calc))
        // without a machine profile: the plain number
        assertEquals(-41.5, Warmup.assistKg(83.0, 1.0, 50, null))
        assertNull(Warmup.assistKg(null, 1.0, 50, calc))
    }
}
