package me.kmsold.safarigrouphelper.data

import com.google.gson.GsonBuilder
import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.config.ConfigManager
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** One recorded critter within a run. */
data class CatchEntry(
    var count: Int = 0,
    var firstBy: String? = null,
)

/** Serializable state of the current run, so a relog inside the safari does not lose progress. */
data class SessionState(
    var active: Boolean = false,
    var startedAt: Long = 0L,
    var endedAt: Long = 0L,
    var completedAt: Long = 0L,
    var completionAnnounced: Boolean = false,
    /** Cleared by a mid-run reset: such a run must not set a personal best. */
    var validForPersonalBest: Boolean = true,
    var totalCatches: Int = 0,
    /** Biomes already announced to the party this run, so it is said once and only once. */
    var announcedBiomes: MutableSet<String> = LinkedHashSet(),
    var catches: MutableMap<String, CatchEntry> = LinkedHashMap(),
)

/**
 * Progress of the *current* Critter Safari run. Uniques are what counts; repeats are only
 * tallied as a number (per critter in [CatchEntry.count] and lifetime in [SafariStats]).
 */
object SafariSession {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file by lazy { ConfigManager.configDir.resolve("session.json") }

    var state: SessionState = SessionState()
        private set

    private var dirty = false
    private var lastSave = 0L

    fun load() {
        state = if (file.exists()) {
            runCatching { gson.fromJson(file.readText(), SessionState::class.java) }
                .onFailure { SafariGroupHelper.logger.error("Could not read session.json", it) }
                .getOrNull() ?: SessionState()
        } else {
            SessionState()
        }
        @Suppress("SENSELESS_COMPARISON")
        if (state.catches == null) state.catches = LinkedHashMap()
        @Suppress("SENSELESS_COMPARISON")
        if (state.announcedBiomes == null) state.announcedBiomes = LinkedHashSet()
    }

    /** Throws away whatever was there and starts counting from zero. */
    fun start() {
        reset()
        state.active = true
        state.startedAt = System.currentTimeMillis()
        markDirty()
    }

    fun stop() {
        if (!state.active) return
        state.active = false
        if (state.endedAt == 0L) state.endedAt = System.currentTimeMillis()
        markDirty()
    }

    fun reset() {
        state = SessionState()
        markDirty()
    }

    val isActive: Boolean get() = state.active

    val validForPersonalBest: Boolean get() = state.validForPersonalBest

    /**
     * Marks the current run as not comparable. Used when the run is reset while still inside the
     * safari: the timer restarts, but the capsules already spent make the time meaningless.
     */
    fun invalidateForPersonalBest() {
        state.validForPersonalBest = false
        markDirty()
    }

    /**
     * Whether run progress may be put on screen. Progress is a Critter Safari thing: outside of
     * it the numbers of the last run are stale, so nothing that counts critters should be drawn.
     */
    val progressVisible: Boolean get() = LocationTracker.inSafari

    val elapsedMs: Long
        get() {
            if (state.startedAt == 0L) return 0
            val end = if (state.active) System.currentTimeMillis() else state.endedAt.takeIf { it > 0 } ?: state.startedAt
            return (end - state.startedAt).coerceAtLeast(0)
        }

    /** Time it took to reach 100%, or null if this run never completed. */
    val completionMs: Long?
        get() = if (state.completedAt > 0 && state.startedAt > 0) state.completedAt - state.startedAt else null

    /** @return true when this critter had not been caught yet in this run. */
    fun record(critter: String, player: String?): Boolean {
        val entry = state.catches.getOrPut(critter) { CatchEntry() }
        val isNew = entry.count == 0
        entry.count++
        if (isNew) entry.firstBy = player
        state.totalCatches++
        if (!isNew) SafariStats.addDuplicate(critter)
        markDirty()
        return isNew
    }

    fun countOf(critter: String): Int = state.catches[critter]?.count ?: 0

    fun hasCritter(critter: String): Boolean = countOf(critter) > 0

    fun firstBy(critter: String): String? = state.catches[critter]?.firstBy

    fun uniques(biome: CritterBiome): Int = biome.critters.count { hasCritter(it) }

    val uniqueTotal: Int get() = CritterBiome.entries.sumOf { uniques(it) }

    val duplicates: Int get() = (state.totalCatches - uniqueTotal).coerceAtLeast(0)

    val isComplete: Boolean get() = uniqueTotal >= CritterBiome.totalCritterCount

    fun isCleared(biome: CritterBiome): Boolean = uniques(biome) >= biome.total

    /** @return true the first time this biome is reported as cleared during this run. */
    fun markBiomeAnnounced(biome: CritterBiome): Boolean {
        if (!state.announcedBiomes.add(biome.name)) return false
        markDirty()
        return true
    }

    /** Marks the moment 100% was reached. Returns true the first time it is called for a run. */
    fun markCompleted(): Boolean {
        if (state.completionAnnounced) return false
        state.completionAnnounced = true
        state.completedAt = System.currentTimeMillis()
        markDirty()
        return true
    }

    fun markDirty() {
        dirty = true
    }

    fun tick() {
        if (!dirty) return
        val now = System.currentTimeMillis()
        if (now - lastSave < 1000) return
        save()
    }

    fun save() {
        dirty = false
        lastSave = System.currentTimeMillis()
        runCatching { file.writeText(gson.toJson(state)) }
            .onFailure { SafariGroupHelper.logger.error("Could not write session.json", it) }
    }
}
