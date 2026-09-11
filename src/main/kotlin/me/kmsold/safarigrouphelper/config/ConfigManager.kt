package me.kmsold.safarigrouphelper.config

import com.google.gson.GsonBuilder
import me.kmsold.safarigrouphelper.SafariGroupHelper
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Loads and saves [SghConfig]. Saving is debounced through [markDirty] so that recording a catch
 * every few seconds does not hit the disk on every single chat line.
 */
object ConfigManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    val configDir: Path by lazy {
        FabricLoader.getInstance().configDir.resolve("safarigrouphelper").also { it.createDirectories() }
    }

    private val configFile: Path get() = configDir.resolve("config.json")

    var config: SghConfig = SghConfig()
        private set

    private var dirty = false
    private var lastSave = 0L

    fun load() {
        config = if (configFile.exists()) {
            runCatching { gson.fromJson(configFile.readText(), SghConfig::class.java) }
                .onFailure { SafariGroupHelper.logger.error("Could not read config.json, using defaults", it) }
                .getOrNull() ?: SghConfig()
        } else {
            SghConfig()
        }
        // Gson happily leaves non-null fields null when a key is missing from an older file.
        @Suppress("SENSELESS_COMPARISON")
        if (config.positions == null) config.positions = LinkedHashMap()
        save()
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
