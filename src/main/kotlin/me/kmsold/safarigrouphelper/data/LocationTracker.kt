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

    private val formattingCodes = Regex("§.")
    private const val AREA_MARKER = "⏣"

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

    fun onLocationPacket(mode: String?, map: String?) {
        apiAvailable = true
        apiMode = mode
        apiMap = map
        update()
    }

    /** Called when we saw our own "entered Critter Safari!" message. */
    fun onChatEnteredSafari() {
        chatEnter = true
        update()
    }

    fun onDisconnect() {
        apiMode = null
        apiMap = null
        chatEnter = false
        sidebarLines = emptyList()
        update()
    }

    fun tick() {
        sidebarLines = readSidebar()
        update()
    }

    private fun update() {
        val area = areaLine
        val sidebarSafari = area?.contains("Safari", ignoreCase = true) == true
        val sidebarCanyon = area?.contains("Torrhus", ignoreCase = true) == true ||
            area?.contains("Canyon", ignoreCase = true) == true

        val apiSafari = apiAvailable && apiMode == SAFARI_MODE
        val apiElsewhere = apiAvailable && apiMode != null && apiMode != SAFARI_MODE

        // Drop the chat signal only when something authoritative disagrees.
        if (apiElsewhere || (area != null && !sidebarSafari)) chatEnter = false

        inCanyon = (apiAvailable && apiMode == CANYON_MODE) || sidebarCanyon
        val safari = apiSafari || sidebarSafari || chatEnter
        if (safari == inSafari) return
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
