package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.util.ChatOut
import me.kmsold.safarigrouphelper.util.TimeFormat

/**
 * Owns the lifecycle of a run: start/stop the timer, record catches, announce uniques and
 * personal bests. Everything it prints is client side only.
 */
object SafariRunController {

    /**
     * The chat message and the location packet both announce the same entry, seconds apart. A run
     * that young is the one they are both talking about, so the second trigger is ignored.
     */
    private const val DOUBLE_TRIGGER_MS = 15_000L

    /**
     * Called on entering the safari, including when hopping straight from one safari into the
     * next - that is a new run, not a continuation of the old one.
     */
    fun onEnterSafari() {
        if (SafariSession.isActive && SafariSession.elapsedMs < DOUBLE_TRIGGER_MS) return
        // Going safari -> safari, so the run that was going on still belongs in the history.
        if (SafariSession.isActive) fileRun()

        SafariSession.start()
        SafariStats.runStarted()
        // You are in the run even before catching anything, and you never show up in chat.
        localPlayerName()?.let { SafariSession.addPlayer(it) }
        val biome = ConfigManager.config.critterSafari.selectedBiome
        ChatOut.send("chat.runStarted", biome.coloredName)
    }

    fun onLeaveSafari() {
        if (!SafariSession.isActive) return
        val elapsed = SafariSession.elapsedMs
        fileRun()
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

    /** Puts the run that is going on right now into the history. */
    private fun fileRun() {
        val clears = CritterBiome.entries.mapNotNull { biome ->
            val at = SafariSession.clearedAt(biome) ?: return@mapNotNull null
            BiomeClear(biome.name, at, SafariSession.contributors(biome).toMutableList())
        }
        SafariStats.recordRun(
            RunRecord(
                startedAt = SafariSession.state.startedAt,
                durationMs = SafariSession.elapsedMs,
                uniques = SafariSession.uniqueTotal,
                total = CritterBiome.totalCritterCount,
                completed = SafariSession.isComplete,
                valid = SafariSession.validForPersonalBest,
                players = SafariSession.players.toMutableList(),
                biomeClears = clears.toMutableList(),
            ),
        )
    }

    private fun localPlayerName(): String? =
        net.minecraft.client.Minecraft.getInstance().player?.gameProfile?.name

    /** Handles one parsed catch. [player] is null when the parser could not tell who caught it. */
    fun onCatch(critter: String, player: String?) {
        // A catch outside of a tracked run still starts one, e.g. if we missed the enter message.
        if (!SafariSession.isActive) SafariSession.start()

        val isNew = SafariSession.record(critter, player)
        val biome = CritterBiome.biomeOf(critter)

        if (isNew && ConfigManager.config.critterSafari.chat.announceNewUniques) {
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

        checkBiomeClears()
        if (SafariSession.isComplete) checkCompletion()
    }

    /**
     * Fires once per run when the biome this player covers is done: the time always lands in your
     * own chat, the party line is optional, and the best time per biome is kept like the overall
     * personal best.
     */
    private fun checkBiomeClears() {
        for (biome in CritterBiome.entries) {
            if (!SafariSession.isCleared(biome)) continue
            if (!SafariSession.markBiomeCleared(biome)) continue
            val at = SafariSession.clearedAt(biome) ?: SafariSession.elapsedMs
            val who = SafariSession.contributors(biome).joinToString(", ").ifEmpty { "-" }
            ChatOut.send("chat.biomeCleared", biome.coloredName, TimeFormat.clock(at), who)
            if (biome == ConfigManager.config.critterSafari.selectedBiome) onOwnBiomeCleared(biome)
        }
    }

    /** The biome this player covers also feeds the party message and the per-biome best time. */
    private fun onOwnBiomeCleared(biome: CritterBiome) {
        if (!SafariSession.markBiomeAnnounced(biome)) return
        val duration = SafariSession.clearedAt(biome) ?: SafariSession.elapsedMs

        if (ConfigManager.config.critterSafari.chat.announceBiomeClearedToParty) {
            // Deliberately English and in Hypixel's own wording: the party has to read it.
            ChatOut.runCommand("pc ${biome.clearedPartyMessage}")
        }

        if (!SafariSession.validForPersonalBest) {
            ChatOut.send("chat.biomeClearNotCounted")
            return
        }
        val previousBest = SafariStats.recordBiomeClear(biome, duration)
        when {
            previousBest == null -> ChatOut.send("chat.biomeClearFirstBest")

            duration < previousBest -> ChatOut.send(
                "chat.biomeClearNewBest",
                TimeFormat.clock(previousBest),
                TimeFormat.clock(previousBest - duration),
            )

            else -> ChatOut.send(
                "chat.biomeClearKept",
                TimeFormat.clock(previousBest),
                TimeFormat.clock(duration - previousBest),
            )
        }
    }

    private fun checkCompletion() {
        if (!SafariSession.markCompleted()) return
        val duration = SafariSession.completionMs ?: SafariSession.elapsedMs
        ChatOut.send("chat.complete", CritterBiome.totalCritterCount, TimeFormat.clock(duration))

        // A run that was reset halfway started with capsules already spent, so its time is not
        // comparable and must never become the personal best.
        if (!SafariSession.validForPersonalBest) {
            ChatOut.send("chat.completionNotCounted")
            return
        }
        val previousBest = SafariStats.recordCompletion(duration)
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
