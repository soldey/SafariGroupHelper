package me.kmsold.safarigrouphelper.chat

import com.google.gson.Gson
import com.google.gson.JsonObject
import me.kmsold.safarigrouphelper.data.CritterBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Runs the bundled patterns against real chat lines copied out of the game. */
class CritterParsingTest {

    private val bundled: JsonObject = CritterParsingTest::class.java
        .getResourceAsStream("/assets/safarigrouphelper/patterns.json")!!
        .bufferedReader()
        .use { Gson().fromJson(it, JsonObject::class.java) }

    private fun regexList(key: String): List<Regex> =
        bundled.getAsJsonArray(key).map { Regex(it.asString) }

    private val catchPatterns get() = regexList("catch")
    private val enterPatterns get() = regexList("enterSafari")
    private val startPatterns get() = regexList("activityStart")
    private val keywords get() = bundled.getAsJsonArray("catchKeywords").map { it.asString.lowercase() }

    private fun parse(raw: String) =
        CritterParsing.parseCatch(CritterParsing.clean(raw), catchPatterns, keywords)

    @Test
    fun `cleans rank prefixes and head placeholders`() {
        assertEquals(
            "Tom_Fisher entered Critter Safari!",
            CritterParsing.clean("[MVP+] [Tom_Fisher head]Tom_Fisher entered Critter Safari!"),
        )
    }

    @Test
    fun `parses the entering player`() {
        val cleaned = CritterParsing.clean("[MVP+] [Tom_Fisher head]Tom_Fisher entered Critter Safari!")
        assertEquals("Tom_Fisher", CritterParsing.parseEnter(cleaned, enterPatterns))
    }

    @Test
    fun `recognises the safari manager send off`() {
        val cleaned = CritterParsing.clean("[NPC] Safari Manager: I already saw your ticket, so you're free to go.")
        assertTrue(CritterParsing.matchesAny(cleaned, startPatterns))
    }

    @Test
    fun `parses own capture line`() {
        val parsed = parse("CAPTURE! You caught a Shyworm and gained a Shyworm Shard!")
        assertEquals("Shyworm", parsed?.critter)
        assertEquals("You", parsed?.player)
    }

    @Test
    fun `parses capture line with colour codes`() {
        val parsed = parse("§6§lCAPTURE! §r§7You caught a §aFlitter §r§7and gained a §aFlitter Shard§7!")
        assertEquals("Flitter", parsed?.critter)
    }

    @Test
    fun `parses a two word critter`() {
        val parsed = parse("CAPTURE! You caught a Mantis Shrimp and gained a Mantis Shrimp Shard!")
        assertEquals("Mantis Shrimp", parsed?.critter)
    }

    @Test
    fun `parses another players capture`() {
        val parsed = parse("CAPTURE! Tom_Fisher caught a Doomspiral!")
        assertEquals("Doomspiral", parsed?.critter)
        assertEquals("Tom_Fisher", parsed?.player)
    }

    @Test
    fun `parses a loot share catch from another player`() {
        val parsed = parse(
            "LOOT SHARE! You received a Foxtrot Shard from [MrJerson head]MrJerson catching a Foxtrot!",
        )
        assertEquals("Foxtrot", parsed?.critter)
        assertEquals("MrJerson", parsed?.player)
    }

    @Test
    fun `loot share does not credit the local player`() {
        val parsed = parse(
            "LOOT SHARE! You received a Mantis Shrimp Shard from MrJerson catching a Mantis Shrimp!",
        )
        assertEquals("Mantis Shrimp", parsed?.critter)
        assertEquals("MrJerson", parsed?.player)
    }

    @Test
    fun `parses a loot share with a stack and a different verb`() {
        val parsed = parse(
            "LOOT SHARE! You received 3x Hideyho Shard from [MrJerson head]MrJerson finding the Hideyho!",
        )
        assertEquals("Hideyho", parsed?.critter)
        assertEquals("MrJerson", parsed?.player)
    }

    @Test
    fun `sparkling critters count as their base critter`() {
        val parsed = parse("CAPTURE! You caught a Sparkling Gemzie and gained a Gemzie Shard!")
        assertEquals("Gemzie", parsed?.critter)
    }

    @Test
    fun `falls back to the heuristic for unknown wordings`() {
        val parsed = parse("Tom_Fisher somehow captured the elusive Woodchucker today")
        assertEquals("Woodchucker", parsed?.critter)
        assertEquals("Tom_Fisher", parsed?.player)
    }

    @Test
    fun `ignores unrelated chat`() {
        assertNull(parse("Tom_Fisher: anyone selling a Hyperion?"))
        assertNull(parse("You caught a Cod!"))
    }

    @Test
    fun `every biome has its critters`() {
        assertEquals(36, CritterBiome.totalCritterCount, "bonus critters must stay out of the total")
        assertEquals(CritterBiome.CAVERN, CritterBiome.biomeOf("Shyworm"))
        assertEquals(CritterBiome.ICY, CritterBiome.biomeOf("Mantis Shrimp"))
    }

    @Test
    fun `macaw is a forest bonus and does not hold a clear back`() {
        assertEquals(listOf("Macaw"), CritterBiome.FOREST.bonusCritters)
        assertEquals(8, CritterBiome.FOREST.total)
        assertTrue(CritterBiome.isBonus("Macaw"))
        assertTrue(!CritterBiome.isBonus("Parakeet"))
        // Still a Forest critter as far as parsing and lookups go.
        assertEquals(CritterBiome.FOREST, CritterBiome.biomeOf("Macaw"))
        assertEquals("Macaw", parse("CAPTURE! You caught a Macaw and gained a Macaw Shard!")?.critter)
    }
}
