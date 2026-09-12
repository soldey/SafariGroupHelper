package me.kmsold.safarigrouphelper.chat

import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariRunController
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.util.ChatOut
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import kotlin.io.path.appendText

/**
 * Hooks Hypixel chat up to the run tracker. The actual string work lives in [CritterParsing];
 * this object only adds the game-facing bits: who is the local player, are we in the safari,
 * and what to do with a parsed catch.
 */
object CritterChatParser {

    private var lastLine: String = ""
    private var lastLineAt: Long = 0

    fun onChatMessage(message: Component) {
        if (!ConfigManager.config.critterSafari.enabled) return
        val cleaned = CritterParsing.clean(message.string)
        if (cleaned.isEmpty()) return

        // Hypixel occasionally delivers the same line through two different packets.
        val now = System.currentTimeMillis()
        if (cleaned == lastLine && now - lastLineAt < 200) return
        lastLine = cleaned
        lastLineAt = now

        if (handleLocationLines(cleaned)) return

        // A line that names a real critter in a real capture message can only come from inside
        // the safari, so it is trusted even when the location sources have not caught up yet.
        val strict = CritterParsing.parsePatternCatch(cleaned, ChatPatterns.catchPatterns)
        if (strict != null) {
            if (!LocationTracker.inSafari) LocationTracker.onChatEnteredSafari()
            debugLog(cleaned)
            SafariRunController.onCatch(strict.critter, resolvePlayer(strict.player))
            return
        }

        if (ConfigManager.config.dev.parseOnlyInSafari && !LocationTracker.inSafari) return
        debugLog(cleaned)

        val loose = CritterParsing.heuristicCatch(cleaned, ChatPatterns.catchKeywords)
        if (loose == null) {
            reportUnparsed(cleaned)
            return
        }
        SafariRunController.onCatch(loose.critter, resolvePlayer(loose.player))
    }

    /** Entering the safari and the Safari Manager's send-off both start a run. */
    private fun handleLocationLines(cleaned: String): Boolean {
        val enteringPlayer = CritterParsing.parseEnter(cleaned, ChatPatterns.enterSafari)
        if (enteringPlayer != null) {
            if (enteringPlayer.isNotEmpty() && !isLocalPlayer(enteringPlayer)) return true
            LocationTracker.onChatEnteredSafari()
            return true
        }
        if (CritterParsing.matchesAny(cleaned, ChatPatterns.activityStart)) {
            if (!SafariSession.isActive) LocationTracker.onChatEnteredSafari()
            return true
        }
        return false
    }

    private fun resolvePlayer(raw: String?): String? = when {
        raw == null -> null
        raw.equals("You", ignoreCase = true) -> localPlayerName()
        else -> raw
    }

    private fun localPlayerName(): String? = Minecraft.getInstance().player?.gameProfile?.name

    private fun isLocalPlayer(name: String): Boolean = name.equals(localPlayerName(), ignoreCase = true)

    /** Tells the user about lines that look like a catch but that no pattern understood. */
    private fun reportUnparsed(cleaned: String) {
        if (!ConfigManager.config.dev.debugChatParsing) return
        val lower = cleaned.lowercase()
        val looksRelevant = ChatPatterns.catchKeywords.any { lower.contains(it) } ||
            CritterBiome.allCritters.any { cleaned.contains(it, ignoreCase = true) }
        if (!looksRelevant) return
        ChatOut.send("chat.unparsed", cleaned)
        SafariGroupHelper.logger.info("Unparsed safari line: {}", cleaned)
    }

    /** With debug on, every line seen inside the safari is appended to chat-debug.log. */
    private fun debugLog(cleaned: String) {
        if (!ConfigManager.config.dev.debugChatParsing) return
        runCatching { ConfigManager.configDir.resolve("chat-debug.log").appendText("$cleaned\n") }
    }
}
