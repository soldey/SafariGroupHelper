package me.kmsold.safarigrouphelper.l10n

import com.google.gson.Gson
import com.google.gson.JsonObject
import me.kmsold.safarigrouphelper.SafariGroupHelper
import net.minecraft.client.Minecraft

/**
 * Translated strings, loaded from `assets/safarigrouphelper/lang/<code>.json`.
 *
 * Every lookup falls back to English and then to the key itself, so a half translated language
 * file is perfectly usable and a missing key is obvious instead of crashing.
 *
 * Values may contain `§` colour codes and `%s` / `%1$s` style placeholders.
 */
object Localization {

    private val gson = Gson()

    private var strings: Map<String, String> = emptyMap()
    private var fallback: Map<String, String> = emptyMap()

    /** The language file currently in use, for `/sgh dump`. */
    var loadedCode: String = Language.FALLBACK_CODE
        private set

    fun reload(language: Language) {
        if (fallback.isEmpty()) fallback = read(Language.FALLBACK_CODE)
        val code = resolve(language)
        strings = if (code == Language.FALLBACK_CODE) fallback else read(code)
        loadedCode = if (strings.isEmpty()) Language.FALLBACK_CODE else code
        SafariGroupHelper.logger.info("Using language {} ({} strings)", loadedCode, strings.size)
    }

    /** [Language.AUTO] follows Minecraft, but only if we actually ship that language. */
    private fun resolve(language: Language): String {
        if (language != Language.AUTO) return language.code
        val gameCode = runCatching {
            Minecraft.getInstance().languageManager.selected.lowercase()
        }.getOrNull() ?: return Language.FALLBACK_CODE
        return Language.byCode(gameCode)?.code ?: Language.FALLBACK_CODE
    }

    private fun read(code: String): Map<String, String> = runCatching {
        val stream = Localization::class.java.getResourceAsStream("/assets/safarigrouphelper/lang/$code.json")
            ?: return emptyMap()
        val json = stream.bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
        json.entrySet()
            .filterNot { it.key.startsWith("_") }
            .associate { it.key to it.value.asString }
    }.onFailure { SafariGroupHelper.logger.error("Could not read the $code language file", it) }
        .getOrDefault(emptyMap())

    /** @return the translation for [key], or the English one, or [key] itself. */
    fun tr(key: String, vararg args: Any?): String {
        val pattern = strings[key] ?: fallback[key] ?: return key
        if (args.isEmpty()) return pattern
        return runCatching { pattern.format(*args) }
            .onFailure { SafariGroupHelper.logger.error("Bad placeholders in '$key'", it) }
            .getOrDefault(pattern)
    }

    /** Same as [tr], but falls back to [default] instead of the key - used for config annotations. */
    fun trOr(key: String, default: String): String = strings[key] ?: fallback[key] ?: default
}
