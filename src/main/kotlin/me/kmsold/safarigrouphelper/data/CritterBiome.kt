package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.l10n.Localization
import net.minecraft.ChatFormatting

/**
 * The four biomes of the Critter Safari and the critters that can be hunted in each of them.
 *
 * [critters] is what a biome needs for a clear. [bonusCritters] are the ones that are not in
 * every safari, so they are tracked and shown but never hold a clear back.
 *
 * Sparkling variants are deliberately folded into their base critter (see [normalizeCritterName]);
 * they are not tracked as separate entries for now.
 */
enum class CritterBiome(
    val displayName: String,
    val formatting: ChatFormatting,
    val critters: List<String>,
    val bonusCritters: List<String> = emptyList(),
) {
    CAVERN(
        "Cavern",
        ChatFormatting.GOLD,
        listOf(
            "Cavernfish",
            "Flitter",
            "Shyworm",
            "Driftling",
            "Chuckwalla",
            "Rockmite",
            "Scrappy",
            "Snoozle",
            "Gemzie",
        ),
    ),
    FOREST(
        "Forest",
        ChatFormatting.GREEN,
        listOf(
            "Foxtrot",
            "Bluebird",
            "Honeybug",
            "Treefrog",
            "Woodchucker",
            "Fluffling",
            "Hideonfloor",
            "Parakeet",
        ),
        bonusCritters = listOf("Macaw"),
    ),
    HAUNTED(
        "Haunted",
        ChatFormatting.DARK_PURPLE,
        listOf(
            "Areita",
            "Bloodbat",
            "Duplico",
            "Gazer",
            "Litterbug",
            "Solsnatcher",
            "Gimmiegold",
            "Hideonwall",
            "Hideyho",
            "Doomspiral",
        ),
    ),
    ICY(
        "Icy",
        ChatFormatting.AQUA,
        listOf(
            "Strongarm",
            "Tepid",
            "Polaris",
            "Shuddersquid",
            "Billygoat",
            "Mantis Shrimp",
            "Nozzlenose",
            "Troodon",
            "Wumpa",
        ),
    ),
    ;

    val key: String get() = name.lowercase()

    /** Name shown to the player, from the language files. */
    val translatedName: String get() = Localization.trOr("biome.$key", displayName)

    /** The `§x` colour code of [formatting], for building translated lines. */
    val colorCode: String get() = formatting.toString()

    /** Colour code plus translated name, the usual way a biome is dropped into a message. */
    val coloredName: String get() = "$colorCode$translatedName"

    /**
     * What gets sent to party chat when this biome is done. English on purpose and never
     * localised: the rest of the group has to be able to read it.
     */
    val clearedPartyMessage: String get() = "$displayName is cleared"

    override fun toString(): String = translatedName

    /** How many critters a clear of this biome needs; bonus ones do not count. */
    val total: Int get() = critters.size

    /** Everything that can turn up here, required and bonus alike. */
    val allCritters: List<String> get() = critters + bonusCritters

    companion object {

        /** Every critter of every biome, longest names first so greedy matching picks "Mantis Shrimp" over "Shrimp". */
        val allCritters: List<String> = entries.flatMap { it.allCritters }.sortedByDescending { it.length }

        private val byCritterLowercase: Map<String, CritterBiome> =
            entries.flatMap { biome -> biome.allCritters.map { it.lowercase() to biome } }.toMap()

        private val canonicalNames: Map<String, String> =
            entries.flatMap { it.allCritters }.associateBy { it.lowercase() }

        /** Only the required ones: this is the denominator of the overall progress. */
        val totalCritterCount: Int = entries.sumOf { it.total }

        fun isBonus(critter: String): Boolean =
            entries.any { biome -> biome.bonusCritters.any { it.equals(critter, ignoreCase = true) } }

        fun byKey(key: String): CritterBiome? = entries.find { it.key.equals(key, ignoreCase = true) }

        fun biomeOf(critter: String): CritterBiome? = byCritterLowercase[critter.lowercase()]

        /** Maps any casing (and "Sparkling X") onto the exact critter name, or null if it is not a known critter. */
        fun canonical(critter: String): String? = canonicalNames[normalizeCritterName(critter)]

        fun isKnownCritter(critter: String): Boolean = canonical(critter) != null

        fun normalizeCritterName(raw: String): String = raw.trim()
            .removePrefix("Sparkling ")
            .removePrefix("sparkling ")
            .removePrefix("SPARKLING ")
            .trim()
            .lowercase()
    }
}
