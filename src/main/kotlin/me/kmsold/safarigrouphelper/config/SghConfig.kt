package me.kmsold.safarigrouphelper.config

import me.kmsold.safarigrouphelper.compat.McCompat
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.hud.HudEditorScreen
import me.kmsold.safarigrouphelper.l10n.Language
import me.kmsold.safarigrouphelper.l10n.Localization
import net.minecraft.client.Minecraft

/** How the "other biomes" HUD block is rendered. */
enum class OtherBiomesMode(val label: String) {
    OFF("Off"),
    COMPACT("Progress only"),
    FULL("Full critter list"),
    ;

    fun next(): OtherBiomesMode = entries[(ordinal + 1) % entries.size]

    /** Shown in the dropdown, so it goes through the language files. */
    override fun toString(): String = Localization.trOr("enum.otherBiomes.$name", label)
}

/** Where the biome name and the switch button may appear. */
enum class BiomeSelectVisibility(val label: String) {
    SAFARI_ONLY("Critter Safari only"),
    SAFARI_AND_CANYON("Safari + Torrhus Canyon"),
    EVERYWHERE("Everywhere"),
    ;

    fun next(): BiomeSelectVisibility = entries[(ordinal + 1) % entries.size]

    override fun toString(): String = Localization.trOr("enum.biomeSelect.$name", label)
}

class HudPos(
    @Expose var x: Int = 0,
    @Expose var y: Int = 0,
    @Expose var scale: Float = 1.0f,
)

class GeneralConfig {

    @ConfigOption(
        name = "HUD positions",
        desc = "Drag blocks with the mouse, scroll over one to resize it, right click to hide it, " +
            "press §eR§7 to reset every position.",
    )
    @ConfigEditorButton(buttonText = "Edit")
    val editPositions: Runnable = Runnable {
        SafariGroupHelper.openScreen(HudEditorScreen(McCompat.currentScreen))
    }

    @Expose
    @ConfigOption(
        name = "Language",
        desc = "Language of the mod's own text. Critter and location names are never translated, " +
            "they appear in Hypixel chat as they are.",
    )
    @ConfigEditorDropdown
    var language: Language = Language.ENGLISH
}

/** Subcategory of Critter Safari: what the tracker draws on screen. */
class CritterSafariHudConfig {

    @ConfigOption(
        name = "HUD positions",
        desc = "The same editor as in General: drag blocks, scroll to resize, §eR§7 resets them.",
    )
    @ConfigEditorButton(buttonText = "Edit")
    val editPositions: Runnable = Runnable {
        SafariGroupHelper.openScreen(HudEditorScreen(McCompat.currentScreen))
    }

    @Expose
    @ConfigOption(
        name = "Other biomes",
        desc = "How much of the other three biomes to show: nothing at all, just the counters " +
            "like §b3/9§7, or the full critter list.",
    )
    @ConfigEditorDropdown
    var otherBiomesMode: OtherBiomesMode = OtherBiomesMode.COMPACT

    @Expose
    @ConfigOption(
        name = "Biome picker shown",
        desc = "Where the biome name and the §e[Switch biome]§7 button are allowed to appear. " +
            "Critter progress itself is always Critter Safari only.",
    )
    @ConfigEditorDropdown
    var biomeSelectVisibility: BiomeSelectVisibility = BiomeSelectVisibility.SAFARI_AND_CANYON

    @Expose
    @ConfigOption(name = "HUD background", desc = "Draws a translucent black box behind every HUD block.")
    @ConfigEditorBoolean
    var hudBackground: Boolean = true

    @Expose
    @ConfigOption(name = "Show my biome", desc = "The block with your own biome and its critter list.")
    @ConfigEditorBoolean
    var showMyBiome: Boolean = true

    @Expose
    @ConfigOption(name = "Show other biomes", desc = "The block summarising the other three biomes.")
    @ConfigEditorBoolean
    var showOtherBiomes: Boolean = true

    @Expose
    @ConfigOption(name = "Show total progress", desc = "The block with the overall bar from 0 to 100%.")
    @ConfigEditorBoolean
    var showTotalProgress: Boolean = true

    @Expose
    @ConfigOption(name = "Show run info", desc = "The block with the timer, catch counts and personal best.")
    @ConfigEditorBoolean
    var showRunInfo: Boolean = true

    @Expose
    @ConfigOption(
        name = "Progress bar",
        desc = "The §a███§7 bar under the total counter. Turn it off to leave just the numbers.",
    )
    @ConfigEditorBoolean
    var showProgressBar: Boolean = true
}

/** Subcategory of Critter Safari: what the tracker says in chat. */
class CritterSafariChatConfig {

    @Expose
    @ConfigOption(
        name = "Announce new uniques",
        desc = "Prints one line in your own chat whenever a critter is caught for the first time " +
            "this run, with the biome and the running total.",
    )
    @ConfigEditorBoolean
    var announceNewUniques: Boolean = true

    @Expose
    @ConfigOption(
        name = "Party announcements",
        desc = "Sends §e/pc <biome> is cleared§7 once every critter of your own biome has been " +
            "caught. This one goes to your party, not just to you, and is always in English.",
    )
    @ConfigEditorBoolean
    var announceBiomeClearedToParty: Boolean = true

    @Expose
    @ConfigOption(
        name = "Auto-accept prompts",
        desc = "Clicks §e[Sure]§7 for you when a critter asks something in chat. This answers on " +
            "your behalf, so leave it off if you would rather click yourself.",
    )
    @ConfigEditorBoolean
    var autoAcceptPrompts: Boolean = false
}

/**
 * Subcategory of Critter Safari: a read-only look at the last runs. The text itself is generated
 * by [LocalizedConfigProcessor] when the settings screen is built.
 */
class RunHistoryConfig {

    @ConfigOption(name = "Last runs", desc = "The 10 most recent runs, newest first.")
    @ConfigEditorInfoText(infoTitle = "")
    val history: String = ""
}

class CritterSafariConfig {

    /** Read-only stats block; its text is generated by [LocalizedConfigProcessor]. */
    @ConfigOption(name = "Your records", desc = "Best times and run counts, overall and per biome.")
    @ConfigEditorInfoText(infoTitle = "")
    val records: String = ""

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Turns the Critter Safari module on or off. With it off there is no HUD and no " +
            "chat parsing at all.",
    )
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "My biome",
        desc = "The biome you cover for the group. Its critter list gets the detailed HUD block, " +
            "the other three are summarised separately.",
    )
    @ConfigEditorDropdown
    var selectedBiome: CritterBiome = CritterBiome.FOREST

    @Expose
    @Category(name = "Hud", desc = "What the tracker draws on screen")
    var hud: CritterSafariHudConfig = CritterSafariHudConfig()

    @Expose
    @Category(name = "Chat", desc = "What the tracker writes in chat")
    var chat: CritterSafariChatConfig = CritterSafariChatConfig()

    @Expose
    @Category(name = "Run history", desc = "Your last 10 runs")
    var runHistory: RunHistoryConfig = RunHistoryConfig()
}

class DevConfig {

    @Expose
    @ConfigOption(
        name = "Parse chat only in Safari",
        desc = "Skips all chat parsing outside the Critter Safari. Keep this on unless you are " +
            "hunting down a parsing problem.",
    )
    @ConfigEditorBoolean
    var parseOnlyInSafari: Boolean = true

    @Expose
    @ConfigOption(
        name = "Debug chat parsing",
        desc = "Writes every chat line seen inside the safari to §echat-debug.log§7 and highlights " +
            "lines that look like a catch but were not understood.",
    )
    @ConfigEditorBoolean
    var debugChatParsing: Boolean = false
}

/** Everything that is persisted to `config/safarigrouphelper/config.json`. */
class SghConfig : Config() {

    @Expose
    @Category(name = "General", desc = "Settings that are not tied to one module")
    var general: GeneralConfig = GeneralConfig()

    @Expose
    @Category(name = "Critter Safari", desc = "Tracking unique critters caught by your group")
    var critterSafari: CritterSafariConfig = CritterSafariConfig()

    @Expose
    @Category(name = "Dev", desc = "Chat parsing internals and debugging")
    var dev: DevConfig = DevConfig()

    /** Edited by dragging in game, not through the settings screen. */
    @Expose
    var positions: MutableMap<String, HudPos> = LinkedHashMap()

    fun position(id: String, defaultX: Int, defaultY: Int): HudPos =
        positions.getOrPut(id) { HudPos(defaultX, defaultY) }

    override fun getTitle(): StructuredText = StructuredText.of("§bSafari Group Helper")

    override fun saveNow() {
        ConfigManager.save()
        // Picking another language only needs the strings swapped; the settings screen itself is
        // rebuilt the next time it is opened.
        Localization.reload(general.language)
        SghConfigGui.invalidate()
    }
}
