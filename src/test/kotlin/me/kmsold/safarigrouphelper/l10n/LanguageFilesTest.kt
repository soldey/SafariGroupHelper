package me.kmsold.safarigrouphelper.l10n

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Guards the language files: a stray key or a dropped placeholder in a translation would only
 * show up in game, where it either prints the key or swallows a value.
 */
class LanguageFilesTest {

    private val placeholder = Regex("%(?:\\d+\\\$)?[a-zA-Z]")

    private fun load(code: String): Map<String, String> {
        val stream = checkNotNull(javaClass.getResourceAsStream("/assets/safarigrouphelper/lang/$code.json")) {
            "missing language file $code.json"
        }
        val json = stream.bufferedReader().use { Gson().fromJson(it, JsonObject::class.java) }
        return json.entrySet().filterNot { it.key.startsWith("_") }.associate { it.key to it.value.asString }
    }

    private val english = load(Language.FALLBACK_CODE)

    private val translations: List<String>
        get() = Language.entries.map { it.code }.filter { it != Language.FALLBACK_CODE }

    @Test
    fun `english is the complete key set`() {
        assertTrue(english.size > 50, "expected the source language to carry every runtime string")
    }

    @Test
    fun `every language in the enum ships a file`() {
        for (language in Language.entries) {
            assertTrue(load(language.code).isNotEmpty(), "${language.code}.json is missing or empty")
        }
    }

    @TestFactory
    fun `translations only use keys english knows`(): List<DynamicTest> = translations.map { code ->
        DynamicTest.dynamicTest(code) {
            val unknown = load(code).keys.filterNot { it.startsWith("config.") || it in english }
            assertEquals(emptyList<String>(), unknown, "$code.json has keys that do not exist in English")
        }
    }

    @TestFactory
    fun `translations keep the same placeholders`(): List<DynamicTest> = translations.map { code ->
        DynamicTest.dynamicTest(code) {
            for ((key, value) in load(code)) {
                val source = english[key] ?: continue
                assertEquals(
                    placeholder.findAll(source).map { it.value }.toSet(),
                    placeholder.findAll(value).map { it.value }.toSet(),
                    "placeholders differ for '$key' in $code.json",
                )
            }
        }
    }
}
