package me.kmsold.safarigrouphelper.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import me.kmsold.safarigrouphelper.SafariGroupHelper
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Loads and saves [SghConfig]. Saving is debounced through [markDirty] so that dragging a HUD
 * block does not hit the disk on every frame.
 */
object ConfigManager {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    val configDir: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("safarigrouphelper").also { it.createDirectories() }
    }

    private val configFile: Path get() = configDir.resolve("config.json")

    var config: SghConfig = SghConfig()
        private set

    private var dirty = false
    private var lastSave = 0L

    fun load() {
        config = readConfig() ?: SghConfig()
        save()
    }

    private fun readConfig(): SghConfig? {
        if (!configFile.exists()) return null
        val raw = runCatching { gson.fromJson(configFile.readText(), JsonObject::class.java) }
            .onFailure { SafariGroupHelper.logger.error("Could not read config.json, using defaults", it) }
            .getOrNull() ?: return null

        // 1.0.0 kept every option at the top level; move those onto the categorised layout.
        val migrated = if (raw.has("selectedBiome") && !raw.has("general")) migrateFlatConfig(raw) else raw
        return runCatching { gson.fromJson(migrated, SghConfig::class.java) }
            .onFailure { SafariGroupHelper.logger.error("Could not parse config.json, using defaults", it) }
            .getOrNull()
    }

    private fun migrateFlatConfig(old: JsonObject): JsonObject {
        SafariGroupHelper.logger.info("Migrating the 1.0.0 config layout")
        val new = JsonObject()
        val general = JsonObject()
        val hud = JsonObject()
        val chat = JsonObject()

        fun move(key: String, target: JsonObject, newKey: String = key) {
            old.get(key)?.let { target.add(newKey, it) }
        }

        move("enabled", general)
        move("selectedBiome", general)
        move("announceNewUniques", general)
        move("otherBiomesMode", hud)
        move("biomeSelectVisibility", hud)
        move("switchButtonOnlyInInventory", hud)
        move("hudBackground", hud)
        move("parseOnlyInSafari", chat)
        move("debugChatParsing", chat)

        new.add("general", general)
        new.add("hud", hud)
        new.add("chat", chat)
        old.get("positions")?.let { new.add("positions", it) }
        return new
    }

    fun markDirty() {
        dirty = true
    }

    /** Called every client tick; writes at most once per second and only when something changed. */
    fun tick() {
        if (!dirty) return
        val now = System.currentTimeMillis()
        if (now - lastSave < 1000) return
        save()
    }

    fun save() {
        dirty = false
        lastSave = System.currentTimeMillis()
        runCatching {
            Files.createDirectories(configDir)
            configFile.writeText(gson.toJson(config))
        }.onFailure { SafariGroupHelper.logger.error("Could not write config.json", it) }
    }
}
