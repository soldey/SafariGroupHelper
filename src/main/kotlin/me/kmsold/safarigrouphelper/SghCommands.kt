package me.kmsold.safarigrouphelper

import com.mojang.brigadier.arguments.StringArgumentType
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.OtherBiomesMode
import me.kmsold.safarigrouphelper.config.SghConfigGui
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.data.SafariStats
import me.kmsold.safarigrouphelper.hud.BiomeSelectScreen
import me.kmsold.safarigrouphelper.hud.HudEditorScreen
import me.kmsold.safarigrouphelper.hud.HudInteractions
import me.kmsold.safarigrouphelper.l10n.Localization
import me.kmsold.safarigrouphelper.util.ChatOut
import me.kmsold.safarigrouphelper.util.TimeFormat
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

/** `/sgh` and its subcommands. Everything is client side. */
object SghCommands {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("sgh")
                    .executes { openSettings() }
                    .then(ClientCommands.literal("settings").executes { openSettings() })
                    .then(ClientCommands.literal("gui").executes { openEditor() })
                    .then(
                        ClientCommands.literal("biome")
                            .executes {
                                SafariGroupHelper.openScreen(BiomeSelectScreen(null))
                                1
                            }
                            .then(
                                ClientCommands.argument("biome", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        CritterBiome.entries.forEach { builder.suggest(it.key) }
                                        builder.buildFuture()
                                    }
                                    .executes { context ->
                                        setBiome(StringArgumentType.getString(context, "biome"))
                                    },
                            ),
                    )
                    .then(
                        ClientCommands.literal("others")
                            .then(
                                ClientCommands.argument("mode", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        OtherBiomesMode.entries.forEach { builder.suggest(it.name.lowercase()) }
                                        builder.buildFuture()
                                    }
                                    .executes { context ->
                                        setOthersMode(StringArgumentType.getString(context, "mode"))
                                    },
                            ),
                    )
                    .then(ClientCommands.literal("reset").executes { resetRun() })
                    .then(ClientCommands.literal("status").executes { printStatus() })
                    .then(ClientCommands.literal("dump").executes { dumpLocation() })
                    .then(ClientCommands.literal("debug").executes { toggleDebug() }),
            )
        }
    }

    private fun openSettings(): Int {
        SafariGroupHelper.openScreen(SghConfigGui.createScreen())
        return 1
    }

    private fun openEditor(): Int {
        SafariGroupHelper.openScreen(HudEditorScreen())
        return 1
    }

    private fun setBiome(raw: String): Int {
        val biome = CritterBiome.byKey(raw)
        if (biome == null) {
            ChatOut.send("command.unknownBiome", raw, CritterBiome.entries.joinToString(", ") { it.key })
            return 0
        }
        ConfigManager.config.critterSafari.selectedBiome = biome
        ConfigManager.save()
        ChatOut.send("command.biomeSet", biome.coloredName)
        return 1
    }

    private fun setOthersMode(raw: String): Int {
        val mode = OtherBiomesMode.entries.find { it.name.equals(raw, ignoreCase = true) }
        if (mode == null) {
            ChatOut.send("command.unknownMode", raw, OtherBiomesMode.entries.joinToString(", ") { it.name.lowercase() })
            return 0
        }
        ConfigManager.config.critterSafari.hud.otherBiomesMode = mode
        ConfigManager.save()
        ChatOut.send("command.othersMode", mode.toString())
        return 1
    }

    private fun resetRun(): Int {
        HudInteractions.resetRunNow()
        return 1
    }

    private fun toggleDebug(): Int {
        val dev = ConfigManager.config.dev
        dev.debugChatParsing = !dev.debugChatParsing
        ConfigManager.save()
        ChatOut.send("command.debug", Localization.tr(if (dev.debugChatParsing) "command.on" else "command.off"))
        return 1
    }

    /** Prints everything the location detection is working with - for reporting problems. */
    private fun dumpLocation(): Int {
        val api = if (LocationTracker.apiAvailable) {
            Localization.tr("command.dump.apiValue", LocationTracker.apiMode, LocationTracker.apiMap)
        } else {
            Localization.tr("command.dump.apiMissing")
        }
        ChatOut.send("command.dump.api", api)
        ChatOut.send("command.dump.flags", LocationTracker.inSafari, LocationTracker.inCanyon)
        ChatOut.send("command.dump.language", Localization.loadedCode)
        ChatOut.send("command.dump.areaLine", LocationTracker.areaLine ?: Localization.tr("command.unknown"))
        val sidebar = LocationTracker.sidebarLines
        if (sidebar.isEmpty()) {
            ChatOut.send("command.dump.sidebarEmpty")
        } else {
            ChatOut.send("command.dump.sidebar", sidebar.size)
            sidebar.forEach { ChatOut.send("command.dump.sidebarLine", it) }
        }
        return 1
    }

    private fun printStatus(): Int {
        val biome = ConfigManager.config.critterSafari.selectedBiome
        val area = when {
            LocationTracker.inSafari -> LocationTracker.CRITTER_SAFARI
            LocationTracker.inCanyon -> LocationTracker.TORRHUS_CANYON
            else -> LocationTracker.areaLine ?: LocationTracker.apiMap ?: Localization.tr("command.unknown")
        }
        ChatOut.send("command.status.area", area)
        ChatOut.send("command.status.biome", biome.coloredName, SafariSession.uniques(biome), biome.total)
        for (other in CritterBiome.entries.filter { it != biome }) {
            ChatOut.send("command.status.otherBiome", other.coloredName, SafariSession.uniques(other), other.total)
        }
        ChatOut.send(
            "command.status.total",
            SafariSession.uniqueTotal,
            CritterBiome.totalCritterCount,
            TimeFormat.clock(SafariSession.elapsedMs),
            SafariSession.duplicates,
        )
        SafariStats.personalBestMs?.let { ChatOut.send("command.status.pb", TimeFormat.clock(it)) }
        return 1
    }
}
