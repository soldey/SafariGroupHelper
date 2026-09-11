package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.util.ChatOut
import me.kmsold.safarigrouphelper.util.TimeFormat

/**
 * Owns the lifecycle of a run: start/stop the timer, record catches, announce uniques and
 * personal bests. Everything it prints is client side only.
 */
object SafariRunController {

    fun onEnterSafari() {
        val fresh = SafariSession.startOrResume()
        if (fresh) {
            SafariStats.runStarted()
            val biome = ConfigManager.config.general.selectedBiome
            ChatOut.send("chat.runStarted", "${biome.colorCode}${biome.translatedName}")
        } else {
            ChatOut.send("chat.runResumed", TimeFormat.clock(SafariSession.elapsedMs))
        }
    }

    fun onLeaveSafari() {
        if (!SafariSession.isActive) return
        val elapsed = SafariSession.elapsedMs
        SafariSession.stop()
        SafariSession.save()
        ChatOut.send(
            "chat.runEnded",
            TimeFormat.clock(elapsed),
            SafariSession.uniqueTotal,
            CritterBiome.totalCritterCount,
            SafariSession.duplicates,
        )
    }

    /** Handles one parsed catch. [player] is null when the parser could not tell who caught it. */
    fun onCatch(critter: String, player: String?) {
        // A catch outside of a tracked run still starts one, e.g. if we missed the enter message.
        if (!SafariSession.isActive) SafariSession.startOrResume()

        val isNew = SafariSession.record(critter, player)
        val biome = CritterBiome.biomeOf(critter)

        if (isNew && ConfigManager.config.general.announceNewUniques) {
            ChatOut.send(
                "chat.newUnique",
                critter,
                biome?.let { "${it.colorCode}${it.translatedName}" } ?: "§7?",
                biome?.let { SafariSession.uniques(it) } ?: 0,
                biome?.total ?: 0,
                SafariSession.uniqueTotal,
                CritterBiome.totalCritterCount,
            )
        }

        if (SafariSession.isComplete) checkCompletion()
    }

    private fun checkCompletion() {
        if (!SafariSession.markCompleted()) return
        val duration = SafariSession.completionMs ?: SafariSession.elapsedMs
        val previousBest = SafariStats.recordCompletion(duration)

        ChatOut.send("chat.complete", CritterBiome.totalCritterCount, TimeFormat.clock(duration))
        when {
            previousBest == null -> ChatOut.send("chat.firstCompletion")

            duration < previousBest -> ChatOut.send(
                "chat.newPersonalBest",
                TimeFormat.clock(previousBest),
                TimeFormat.clock(previousBest - duration),
            )

            else -> ChatOut.send(
                "chat.personalBestKept",
                TimeFormat.clock(previousBest),
                TimeFormat.clock(duration - previousBest),
            )
        }
    }
}
