package me.kmsold.safarigrouphelper.chat

import com.google.gson.Gson
import com.google.gson.JsonObject
import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.config.ConfigManager
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * The regexes the chat parser uses. Loaded from the bundled `patterns.json` and copied into the
 * config folder so they can be tweaked without rebuilding the mod. When the bundled file gets a
 * newer `version`, the on-disk copy is refreshed (the old one is kept as `patterns.json.bak`).
 */
object ChatPatterns {

    private val gson = Gson()

    var catchPatterns: List<Regex> = emptyList()
        private set
    var enterSafari: List<Regex> = emptyList()
        private set
    var activityStart: List<Regex> = emptyList()
        private set
    var catchKeywords: List<String> = listOf("caught", "captured")
        private set

    fun load() {
        val bundled = readBundled()
        val file = ConfigManager.configDir.resolve("patterns.json")

        val json = if (file.exists()) {
            val onDisk = runCatching { gson.fromJson(file.readText(), JsonObject::class.java) }
                .onFailure { SafariGroupHelper.logger.error("Could not read patterns.json", it) }
                .getOrNull()
            val onDiskVersion = onDisk?.get("version")?.asInt ?: -1
            val bundledVersion = bundled?.get("version")?.asInt ?: 0
            if (onDisk == null || onDiskVersion < bundledVersion) {
                runCatching {
                    if (onDisk != null) {
                        ConfigManager.configDir.resolve("patterns.json.bak").writeText(file.readText())
                    }
                    bundled?.let { file.writeText(gson.toJson(it)) }
                }
                bundled ?: onDisk
            } else {
                onDisk
            }
        } else {
            runCatching { bundled?.let { file.writeText(gson.toJson(it)) } }
            bundled
        } ?: return

        catchPatterns = json.regexList("catch")
        enterSafari = json.regexList("enterSafari")
        activityStart = json.regexList("activityStart")
        json.get("catchKeywords")?.asJsonArray?.let { array ->
            catchKeywords = array.map { it.asString.lowercase() }
        }
        SafariGroupHelper.logger.info(
            "Loaded {} catch / {} enter / {} start patterns",
            catchPatterns.size,
            enterSafari.size,
            activityStart.size,
        )
    }

    private fun readBundled(): JsonObject? = runCatching {
        val stream = ChatPatterns::class.java.getResourceAsStream("/assets/safarigrouphelper/patterns.json")
            ?: error("bundled patterns.json is missing from the jar")
        stream.bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
    }.onFailure { SafariGroupHelper.logger.error("Could not read bundled patterns.json", it) }.getOrNull()

    private fun JsonObject.regexList(key: String): List<Regex> {
        val array = get(key)?.asJsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { Regex(element.asString) }
                .onFailure { SafariGroupHelper.logger.error("Invalid pattern in '$key': ${element.asString}", it) }
                .getOrNull()
        }
    }
}
