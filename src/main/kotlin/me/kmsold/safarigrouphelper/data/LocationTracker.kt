package me.kmsold.safarigrouphelper.data

import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

/**
 * Figures out where the player is, from three sources in order of trust:
 *
 *  1. the Hypixel Mod API location packet (`mode=safari` / `mode=foraging_3`) - exact, but only
 *     available when the `hypixel-mod-api` mod is installed;
 *  2. the SkyBlock sidebar scoreboard (the `⏣ <area>` line);
 *  3. the chat line "<player> entered Critter Safari!", which is the fastest of the three.
 *
 * The chat signal is sticky: it stays set until a more authoritative source says we are
 * somewhere else, so the HUD does not vanish just because the sidebar words things differently
 * than expected.
 */
object LocationTracker {

    const val CRITTER_SAFARI = "Critter Safari"
    const val TORRHUS_CANYON = "Torrhus Canyon"

    /** Hypixel Mod API `mode` values, see SkyBlock island ids. */
    const val SAFARI_MODE = "safari"
    const val CANYON_MODE = "foraging_3"

    /**
     * The chat message beats the sidebar to the punch by a second or two, during which the
     * sidebar still shows the area we just left. It must not cancel the chat signal in that
     * window, or the run would start and immediately stop again.
     */
    private const val CHAT_TRUST_MS = 15_000L

    private val formattingCodes = Regex("§.")
    private const val AREA_MARKER = "⏣"

    /** Only the safari sidebar counts captured mobs, so the line alone gives us away. */
    private val safariSidebarMarker = Regex("Captured Mobs:\\s*\\d+")

    var apiMode: String? = null
        private set
    var apiMap: String? = null
        private set

    /** True once a location packet has been received, i.e. the mod api can be trusted. */
    var apiAvailable: Boolean = false
        private set

    var sidebarLines: List<String> = emptyList()
        private set

    /** The `⏣ <area>` sidebar line, if there is one. */
    val areaLine: String? get() = sidebarLines.firstOrNull { it.contains(AREA_MARKER) }

    var inSafari: Boolean = false
        private set

    var inCanyon: Boolean = false
        private set

    private var chatEnter = false
    private var chatEnterAt = 0L

    /** Which safari instance we are on, so a safari -> safari hop is not mistaken for staying put. */
    private var safariServer: String? = null

    fun onLocationPacket(serverName: String?, mode: String?, map: String?) {
        apiAvailable = true
        apiMode = mode
        apiMap = map
        val safari = mode == SAFARI_MODE
        // Hypixel hands out a different server per safari, so a new name means a new run.
        val newInstance = safari && serverName != null && safariServer != null && serverName != safariServer
        safariServer = if (safari) serverName else null
        update(newInstance)
    }

    /** Called when we saw our own "entered Critter Safari!" message. */
    fun onChatEnteredSafari() {
        chatEnter = true
        chatEnterAt = System.currentTimeMillis()
        // Entering is entering, even when we were already counted as inside: going straight from
        // one safari into the next has to start a new run rather than continue the old one.
        update(newInstance = true)
    }

    fun onDisconnect() {
        apiMode = null
        apiMap = null
        chatEnter = false
        safariServer = null
        sidebarLines = emptyList()
        update()
    }

    fun tick() {
        sidebarLines = readSidebar()
        update()
    }

    private fun update(newInstance: Boolean = false) {
        val area = areaLine
        val sidebarSafari = area?.contains("Safari", ignoreCase = true) == true ||
            sidebarLines.any { safariSidebarMarker.containsMatchIn(it) }
        val sidebarCanyon = area?.contains("Torrhus", ignoreCase = true) == true ||
            area?.contains("Canyon", ignoreCase = true) == true

        val apiSafari = apiAvailable && apiMode == SAFARI_MODE
        val apiElsewhere = apiAvailable && apiMode != null && apiMode != SAFARI_MODE
        val chatIsFresh = System.currentTimeMillis() - chatEnterAt < CHAT_TRUST_MS

        // Drop the chat signal when something authoritative disagrees - but give the sidebar a
        // moment to catch up first, otherwise it cancels the signal with a stale area.
        if (apiElsewhere || (area != null && !sidebarSafari && !chatIsFresh)) chatEnter = false

        inCanyon = (apiAvailable && apiMode == CANYON_MODE) || sidebarCanyon
        val safari = apiSafari || sidebarSafari || chatEnter
        if (safari == inSafari) {
            if (safari && newInstance) SafariRunController.onEnterSafari()
            return
        }
        inSafari = safari
        if (safari) SafariRunController.onEnterSafari() else SafariRunController.onLeaveSafari()
    }

    /** Text of every visible sidebar row, formatting codes stripped. */
    private fun readSidebar(): List<String> {
        val level = Minecraft.getInstance().level ?: return emptyList()
        val scoreboard = level.scoreboard
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        val lines = ArrayList<String>()
        lines += objective.displayName.string.stripFormatting()
        for (entry in scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden) continue
            val team = scoreboard.getPlayerTeam(entry.owner())
            lines += PlayerTeam.formatNameForTeam(team, entry.ownerName()).string.stripFormatting()
        }
        return lines
    }

    private fun String.stripFormatting(): String = formattingCodes.replace(this, "").trim()

    /** Whether the biome selection UI may be shown at the current location. */
    fun biomeSelectAllowed(visibility: me.kmsold.safarigrouphelper.config.BiomeSelectVisibility): Boolean =
        when (visibility) {
            me.kmsold.safarigrouphelper.config.BiomeSelectVisibility.SAFARI_ONLY -> inSafari
            me.kmsold.safarigrouphelper.config.BiomeSelectVisibility.SAFARI_AND_CANYON -> inSafari || inCanyon
            me.kmsold.safarigrouphelper.config.BiomeSelectVisibility.EVERYWHERE -> true
        }
}
