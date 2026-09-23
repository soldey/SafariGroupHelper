package me.kmsold.safarigrouphelper.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.screens.Screen

/**
 * The handful of client calls that moved between Minecraft versions. 26.2 split `Gui` into a
 * screen half and a `Hud` half, so screens and the chat log are reached differently there; every
 * other API the mod uses is the same, which is why only this file exists per version.
 */
object McCompat {

    val currentScreen: Screen? get() = Minecraft.getInstance().gui.screen()

    fun setScreen(screen: Screen?) = Minecraft.getInstance().gui.setScreen(screen)

    val chat: ChatComponent get() = Minecraft.getInstance().gui.hud.chat
}
