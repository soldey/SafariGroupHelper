package me.kmsold.safarigrouphelper.util

import me.kmsold.safarigrouphelper.l10n.Localization
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

fun text(value: String): MutableComponent = Component.literal(value)

fun text(value: String, vararg formatting: ChatFormatting): MutableComponent =
    Component.literal(value).withStyle(*formatting)

/** A translated line. Values in the language files carry their own `§` colour codes. */
fun tr(key: String, vararg args: Any?): MutableComponent = Component.literal(Localization.tr(key, *args))

/** Client-side only chat output; never reaches the server or other players. */
object ChatOut {

    fun send(message: Component) {
        val chat = Minecraft.getInstance().gui.chat
        chat.addClientSystemMessage(text(Localization.tr("chat.prefix")).append(message))
    }

    fun send(key: String, vararg args: Any?) = send(tr(key, *args))

    /**
     * Runs a command as if the player typed it. Unlike [send] this leaves the client, so it is
     * only ever called for features the player switched on.
     */
    fun runCommand(command: String) {
        val connection = Minecraft.getInstance().player?.connection ?: return
        connection.sendCommand(command)
    }
}

object TimeFormat {

    /** mm:ss, or h:mm:ss once the run passes an hour. */
    fun clock(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }
}
