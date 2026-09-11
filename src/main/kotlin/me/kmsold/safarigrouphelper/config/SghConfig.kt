package me.kmsold.safarigrouphelper.config

import me.kmsold.safarigrouphelper.data.CritterBiome

/** How the "other biomes" HUD block is rendered. */
enum class OtherBiomesMode(val label: String) {
    OFF("Off"),
    COMPACT("Progress only"),
    FULL("Full critter list"),
    ;

    fun next(): OtherBiomesMode = entries[(ordinal + 1) % entries.size]
}

/** Where the biome switch button / biome header may appear. */
enum class BiomeSelectVisibility(val label: String) {
    SAFARI_ONLY("Critter Safari only"),
    SAFARI_AND_CANYON("Safari + Torrhus Canyon"),
    EVERYWHERE("Everywhere"),
    ;

    fun next(): BiomeSelectVisibility = entries[(ordinal + 1) % entries.size]
}

data class HudPos(
    var x: Int = 0,
    var y: Int = 0,
    var scale: Float = 1.0f,
    var enabled: Boolean = true,
)

/**
 * Everything that is persisted to `config/safarigrouphelper/config.json`.
 * Plain data class so Gson can (de)serialize it without custom adapters.
 */
data class SghConfig(
    var enabled: Boolean = true,

    /** The biome this player is responsible for in the group. */
    var selectedBiome: CritterBiome = CritterBiome.FOREST,

    var otherBiomesMode: OtherBiomesMode = OtherBiomesMode.COMPACT,

    var biomeSelectVisibility: BiomeSelectVisibility = BiomeSelectVisibility.SAFARI_AND_CANYON,

    /** Hide the "[Switch biome]" button unless an inventory/container screen is open. */
    var switchButtonOnlyInInventory: Boolean = true,

    /** Skip chat parsing entirely while outside the Critter Safari. */
    var parseOnlyInSafari: Boolean = true,

    /** Log every safari-looking chat line that the parser could not understand. */
    var debugChatParsing: Boolean = false,

    /** Announce every newly caught unique critter in your own chat. */
    var announceNewUniques: Boolean = true,

    /** Draw a translucent background behind each HUD block. */
    var hudBackground: Boolean = true,

    var positions: MutableMap<String, HudPos> = LinkedHashMap(),
) {
    fun position(id: String, defaultX: Int, defaultY: Int): HudPos =
        positions.getOrPut(id) { HudPos(defaultX, defaultY) }
}
