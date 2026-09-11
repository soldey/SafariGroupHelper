package me.kmsold.safarigrouphelper.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.hud.HudEditorScreen
import net.minecraft.client.Minecraft

/** How the "other biomes" HUD block is rendered. */
enum class OtherBiomesMode(val label: String) {
    OFF("Off"),
    COMPACT("Progress only"),
    FULL("Full critter list"),
    ;

    fun next(): OtherBiomesMode = entries[(ordinal + 1) % entries.size]

    override fun toString(): String = label
}

/** Where the biome name and the switch button may appear. */
enum class BiomeSelectVisibility(val label: String) {
    SAFARI_ONLY("Critter Safari only"),
    SAFARI_AND_CANYON("Safari + Torrhus Canyon"),
    EVERYWHERE("Everywhere"),
    ;

    fun next(): BiomeSelectVisibility = entries[(ordinal + 1) % entries.size]

    override fun toString(): String = label
}

class HudPos(
    @Expose var x: Int = 0,
    @Expose var y: Int = 0,
    @Expose var scale: Float = 1.0f,
    @Expose var enabled: Boolean = true,
)

class CritterSafariConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Turns the Critter Safari module on or off. With it off there is no HUD and no " +
            "chat parsing at all.",
    )
    @ConfigEditorBoolean
    var enabled: Boolean = true
}

class GeneralConfig {

    @Expose
    @ConfigOption(
        name = "My biome",
        desc = "The biome you cover for the group. Its critter list gets the detailed HUD block, " +
            "the other three are summarised separately.",
    )
    @ConfigEditorDropdown
    var selectedBiome: CritterBiome = CritterBiome.FOREST

    @Expose
    @ConfigOption(
        name = "Announce new uniques",
        desc = "Prints one line in your own chat whenever a critter is caught for the first time " +
            "this run, with the biome and the running total.",
    )
    @ConfigEditorBoolean
    var announceNewUniques: Boolean = true
}

class HudConfig {

    @ConfigOption(
        name = "HUD positions",
        desc = "Drag blocks with the mouse, scroll over one to resize it, right click to hide it, " +
            "press §eR§7 to reset every position.",
    )
    @ConfigEditorButton(buttonText = "Edit")
    val editPositions: Runnable = Runnable {
        SafariGroupHelper.openScreen(HudEditorScreen(Minecraft.getInstance().screen))
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
    @Category(name = "Critter Safari", desc = "Tracking unique critters caught by your group")
    var critterSafari: CritterSafariConfig = CritterSafariConfig()

    @Expose
    @Category(name = "General", desc = "Your biome and how the mod talks to you")
    var general: GeneralConfig = GeneralConfig()

    @Expose
    @Category(name = "HUD", desc = "What is drawn on screen and where")
    var hud: HudConfig = HudConfig()

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
    }
}
