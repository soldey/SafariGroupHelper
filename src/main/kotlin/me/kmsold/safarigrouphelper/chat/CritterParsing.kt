package me.kmsold.safarigrouphelper.chat

import me.kmsold.safarigrouphelper.data.CritterBiome

/** A catch understood from a chat line. [player] is null when the wording did not name anyone. */
data class ParsedCatch(val critter: String, val player: String?)

/**
 * The pure part of chat parsing: no Minecraft, no config, no state - just strings in, results out.
 * Kept separate from [CritterChatParser] so it can be unit tested against real chat lines.
 */
object CritterParsing {

    private val formattingCodes = Regex("§.")
    private val brackets = Regex("\\[[^\\[\\]]*]")
    private val whitespace = Regex("\\s+")
    private val nameToken = Regex("[A-Za-z0-9_]{2,16}")

    /**
     * Strips colour codes and every `[bracketed]` segment, then collapses whitespace, so that
     * `"[MVP+] [Tom_Fisher head]Tom_Fisher entered Critter Safari!"` becomes
     * `"Tom_Fisher entered Critter Safari!"`.
     */
    fun clean(raw: String): String = whitespace
        .replace(brackets.replace(formattingCodes.replace(raw, ""), " "), " ")
        .trim()

    /** Runs the configured patterns first, then falls back to [heuristicCatch]. */
    fun parseCatch(cleaned: String, patterns: List<Regex>, keywords: List<String>): ParsedCatch? =
        parsePatternCatch(cleaned, patterns) ?: heuristicCatch(cleaned, keywords)

    /**
     * Only the configured patterns. A hit here is solid enough to act on even when the mod is not
     * sure it is inside the safari yet.
     */
    fun parsePatternCatch(cleaned: String, patterns: List<Regex>): ParsedCatch? {
        for (pattern in patterns) {
            val match = pattern.find(cleaned) ?: continue
            val critter = match.group("critter")?.let { CritterBiome.canonical(it) } ?: continue
            return ParsedCatch(critter, match.group("player"))
        }
        return null
    }

    /**
     * Last-resort parse for wordings we have not seen: a catch keyword followed somewhere by a
     * known critter name. The player is the last name-shaped word before the keyword.
     */
    fun heuristicCatch(cleaned: String, keywords: List<String>): ParsedCatch? {
        val lower = cleaned.lowercase()
        val keywordIndex = keywords.mapNotNull { keyword ->
            lower.indexOf(keyword).takeIf { it >= 0 }?.let { it to keyword }
        }.minByOrNull { it.first } ?: return null

        val after = cleaned.substring(keywordIndex.first + keywordIndex.second.length)
        val critter = CritterBiome.allCritters
            .filter { after.contains(it, ignoreCase = true) }
            .minByOrNull { after.indexOf(it, ignoreCase = true) }
            ?: return null

        val before = cleaned.substring(0, keywordIndex.first)
        return ParsedCatch(critter, guessPlayer(before))
    }

    /**
     * In practice the player is the first name-shaped word of the line. Shouty prefixes such as
     * `CAPTURE!` and critter names are skipped.
     */
    private fun guessPlayer(before: String): String? = nameToken.findAll(before)
        .map { it.value }
        .firstOrNull { token ->
            token != token.uppercase() && CritterBiome.canonical(token) == null
        }

    /** @return the player named in an "entered Critter Safari" line, or null if there is none. */
    fun parseEnter(cleaned: String, patterns: List<Regex>): String? {
        for (pattern in patterns) {
            val match = pattern.find(cleaned) ?: continue
            return match.group("player") ?: ""
        }
        return null
    }

    fun matchesAny(cleaned: String, patterns: List<Regex>): Boolean =
        patterns.any { it.containsMatchIn(cleaned) }

    /** Named groups throw when the pattern does not declare them, so every lookup is guarded. */
    private fun MatchResult.group(name: String): String? = runCatching { groups[name]?.value }.getOrNull()
}
