package me.kmsold.safarigrouphelper

import com.mojang.brigadier.arguments.StringArgumentType
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.OtherBiomesMode
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.data.SafariStats
import me.kmsold.safarigrouphelper.hud.BiomeSelectScreen
import me.kmsold.safarigrouphelper.hud.HudEditorScreen
import me.kmsold.safarigrouphelper.hud.SghConfigScreen
import me.kmsold.safarigrouphelper.util.ChatOut
import me.kmsold.safarigrouphelper.util.TimeFormat
import me.kmsold.safarigrouphelper.util.text
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.minecraft.ChatFormatting

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
        SafariGroupHelper.openScreen(SghConfigScreen(null))
        return 1
    }

    private fun openEditor(): Int {
        SafariGroupHelper.openScreen(HudEditorScreen())
        return 1
    }

    private fun setBiome(raw: String): Int {
        val biome = CritterBiome.byKey(raw)
        if (biome == null) {
            ChatOut.send(
                text("Unknown biome '$raw'. Use: ", ChatFormatting.RED)
                    .append(text(CritterBiome.entries.joinToString(", ") { it.key }, ChatFormatting.GRAY)),
            )
            return 0
        }
        ConfigManager.config.selectedBiome = biome
        ConfigManager.save()
        ChatOut.send(
            text("Biome set to ", ChatFormatting.GRAY)
                .append(text(biome.displayName, biome.formatting)),
        )
        return 1
    }

    private fun setOthersMode(raw: String): Int {
        val mode = OtherBiomesMode.entries.find { it.name.equals(raw, ignoreCase = true) }
        if (mode == null) {
            ChatOut.send(
                text("Unknown mode '$raw'. Use: ", ChatFormatting.RED)
                    .append(
                        text(
                            OtherBiomesMode.entries.joinToString(", ") { it.name.lowercase() },
                            ChatFormatting.GRAY,
                        ),
                    ),
            )
            return 0
        }
        ConfigManager.config.otherBiomesMode = mode
        ConfigManager.save()
        ChatOut.send(
            text("Other biomes display: ", ChatFormatting.GRAY)
                .append(text(mode.label, ChatFormatting.YELLOW)),
        )
        return 1
    }

    private fun resetRun(): Int {
        SafariSession.reset()
        SafariSession.save()
        ChatOut.send("Run progress reset.", ChatFormatting.YELLOW)
        return 1
    }

    private fun toggleDebug(): Int {
        val config = ConfigManager.config
        config.debugChatParsing = !config.debugChatParsing
        ConfigManager.save()
        ChatOut.send(
            text("Chat debug: ", ChatFormatting.GRAY)
                .append(
                    if (config.debugChatParsing) text("ON", ChatFormatting.GREEN)
                    else text("OFF", ChatFormatting.RED),
                )
                .append(text(" (log: chat-debug.log)", ChatFormatting.DARK_GRAY)),
        )
        return 1
    }

    /** Prints everything the location detection is working with - for reporting problems. */
    private fun dumpLocation(): Int {
        ChatOut.send(
            text("mod api: ", ChatFormatting.GRAY)
                .append(
                    if (LocationTracker.apiAvailable) {
                        text("mode=${LocationTracker.apiMode} map=${LocationTracker.apiMap}", ChatFormatting.GREEN)
                    } else {
                        text("no location packet received yet", ChatFormatting.RED)
                    },
                ),
        )
        ChatOut.send(
            text("inSafari=${LocationTracker.inSafari} inCanyon=${LocationTracker.inCanyon}", ChatFormatting.YELLOW),
        )
        ChatOut.send(text("area line: ${LocationTracker.areaLine ?: "none"}", ChatFormatting.GRAY))
        val sidebar = LocationTracker.sidebarLines
        if (sidebar.isEmpty()) {
            ChatOut.send("sidebar: empty", ChatFormatting.RED)
        } else {
            ChatOut.send("sidebar (${sidebar.size} lines):", ChatFormatting.GRAY)
            sidebar.forEach { ChatOut.send(text("  '$it'", ChatFormatting.DARK_GRAY)) }
        }
        return 1
    }

    private fun printStatus(): Int {
        val biome = ConfigManager.config.selectedBiome
        ChatOut.send(
            text("Area: ", ChatFormatting.GRAY)
                .append(
                    text(
                        when {
                            LocationTracker.inSafari -> LocationTracker.CRITTER_SAFARI
                            LocationTracker.inCanyon -> LocationTracker.TORRHUS_CANYON
                            else -> LocationTracker.areaLine ?: LocationTracker.apiMap ?: "unknown"
                        },
                        ChatFormatting.YELLOW,
                    ),
                ),
        )
        ChatOut.send(
            text("Biome ", ChatFormatting.GRAY)
                .append(text(biome.displayName, biome.formatting))
                .append(text(" ${SafariSession.uniques(biome)}/${biome.total}", ChatFormatting.AQUA)),
        )
        for (other in CritterBiome.entries.filter { it != biome }) {
            ChatOut.send(
                text("  ", ChatFormatting.GRAY)
                    .append(text(other.displayName, other.formatting))
                    .append(text(" ${SafariSession.uniques(other)}/${other.total}", ChatFormatting.AQUA)),
            )
        }
        ChatOut.send(
            text("Total ", ChatFormatting.GRAY)
                .append(
                    text(
                        "${SafariSession.uniqueTotal}/${CritterBiome.totalCritterCount}",
                        ChatFormatting.AQUA,
                    ),
                )
                .append(text(" | time ", ChatFormatting.GRAY))
                .append(text(TimeFormat.clock(SafariSession.elapsedMs), ChatFormatting.YELLOW))
                .append(text(" | repeats ", ChatFormatting.GRAY))
                .append(text("${SafariSession.duplicates}", ChatFormatting.WHITE)),
        )
        SafariStats.personalBestMs?.let {
            ChatOut.send(
                text("Personal best: ", ChatFormatting.GRAY)
                    .append(text(TimeFormat.clock(it), ChatFormatting.LIGHT_PURPLE)),
            )
        }
        return 1
    }
}
