package nl.ibs.gym8020.core.material

import nl.ibs.gym8020.core.logic.WeightSteps
import kotlin.math.abs
import kotlin.math.roundToInt

/** How the weight of an exercise is built up (D-26). */
enum class MaterialKind {
    /** Bar or plate-loaded machine: base weight plus plates on 1 or 2 loading points. */
    PLATES,

    /** Pin-stack machine or cable: min and step. */
    STACK,

    /** Fixed dumbbell range; the value is the weight per dumbbell. */
    DUMBBELLS,

    /** Bodyweight: assistance (negative) in steps, extra weight (positive) with plates on a belt. */
    BODYWEIGHT,
}

/**
 * A reusable material profile (D-27), e.g. "Olympic bar 20 kg" or "Pin stack 5 kg".
 * Plate-based kinds use the gym's plate stock. There is no maximum (#6, schema v3): material never limits.
 */
data class MaterialProfile(
    val kind: MaterialKind,
    val baseKg: Double = 0.0,
    val loadingPoints: Int = 2,
    val minKg: Double = 0.0,
    val stepKg: Double = 5.0,
)

/** The gym's plates: size in kg → number of pieces in total. */
data class PlateStock(val plates: Map<Double, Int>) {
    fun encode(): String = plates.entries.sortedByDescending { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    companion object {
        /** Common gym set (D-27): 25 / 20 / 15 / 10 / 5 / 2,5 / 1,25 kg. */
        val DEFAULT = PlateStock(mapOf(25.0 to 4, 20.0 to 4, 15.0 to 2, 10.0 to 4, 5.0 to 4, 2.5 to 4, 1.25 to 4))

        fun decode(text: String?): PlateStock {
            if (text.isNullOrBlank()) return DEFAULT
            val map = text.split(',').mapNotNull { part ->
                val (k, v) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                val kg = k.toDoubleOrNull() ?: return@mapNotNull null
                val n = v.toIntOrNull() ?: return@mapNotNull null
                if (kg > 0 && n >= 0) kg to n else null
            }.toMap()
            return if (map.isEmpty()) DEFAULT else PlateStock(map)
        }
    }
}

/** Plates per loading point, ordered from inside (first on) to outside (last on). */
data class Loading(val plates: List<Double>) {
    val perSideKg: Double get() = plates.sum()
}

/**
 * Loading instructions for a sequence of sets (D-24): per set the plates per side (inside → outside), and what
 * to take off (outer plate first) / put on coming from the previous set. [stacked] (#71): the plates are stacked
 * (GLOSSARY "Stapelen"), so heavy → light only takes plates off; false when the plate stock cannot do that and
 * the plan falls back to the fewest changes.
 */
data class LoadStep(
    val totalKg: Double,
    val loading: Loading,
    val remove: List<Double>,
    val add: List<Double>,
    val stacked: Boolean = true,
)

private fun g(kg: Double): Int = (kg * 1000).roundToInt()
private fun kg(grams: Int): Double = grams / 1000.0

/** Weight calculations for one profile with the gym's plate stock. */
class WeightCalculator(val profile: MaterialProfile, private val stock: PlateStock) : WeightSteps {

    private val sizes: List<Int> = stock.plates.keys.map(::g).sortedDescending()

    /** Pieces available per loading point for each size (same order as [sizes]). */
    private val perSide: IntArray by lazy {
        val lp = when (profile.kind) {
            MaterialKind.PLATES -> profile.loadingPoints.coerceAtLeast(1)
            else -> 1
        }
        IntArray(sizes.size) { i -> (stock.plates.entries.first { g(it.key) == sizes[i] }.value) / lp }
    }

    private val loadingPoints: Int get() = if (profile.kind == MaterialKind.PLATES) profile.loadingPoints.coerceAtLeast(1) else 1

    /** All plate combinations per side, grouped by their weight in grams. Capped for sanity. */
    private val combos: Map<Int, List<IntArray>> by lazy {
        val out = HashMap<Int, MutableList<IntArray>>()
        val counts = IntArray(sizes.size)
        var produced = 0
        fun rec(i: Int, sum: Int) {
            if (produced > 200_000) return
            if (i == sizes.size) {
                out.getOrPut(sum) { mutableListOf() }.add(counts.copyOf())
                produced++
                return
            }
            for (n in 0..perSide[i]) {
                counts[i] = n
                rec(i + 1, sum + n * sizes[i])
            }
            counts[i] = 0
        }
        rec(0, 0)
        out
    }

    /** All achievable weights, ascending (D-28). For bodyweight: signed extra weight. */
    val achievable: List<Double> by lazy {
        when (profile.kind) {
            MaterialKind.PLATES -> combos.keys.map { kg(g(profile.baseKg) + it * loadingPoints) }.distinct().sorted()
            MaterialKind.STACK, MaterialKind.DUMBBELLS -> range(profile.minKg, RANGE_KG, profile.stepKg)
            MaterialKind.BODYWEIGHT -> {
                val assist = if (profile.stepKg > 0) range(profile.stepKg, MAX_ASSIST_KG, profile.stepKg).map { -it } else emptyList()
                val extra = combos.keys.filter { it > 0 }.map(::kg)
                (assist + 0.0 + extra).distinct().sorted()
            }
        }
    }

    private fun range(from: Double, to: Double, step: Double): List<Double> {
        if (step <= 0 || to < from) return listOf(from)
        val n = ((to - from) / step + 1e-9).toInt()
        return (0..n).map { kg(g(from + it * step)) }
    }

    /** Nearest achievable weight; on a tie the lower one. */
    fun snap(weightKg: Double): Double = achievable.minByOrNull { abs(it - weightKg) + if (it > weightKg) 1e-9 else 0.0 } ?: weightKg

    fun isAchievable(weightKg: Double): Boolean = achievable.any { abs(it - weightKg) < 1e-6 }

    /** Step used outside the known range: the smallest plate, or the profile's step (D-79). */
    private val stepAbove: Double
        get() = when (profile.kind) {
            MaterialKind.PLATES -> sizes.lastOrNull()?.let { kg(it * loadingPoints) } ?: profile.stepKg
            MaterialKind.BODYWEIGHT -> sizes.lastOrNull()?.let(::kg) ?: profile.stepKg
            else -> profile.stepKg
        }.takeIf { it > 0 } ?: 1.0

    private val stepBelow: Double
        get() = (if (profile.kind == MaterialKind.PLATES) stepAbove else profile.stepKg).takeIf { it > 0 } ?: 1.0

    /**
     * Next weight up. Inside the profile's range: the next achievable weight. Above it the profile does not
     * limit (D-79): + keeps going in the same step, so 80 → 82.5 → 85 on a cable that "ends" at 80.
     * Never lower than [weightKg].
     */
    override fun up(weightKg: Double): Double {
        val hi = achievable.lastOrNull()
        if (hi == null || weightKg >= hi - 1e-6) return kg(g(weightKg + stepAbove))
        return achievable.first { it > weightKg + 1e-6 }
    }

    /**
     * Next weight down. Inside the range: the previous achievable weight; outside it: one step, never below
     * zero for external weights. Never higher than [weightKg].
     */
    override fun down(weightKg: Double): Double {
        val lo = achievable.firstOrNull()
        val hi = achievable.lastOrNull()
        val next = when {
            lo == null || hi == null -> weightKg - stepBelow
            weightKg > hi + 1e-6 -> maxOf(hi, weightKg - stepAbove)
            weightKg <= lo + 1e-6 -> weightKg - stepBelow
            else -> achievable.last { it < weightKg - 1e-6 }
        }
        val floored = if (profile.kind == MaterialKind.BODYWEIGHT) next else maxOf(0.0, next)
        return minOf(weightKg, kg(g(floored)))
    }

    /** Weight for a warm-up (D-44): empty bar, lowest pin, lightest dumbbell, or bodyweight only. */
    val warmupKg: Double
        get() = when (profile.kind) {
            MaterialKind.PLATES -> profile.baseKg
            MaterialKind.STACK, MaterialKind.DUMBBELLS -> profile.minKg
            MaterialKind.BODYWEIGHT -> 0.0
        }

    private fun perSideGrams(totalKg: Double): Int? {
        val grams = when (profile.kind) {
            MaterialKind.PLATES -> {
                val rest = g(totalKg) - g(profile.baseKg)
                if (rest < 0 || rest % loadingPoints != 0) return null
                rest / loadingPoints
            }
            MaterialKind.BODYWEIGHT -> if (totalKg > 0) g(totalKg) else return null
            else -> return null
        }
        return grams.takeIf { combos.containsKey(it) }
    }

    private fun toLoading(c: IntArray): Loading =
        Loading(c.indices.flatMap { i -> List(c[i]) { kg(sizes[i]) } })

    private fun pieces(c: IntArray) = c.sum()

    private fun changes(from: IntArray, to: IntArray): Pair<IntArray, IntArray> {
        val remove = IntArray(from.size) { i -> maxOf(0, from[i] - to[i]) }
        val add = IntArray(from.size) { i -> maxOf(0, to[i] - from[i]) }
        return remove to add
    }

    /**
     * Loading plan for consecutive sets, in the order they are done. The plates are stacked (#71, GLOSSARY
     * "Stapelen"): the lightest weight's plates sit innermost and each heavier weight only adds plates outside,
     * so heavy → light only takes plates off. Each step adds as few plates as possible; on a tie the stack with
     * the fewest different plate sizes wins (fewer plates to fetch). When the stock cannot stack every weight,
     * the plan falls back to the fewest changes between sets ([LoadStep.stacked] false).
     * Returns null for non-plate profiles or unachievable weights.
     */
    fun plan(weightsKg: List<Double>): List<LoadStep>? {
        if (profile.kind != MaterialKind.PLATES && profile.kind != MaterialKind.BODYWEIGHT) return null
        if (weightsKg.isEmpty()) return emptyList()
        val targets = weightsKg.map { perSideGrams(it) ?: if (profile.kind == MaterialKind.BODYWEIGHT && it <= 0) 0 else return null }
        val levels = targets.distinct().sorted()
        val stacks = stack(levels) ?: return fewestChanges(weightsKg, targets)
        val byTarget = levels.indices.associate { levels[it] to stacks[it] }
        return weightsKg.indices.map { i ->
            val now = byTarget.getValue(targets[i])
            val before = if (i == 0) emptyList() else byTarget.getValue(targets[i - 1])
            val remove = if (i > 0 && now.size < before.size) before.drop(now.size).reversed() else emptyList()
            val add = if (i > 0 && now.size > before.size) now.drop(before.size) else emptyList()
            LoadStep(weightsKg[i], Loading(now), remove, add)
        }
    }

    /**
     * Stacks per level (ascending per-side grams), each inside → outside, every stack a prefix of the next; null
     * when the stock cannot. Branch and bound over the plates each level adds.
     */
    private fun stack(levels: List<Int>): List<List<Double>>? {
        val increments = levels.mapIndexed { i, t -> t - (if (i == 0) 0 else levels[i - 1]) }
        val options: List<List<IntArray>> = increments.map { inc ->
            if (inc == 0) return@map listOf(IntArray(sizes.size))
            val all = combos[inc] ?: return null
            val fewest = all.minOf(::pieces)
            all.filter { pieces(it) <= fewest + 2 }
                .sortedWith { a, b -> compareValuesBy(a, b, ::pieces).takeIf { it != 0 } ?: biggerFirst(a, b) }
                .take(MAX_OPTIONS)
        }
        // Lower bound on the plates still to add from level i on.
        val bound = IntArray(options.size + 1)
        for (i in options.indices.reversed()) bound[i] = bound[i + 1] + options[i].minOf(::pieces)

        val chosen = arrayOfNulls<IntArray>(options.size)
        var best: List<IntArray>? = null
        var bestScore = intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        var nodes = 0
        val left = perSide.copyOf()
        fun search(i: Int, plates: Int) {
            if (++nodes > MAX_NODES || plates + bound[i] > bestScore[0]) return
            if (i == options.size) {
                val used = IntArray(sizes.size) { k -> chosen.sumOf { it!![k] } }
                val score = intArrayOf(plates, used.count { it > 0 }, chosen.maxOf { pieces(it!!) })
                if (compareScores(score, bestScore) < 0) { bestScore = score; best = chosen.map { it!! } }
                return
            }
            for (c in options[i]) {
                if (c.indices.any { c[it] > left[it] }) continue
                for (k in c.indices) left[k] -= c[k]
                chosen[i] = c
                search(i + 1, plates + pieces(c))
                for (k in c.indices) left[k] += c[k]
            }
        }
        search(0, 0)
        val steps = best ?: return null
        val out = mutableListOf<List<Double>>()
        var acc = emptyList<Double>()
        for (c in steps) { acc = acc + toLoading(c).plates; out += acc }
        return out
    }

    private fun compareScores(a: IntArray, b: IntArray): Int {
        for (i in a.indices) if (a[i] != b[i]) return a[i].compareTo(b[i])
        return 0
    }

    /** More of the bigger plates first (counts are ordered big → small). */
    private fun biggerFirst(a: IntArray, b: IntArray): Int {
        for (i in a.indices) if (a[i] != b[i]) return b[i].compareTo(a[i])
        return 0
    }

    /**
     * Fallback when the stock cannot stack: the first loading is chosen together with the second so that heavy →
     * light ideally means taking off only the outer plate(s); later sets follow with the fewest changes.
     */
    private fun fewestChanges(weightsKg: List<Double>, targets: List<Int>): List<LoadStep> {
        fun candidates(t: Int) = combos[t] ?: listOf(IntArray(sizes.size))

        // First loading: best pair with the second set (or simply fewest plates).
        val first: IntArray = if (targets.size == 1) {
            candidates(targets[0]).minWith(compareBy(::pieces))
        } else {
            var best: IntArray? = null
            var bestScore = Int.MAX_VALUE
            for (a in candidates(targets[0])) {
                val b = bestTransition(a, candidates(targets[1]))
                val (rem, add) = changes(a, b)
                // Changes first; prefer only removing; then fewer plates on the bar.
                val score = (rem.sum() + add.sum()) * 10_000 + add.sum() * 100 + pieces(a)
                if (score < bestScore) { bestScore = score; best = a }
            }
            best!!
        }
        val steps = mutableListOf<LoadStep>()
        var current = first
        // Plates that stay on for the second set go inside, the ones to take off go outside.
        val firstOrder = if (targets.size > 1) ordered(current, bestTransition(current, candidates(targets[1]))) else ordered(current, null)
        steps += LoadStep(weightsKg[0], firstOrder, emptyList(), emptyList(), stacked = false)
        for (i in 1 until targets.size) {
            val next = bestTransition(current, candidates(targets[i]))
            val (rem, add) = changes(current, next)
            steps += LoadStep(weightsKg[i], ordered(next, current), expand(rem, descending = false), expand(add, descending = true), stacked = false)
            current = next
        }
        return steps
    }

    private fun bestTransition(from: IntArray, options: List<IntArray>): IntArray =
        options.minWith(compareBy<IntArray>({ c -> changes(from, c).let { it.first.sum() + it.second.sum() } }, { pieces(it) }))

    /** Inside → outside: plates shared with the previous loading first (big → small), then the rest. */
    private fun ordered(c: IntArray, previous: IntArray?): Loading {
        if (previous == null) return toLoading(c)
        val shared = IntArray(c.size) { i -> minOf(c[i], previous[i]) }
        val extra = IntArray(c.size) { i -> c[i] - shared[i] }
        return Loading(toLoading(shared).plates + toLoading(extra).plates)
    }

    /** Plates as a list; removal is listed outer (small) first. */
    private fun expand(c: IntArray, descending: Boolean): List<Double> {
        val l = toLoading(c).plates
        return if (descending) l else l.reversed()
    }

    companion object {
        /**
         * How far the listed weights of a stack or dumbbells go. Profiles have no maximum (#6); above this the
         * weight still goes up in the profile's step (D-79).
         */
        const val RANGE_KG = 200.0

        /** The most assistance listed for an assisted machine (signed: down to −100 kg). */
        const val MAX_ASSIST_KG = 100.0

        /** Stacking search limits: plate choices per step and search nodes (#71). */
        private const val MAX_OPTIONS = 40
        private const val MAX_NODES = 200_000
    }
}
