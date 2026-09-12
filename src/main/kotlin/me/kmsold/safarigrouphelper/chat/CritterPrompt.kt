package me.kmsold.safarigrouphelper.chat

import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.util.ChatOut
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component

/**
 * Answers the critter prompts Hypixel puts in chat:
 *
 * ```
 * [MOB] Hideyho: How about it?
 * Select an option: [Sure] [No thanks...]
 * ```
 *
 * `[Sure]` is a normal clickable chat component, so accepting is a matter of finding it in the
 * message and running the command it carries - exactly what clicking it would do.
 */
object CritterPrompt {

    /** The wording of the accepting option. Kept in one place in case Hypixel words it differently. */
    private val ACCEPT_LABELS = setOf("sure", "yes", "ok", "okay")

    /** One prompt can arrive twice; the same answer is not sent again straight away. */
    private const val REPEAT_WINDOW_MS = 3000L

    private var lastCommand: String? = null
    private var lastCommandAt = 0L

    /** @return true when the prompt was answered, so the line needs no further parsing. */
    fun handle(message: Component): Boolean {
        if (!ConfigManager.config.critterSafari.chat.autoAcceptPrompts) return false
        if (!LocationTracker.inSafari) return false
        if (!message.string.contains("Select an option", ignoreCase = true)) return false

        val command = findAcceptCommand(message) ?: return false
        val now = System.currentTimeMillis()
        if (command == lastCommand && now - lastCommandAt < REPEAT_WINDOW_MS) return true
        lastCommand = command
        lastCommandAt = now

        SafariGroupHelper.logger.info("Answering a critter prompt with /{}", command)
        ChatOut.runCommand(command)
        ChatOut.send("chat.promptAccepted")
        return true
    }

    /** Walks the message tree looking for the accepting option and the command behind it. */
    private fun findAcceptCommand(component: Component): String? {
        val label = component.string.trim().trim('[', ']').lowercase()
        if (label in ACCEPT_LABELS) {
            val click = component.style.clickEvent
            if (click is ClickEvent.RunCommand) return click.command().removePrefix("/")
        }
        for (sibling in component.siblings) {
            findAcceptCommand(sibling)?.let { return it }
        }
        return null
    }
}
