package nl.ibs.gym8020.core

import nl.ibs.gym8020.core.model.Muscle
import nl.ibs.gym8020.core.model.MuscleShare
import nl.ibs.gym8020.core.model.Progression
import nl.ibs.gym8020.core.model.SetRole
import nl.ibs.gym8020.core.stats.MuscleComparison
import nl.ibs.gym8020.core.stats.SetRecord
import nl.ibs.gym8020.core.stats.Trend
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Muscle map in the summary (#67, decision #8 points 2, 3 and 5). */
class MuscleComparisonTest {
    private val today = LocalDate.of(2026, 10, 9)
    private val earlier = LocalDate.of(2026, 10, 1)

    private val bench = 1L
    private val pushdown = 2L
    private val fly = 3L
    private val lateral = 4L
    private val wrist = 5L

    private val muscles = mapOf(
        bench to listOf(MuscleShare(Muscle.CHEST, 100), MuscleShare(Muscle.TRICEPS, 50)),
        pushdown to listOf(MuscleShare(Muscle.TRICEPS, 100)),
        fly to listOf(MuscleShare(Muscle.CHEST, 100), MuscleShare(Muscle.FRONT_DELT, 50)),
        lateral to listOf(MuscleShare(Muscle.SIDE_DELT, 100)),
        wrist to listOf(MuscleShare(Muscle.FOREARMS, 100)),
    )

    /** Order of this session; fly and lateral raise are skipped today. */
    private val planned = listOf(bench, fly, pushdown, lateral, wrist)

    private fun s(session: Long, ex: Long, role: SetRole, w: Double, reps: Int, prog: Progression = Progression.WEIGHT) =
        SetRecord(session, if (session == 2L) today else earlier, ex, role, w, reps, w, null, prog)

    /** Today: no fly, no lateral raise (skipped exercises have no sets in the statistics). */
    private val now = listOf(
        s(2, bench, SetRole.WARMUP, 40.0, 10),
        s(2, bench, SetRole.HEAVY, 100.0, 4),
        s(2, bench, SetRole.HEAVY, 100.0, 4),
        s(2, pushdown, SetRole.HEAVY, 30.0, 12),
        s(2, wrist, SetRole.HEAVY, 20.0, 10),
    )

    /** The previous session of the same workout, which is also the last time of each exercise. */
    private val previous = listOf(
        s(1, bench, SetRole.HEAVY, 90.0, 6),
        s(1, bench, SetRole.HEAVY, 90.0, 6),
        s(1, fly, SetRole.HEAVY, 20.0, 12),
        s(1, fly, SetRole.HEAVY, 20.0, 12),
        s(1, pushdown, SetRole.HEAVY, 30.0, 10),
        s(1, lateral, SetRole.HEAVY, 10.0, 15),
    )
    private val lastTimes = previous.groupBy { it.exerciseId }

    private fun map(prev: List<SetRecord>? = previous, last: Map<Long, List<SetRecord>> = lastTimes) =
        MuscleComparison.of(now, planned, last, prev, muscles)

    // ---- "Vs. last time": strength, only what you did ----

    @Test
    fun `strength per muscle is the share-weighted change of what you did today`() {
        val m = map().strength
        // bench 100 × 4 = e1RM 113,33 against 90 × 6 = 108: +4,94 %; the skipped fly does not count
        assertEquals(0.0494, m.getValue(Muscle.CHEST).change!!, 1e-4)
        assertEquals(Trend.UP, m.getValue(Muscle.CHEST).trend)
        assertFalse(m.getValue(Muscle.CHEST).skipped)
        // triceps: bench +4,94 % at 50 %, pushdown 30 × 12 = 42 against 30 × 10 = 40: +5 % at 100 %
        assertEquals(0.0498, m.getValue(Muscle.TRICEPS).change!!, 1e-4)
    }

    @Test
    fun `a muscle only trained by skipped exercises is skipped, not equal`() {
        val m = map().strength
        val delt = m.getValue(Muscle.FRONT_DELT)
        assertTrue(delt.skipped)
        assertNull(delt.change)
        assertNull(delt.trend)
        assertTrue(m.getValue(Muscle.SIDE_DELT).skipped)
    }

    @Test
    fun `a muscle trained today without an earlier time has no comparison and is not skipped`() {
        val forearms = map().strength.getValue(Muscle.FOREARMS)
        assertNull(forearms.change)
        assertNull(forearms.trend)
        assertFalse(forearms.skipped)
    }

    @Test
    fun `a muscle nobody trains is absent`() {
        assertNull(map().strength[Muscle.ABS])
        assertNull(map().volume[Muscle.ABS])
        assertNull(map().breakdown[Muscle.ABS])
    }

    @Test
    fun `within 1 percent counts as equal, as per exercise`() {
        // bench 100 × 4 = 113,33 against 90 × 8 = 114: −0,6 %
        val chest = map(last = lastTimes + (bench to listOf(s(1, bench, SetRole.HEAVY, 90.0, 8)))).strength.getValue(Muscle.CHEST)
        assertEquals(-0.0058, chest.change!!, 1e-4)
        assertEquals(Trend.SAME, chest.trend)
        // 100 × 6 = 120: −5,6 %
        val worse = map(last = lastTimes + (bench to listOf(s(1, bench, SetRole.HEAVY, 100.0, 6)))).strength.getValue(Muscle.CHEST)
        assertEquals(Trend.DOWN, worse.trend)
    }

    // ---- "Totaal": volume against the previous session of this workout ----

    @Test
    fun `volume per muscle against the previous session, skipped exercises as 0`() {
        val v = map().volume
        // chest: bench 2 × 400 = 800 against bench 2 × 540 + fly 2 × 240 = 1.560
        val chest = v.getValue(Muscle.CHEST)
        assertEquals(800.0, chest.now, 1e-9)
        assertEquals(-760.0, chest.change!!.absolute, 1e-9)
        assertEquals(-0.4872, chest.change!!.relative!!, 1e-4)
        assertEquals(Trend.DOWN, chest.trend)
        assertFalse(chest.skipped)
        // triceps: 800 × 50 % + 360 = 760 against 1.080 × 50 % + 300 = 840
        assertEquals(760.0, v.getValue(Muscle.TRICEPS).now, 1e-9)
        assertEquals(-80.0, v.getValue(Muscle.TRICEPS).change!!.absolute, 1e-9)
        // front delt only via the skipped fly: 0 against 240, −100 %
        val delt = v.getValue(Muscle.FRONT_DELT)
        assertEquals(0.0, delt.now, 1e-9)
        assertEquals(-240.0, delt.change!!.absolute, 1e-9)
        assertEquals(-1.0, delt.change!!.relative!!, 1e-9)
        assertTrue(delt.skipped)
        // forearms: new in this workout, 200 against 0 has no percentage
        val forearms = v.getValue(Muscle.FOREARMS)
        assertEquals(200.0, forearms.change!!.absolute, 1e-9)
        assertNull(forearms.change!!.relative)
        assertEquals(Trend.UP, forearms.trend)
    }

    @Test
    fun `volume without a previous session of this workout has no change`() {
        val chest = map(prev = null).volume.getValue(Muscle.CHEST)
        assertEquals(800.0, chest.now, 1e-9)
        assertNull(chest.change)
        assertNull(chest.trend)
    }

    @Test
    fun `a muscle only trained in the previous session shows its drop`() {
        // lateral raise left out of today's session altogether (not planned): side delts 0 against 150, not skipped
        val v = MuscleComparison.of(now, listOf(bench, fly, pushdown, wrist), lastTimes, previous, muscles).volume
        val side = v.getValue(Muscle.SIDE_DELT)
        assertEquals(-150.0, side.change!!.absolute, 1e-9)
        assertFalse(side.skipped)
    }

    // ---- tap a muscle: which exercises count, and which you skipped ----

    @Test
    fun `breakdown lists the exercises that count with their numbers, and the skipped ones`() {
        val chest = map().breakdown.getValue(Muscle.CHEST)
        assertEquals(listOf(bench), chest.strength.map { it.exerciseId })
        assertEquals(100, chest.strength[0].percent)
        assertEquals(5.333, chest.strength[0].comparison.change!!.absolute, 1e-3)
        assertEquals(listOf(bench), chest.volume.map { it.exerciseId })
        assertEquals(800.0, chest.volume[0].kg, 1e-9)
        assertEquals(1.0, chest.volume[0].share!!, 1e-9)
        assertEquals(listOf(fly), chest.skipped)
    }

    @Test
    fun `breakdown follows the session order and splits the volume by share`() {
        val triceps = map().breakdown.getValue(Muscle.TRICEPS)
        assertEquals(listOf(bench, pushdown), triceps.strength.map { it.exerciseId })
        assertEquals(listOf(50, 100), triceps.strength.map { it.percent })
        assertEquals(0.05, triceps.strength[1].comparison.change!!.relative!!, 1e-9)
        assertEquals(listOf(400.0, 360.0), triceps.volume.map { it.kg })
        // 400 / 760 and 360 / 760
        assertEquals(0.5263, triceps.volume[0].share!!, 1e-4)
        assertEquals(0.4737, triceps.volume[1].share!!, 1e-4)
        assertEquals(emptyList(), triceps.skipped)
    }

    @Test
    fun `breakdown of a skipped muscle has only the skipped exercises`() {
        val delt = map().breakdown.getValue(Muscle.FRONT_DELT)
        assertEquals(emptyList(), delt.strength)
        assertEquals(emptyList(), delt.volume)
        assertEquals(listOf(fly), delt.skipped)
    }

    @Test
    fun `reps progression counts in reps for the muscle`() {
        val crunch = 6L
        val shares = muscles + (crunch to listOf(MuscleShare(Muscle.ABS, 100)))
        val t = listOf(s(2, crunch, SetRole.HEAVY, 0.0, 22, Progression.REPS))
        val last = mapOf(crunch to listOf(s(1, crunch, SetRole.HEAVY, 0.0, 20, Progression.REPS)))
        val abs = MuscleComparison.of(t, listOf(crunch), last, null, shares)
        assertEquals(0.1, abs.strength.getValue(Muscle.ABS).change!!, 1e-9)
        // no weight, so no volume
        assertEquals(0.0, abs.volume.getValue(Muscle.ABS).now, 1e-9)
        assertEquals(0.0, abs.breakdown.getValue(Muscle.ABS).volume[0].kg, 1e-9)
        assertNull(abs.breakdown.getValue(Muscle.ABS).volume[0].share)
        assertEquals(2.0, abs.breakdown.getValue(Muscle.ABS).strength[0].comparison.change!!.absolute, 1e-9)
    }
}
