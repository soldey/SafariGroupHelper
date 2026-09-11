package me.kmsold.safarigrouphelper.data

import com.google.gson.GsonBuilder
import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.SghConfigGui
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class StatsState(
    /** Fastest 100% completion of all four biomes, in milliseconds. 0 = never completed. */
    var bestCompletionMs: Long = 0L,
    var runsStarted: Int = 0,
    var runsCompleted: Int = 0,
    /** Lifetime repeat catches per critter - not used for progress, kept for future features. */
    var duplicates: MutableMap<String, Int> = LinkedHashMap(),
    /** The most recent runs, newest first, capped at [RunHistory.MAX_ENTRIES]. */
    var runs: MutableList<RunRecord> = ArrayList(),
    /** Fastest clear of a single biome, keyed by [CritterBiome.name]. */
    var biomeBest: MutableMap<String, Long> = LinkedHashMap(),
)

/** Long-lived statistics that survive across runs: personal best and lifetime duplicate counts. */
object SafariStats {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file by lazy { ConfigManager.configDir.resolve("stats.json") }

    var state: StatsState = StatsState()
        private set

    private var dirty = false

    fun load() {
        state = if (file.exists()) {
            runCatching { gson.fromJson(file.readText(), StatsState::class.java) }
                .onFailure { SafariGroupHelper.logger.error("Could not read stats.json", it) }
                .getOrNull() ?: StatsState()
        } else {
            StatsState()
        }
        @Suppress("SENSELESS_COMPARISON")
        if (state.duplicates == null) state.duplicates = LinkedHashMap()
        @Suppress("SENSELESS_COMPARISON")
        if (state.runs == null) state.runs = ArrayList()
        @Suppress("SENSELESS_COMPARISON")
        if (state.biomeBest == null) state.biomeBest = LinkedHashMap()
    }

    fun addDuplicate(critter: String) {
        state.duplicates[critter] = (state.duplicates[critter] ?: 0) + 1
        dirty = true
    }

    /** Files a finished run into the history and drops anything past the tenth. */
    fun recordRun(record: RunRecord) {
        state.runs.add(0, record)
        while (state.runs.size > RunHistory.MAX_ENTRIES) state.runs.removeAt(state.runs.size - 1)
        save()
        // The history is baked into the settings screen when it is built.
        SghConfigGui.invalidate()
    }

    fun runStarted() {
        state.runsStarted++
        dirty = true
    }

    /**
     * Records a finished 100% run.
     * @return the previous personal best in ms, or null when this is the very first completion.
     */
    fun recordCompletion(durationMs: Long): Long? {
        state.runsCompleted++
        val previous = state.bestCompletionMs.takeIf { it > 0 }
        if (previous == null || durationMs < previous) {
            state.bestCompletionMs = durationMs
        }
        dirty = true
        save()
        return previous
    }

    val personalBestMs: Long? get() = state.bestCompletionMs.takeIf { it > 0 }

    fun biomeBestMs(biome: CritterBiome): Long? = state.biomeBest[biome.name]?.takeIf { it > 0 }

    /**
     * Records how long it took to clear one biome.
     * @return the previous best for that biome, or null when this is the first clear.
     */
    fun recordBiomeClear(biome: CritterBiome, durationMs: Long): Long? {
        val previous = biomeBestMs(biome)
        if (previous == null || durationMs < previous) state.biomeBest[biome.name] = durationMs
        dirty = true
        save()
        return previous
    }

    fun tick() {
        if (dirty) save()
    }

    fun save() {
        dirty = false
        runCatching { file.writeText(gson.toJson(state)) }
            .onFailure { SafariGroupHelper.logger.error("Could not write stats.json", it) }
    }
}
