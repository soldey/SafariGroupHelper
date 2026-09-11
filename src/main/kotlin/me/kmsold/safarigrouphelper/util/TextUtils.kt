package me.kmsold.safarigrouphelper.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

fun text(value: String): MutableComponent = Component.literal(value)

fun text(value: String, vararg formatting: ChatFormatting): MutableComponent =
    Component.literal(value).withStyle(*formatting)

/** Client-side only chat output; never reaches the server or other players. */
object ChatOut {

    private val prefix: Component = text("[SGH] ", ChatFormatting.DARK_AQUA)

    fun send(message: Component) {
        val chat = Minecraft.getInstance().gui.chat
        chat.addClientSystemMessage(text("").append(prefix).append(message))
    }

    fun send(message: String, vararg formatting: ChatFormatting) = send(text(message, *formatting))
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
