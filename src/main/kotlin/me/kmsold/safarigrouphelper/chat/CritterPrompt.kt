package me.kmsold.safarigrouphelper.chat

import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.util.ChatOut
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.appendText

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
 *
 * With debug chat parsing on, every prompt-looking message is written to `prompt-debug.log`
 * together with its whole component tree and the decision taken, which is what to send when the
 * button is not being pressed.
 */
object CritterPrompt {

    /** The wording of the accepting option. */
    private val ACCEPT_LABELS = setOf("sure", "yes", "ok", "okay", "accept")

    /** Anything that smells like a prompt gets logged, even when the feature is off. */
    private val PROMPT_HINTS = listOf("select an option", "sure", "no thanks")

    /** One prompt can arrive twice; the same answer is not sent again straight away. */
    private const val REPEAT_WINDOW_MS = 3000L

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

    private var lastCommand: String? = null
    private var lastCommandAt = 0L

    /** One clickable piece of a message. */
    private data class Clickable(val text: String, val click: ClickEvent)

    /** @return true when the prompt was answered, so the line needs no further parsing. */
    fun handle(message: Component): Boolean {
        val plain = message.string
        val looksLikePrompt = PROMPT_HINTS.any { plain.contains(it, ignoreCase = true) }
        if (!looksLikePrompt) return false

        val clickables = ArrayList<Clickable>()
        collectClickables(message, null, clickables)
        val accept = pickAccept(clickables)

        val enabled = ConfigManager.config.critterSafari.chat.autoAcceptPrompts
        val inSafari = LocationTracker.inSafari
        val command = (accept?.click as? ClickEvent.RunCommand)?.command()?.removePrefix("/")

        val decision = when {
            !enabled -> "skipped: 'Auto-accept prompts' is off"
            !inSafari -> "skipped: not inside the safari"
            accept == null -> "skipped: no clickable option matched $ACCEPT_LABELS"
            command == null -> "skipped: the option carries ${accept.click::class.java.simpleName}, not RunCommand"
            command == lastCommand && System.currentTimeMillis() - lastCommandAt < REPEAT_WINDOW_MS ->
                "skipped: the same command was just sent"
            else -> "sent: /$command"
        }
        debugDump(plain, clickables, decision)

        if (!decision.startsWith("sent")) return false
        lastCommand = command
        lastCommandAt = System.currentTimeMillis()
        SafariGroupHelper.logger.info("Answering a critter prompt with /{}", command)
        ChatOut.runCommand(command!!)
        ChatOut.send("chat.promptAccepted")
        return true
    }

    /**
     * Every node that can be clicked, with the text it shows. A click event set on a wrapper is
     * inherited by its children, which is how Hypixel usually builds these buttons.
     */
    private fun collectClickables(component: Component, inherited: ClickEvent?, into: MutableList<Clickable>) {
        val click = component.style.clickEvent ?: inherited
        val text = component.string.trim()
        if (click != null && text.isNotEmpty()) into += Clickable(text, click)
        for (sibling in component.siblings) collectClickables(sibling, click, into)
    }

    /** Prefers an exact label, then anything short that merely contains one. */
    private fun pickAccept(clickables: List<Clickable>): Clickable? {
        clickables.firstOrNull { normalise(it.text) in ACCEPT_LABELS }?.let { return it }
        return clickables.firstOrNull { candidate ->
            val text = normalise(candidate.text)
            text.length <= 16 && ACCEPT_LABELS.any { text.contains(it) }
        }
    }

    private fun normalise(text: String): String = text.trim().trim('[', ']', '.', '!').trim().lowercase()

    private fun debugDump(plain: String, clickables: List<Clickable>, decision: String) {
        if (!ConfigManager.config.dev.debugChatParsing) return
        val report = buildString {
            appendLine("=== ${LocalTime.now().format(timeFormat)} prompt ===")
            appendLine("raw     : $plain")
            appendLine("enabled : ${ConfigManager.config.critterSafari.chat.autoAcceptPrompts}")
            appendLine("inSafari: ${LocationTracker.inSafari}")
            if (clickables.isEmpty()) {
                appendLine("clickable: none found in this message")
            } else {
                clickables.forEachIndexed { index, clickable ->
                    appendLine("clickable[$index]: text='${clickable.text}' ${describe(clickable.click)}")
                }
            }
            appendLine("decision: $decision")
        }
        SafariGroupHelper.logger.info("Critter prompt:\n{}", report)
        runCatching {
            ConfigManager.configDir.resolve("prompt-debug.log").appendText(report)
        }
    }

    private fun describe(click: ClickEvent): String = when (click) {
        is ClickEvent.RunCommand -> "RunCommand('${click.command()}')"
        is ClickEvent.SuggestCommand -> "SuggestCommand('${click.command()}')"
        else -> "${click::class.java.simpleName}(${click.action()})"
    }
}
