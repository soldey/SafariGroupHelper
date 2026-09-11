package me.kmsold.safarigrouphelper.data

import net.minecraft.ChatFormatting

/**
 * The four biomes of the Critter Safari and the critters that can be hunted in each of them.
 *
 * Sparkling variants are deliberately folded into their base critter (see [normalizeCritterName]);
 * they are not tracked as separate entries for now.
 */
enum class CritterBiome(
    val displayName: String,
    val formatting: ChatFormatting,
    val critters: List<String>,
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
            "Macaw",
        ),
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

    val total: Int get() = critters.size

    companion object {

        /** Every critter of every biome, longest names first so greedy matching picks "Mantis Shrimp" over "Shrimp". */
        val allCritters: List<String> = entries.flatMap { it.critters }.sortedByDescending { it.length }

        private val byCritterLowercase: Map<String, CritterBiome> =
            entries.flatMap { biome -> biome.critters.map { it.lowercase() to biome } }.toMap()

        private val canonicalNames: Map<String, String> =
            entries.flatMap { it.critters }.associateBy { it.lowercase() }

        val totalCritterCount: Int = entries.sumOf { it.total }

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
