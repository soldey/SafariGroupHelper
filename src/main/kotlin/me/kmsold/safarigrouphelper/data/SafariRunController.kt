package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.util.ChatOut
import me.kmsold.safarigrouphelper.util.TimeFormat
import me.kmsold.safarigrouphelper.util.text
import net.minecraft.ChatFormatting

/**
 * Owns the lifecycle of a run: start/stop the timer, record catches, announce uniques and
 * personal bests. Everything it prints is client side only.
 */
object SafariRunController {

    fun onEnterSafari() {
        val fresh = SafariSession.startOrResume()
        if (fresh) {
            SafariStats.runStarted()
            ChatOut.send(
                text("Run started. ", ChatFormatting.GREEN)
                    .append(text("Biome: ", ChatFormatting.GRAY))
                    .append(
                        text(
                            ConfigManager.config.general.selectedBiome.displayName,
                            ConfigManager.config.general.selectedBiome.formatting,
                        ),
                    ),
            )
        } else {
            ChatOut.send(
                text("Resumed run at ", ChatFormatting.GRAY)
                    .append(text(TimeFormat.clock(SafariSession.elapsedMs), ChatFormatting.YELLOW)),
            )
        }
    }

    fun onLeaveSafari() {
        if (!SafariSession.isActive) return
        val elapsed = SafariSession.elapsedMs
        SafariSession.stop()
        SafariSession.save()
        ChatOut.send(
            text("Run ended after ", ChatFormatting.GRAY)
                .append(text(TimeFormat.clock(elapsed), ChatFormatting.YELLOW))
                .append(text(" - ", ChatFormatting.DARK_GRAY))
                .append(
                    text(
                        "${SafariSession.uniqueTotal}/${CritterBiome.totalCritterCount} unique",
                        ChatFormatting.AQUA,
                    ),
                )
                .append(text(", ${SafariSession.duplicates} repeats", ChatFormatting.GRAY)),
        )
    }

    /** Handles one parsed catch. [player] is null when the parser could not tell who caught it. */
    fun onCatch(critter: String, player: String?) {
        // A catch outside of a tracked run still starts one, e.g. if we missed the enter message.
        if (!SafariSession.isActive) SafariSession.startOrResume()

        val isNew = SafariSession.record(critter, player)
        val config = ConfigManager.config
        val biome = CritterBiome.biomeOf(critter)

        if (isNew && config.general.announceNewUniques) {
            val biomeName = biome?.displayName ?: "?"
            val biomeColor = biome?.formatting ?: ChatFormatting.GRAY
            val progress = biome?.let { "${SafariSession.uniques(it)}/${it.total}" } ?: ""
            ChatOut.send(
                text("New: ", ChatFormatting.GREEN)
                    .append(text(critter, ChatFormatting.WHITE))
                    .append(text(" [", ChatFormatting.DARK_GRAY))
                    .append(text(biomeName, biomeColor))
                    .append(text(" $progress", ChatFormatting.GRAY))
                    .append(text("] ", ChatFormatting.DARK_GRAY))
                    .append(
                        text(
                            "${SafariSession.uniqueTotal}/${CritterBiome.totalCritterCount} total",
                            ChatFormatting.AQUA,
                        ),
                    ),
            )
        }

        if (SafariSession.isComplete) checkCompletion()
    }

    private fun checkCompletion() {
        if (!SafariSession.markCompleted()) return
        val duration = SafariSession.completionMs ?: SafariSession.elapsedMs
        val previousBest = SafariStats.recordCompletion(duration)

        ChatOut.send(
            text("100% - all ", ChatFormatting.GOLD)
                .append(text("${CritterBiome.totalCritterCount}", ChatFormatting.YELLOW))
                .append(text(" critters in ", ChatFormatting.GOLD))
                .append(text(TimeFormat.clock(duration), ChatFormatting.YELLOW)),
        )
        when {
            previousBest == null -> ChatOut.send(
                text("First completion - new personal best!", ChatFormatting.LIGHT_PURPLE),
            )

            duration < previousBest -> ChatOut.send(
                text("NEW PERSONAL BEST! ", ChatFormatting.LIGHT_PURPLE)
                    .append(text("(old: ${TimeFormat.clock(previousBest)}, ", ChatFormatting.GRAY))
                    .append(text("-${TimeFormat.clock(previousBest - duration)})", ChatFormatting.GREEN)),
            )

            else -> ChatOut.send(
                text("Personal best stays at ", ChatFormatting.GRAY)
                    .append(text(TimeFormat.clock(previousBest), ChatFormatting.YELLOW))
                    .append(text(" (+${TimeFormat.clock(duration - previousBest)})", ChatFormatting.RED)),
            )
        }
    }
}
