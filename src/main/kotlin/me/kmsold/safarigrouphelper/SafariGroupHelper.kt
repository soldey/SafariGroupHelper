package me.kmsold.safarigrouphelper

import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import me.kmsold.safarigrouphelper.chat.ChatPatterns
import me.kmsold.safarigrouphelper.chat.CritterChatParser
import me.kmsold.safarigrouphelper.compat.HypixelLocationApi
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.SghConfigGui
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.data.SafariStats
import me.kmsold.safarigrouphelper.hud.BiomeSelectScreen
import me.kmsold.safarigrouphelper.hud.HudEditorScreen
import me.kmsold.safarigrouphelper.hud.HudInteractions
import me.kmsold.safarigrouphelper.hud.HudRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object SafariGroupHelper : ClientModInitializer {

    const val MOD_ID = "safarigrouphelper"

    val logger: Logger = LoggerFactory.getLogger("Safari Group Helper")

    /** Screens opened from a command are queued; opening them mid-command fights the chat screen. */
    private var queuedScreen: Screen? = null
    private var tickCounter = 0

    override fun onInitializeClient() {
        ConfigManager.load()
        SafariStats.load()
        SafariSession.load()
        ChatPatterns.load()

        registerChatListeners()
        registerHud()
        registerScreenHooks()
        registerConnectionHooks()
        SghCommands.register()

        ClientTickEvents.END_CLIENT_TICK.register { onTick() }
        logger.info("Safari Group Helper ready")
    }

    fun openScreen(screen: Screen) {
        queuedScreen = screen
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

    private fun registerChatListeners() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) CritterChatParser.onChatMessage(message)
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            CritterChatParser.onChatMessage(message)
        }
    }

    private fun registerHud() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("hud")) { graphics, _ ->
            // While a screen is open we draw through the screen hook instead, on top of it.
            if (Minecraft.getInstance().screen == null) {
                HudRenderer.render(graphics, inInventory = false)
            }
        }
    }

    private fun registerScreenHooks() {
        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            if (screen.isOwnScreen()) return@register

            ScreenEvents.afterExtract(screen).register { currentScreen, graphics, _, _, _ ->
                HudRenderer.render(graphics, inInventory = currentScreen is AbstractContainerScreen<*>)
            }

            ScreenMouseEvents.allowMouseClick(screen).register { currentScreen, event ->
                val action = if (event.button() == 0) HudRenderer.actionAt(event.x(), event.y()) else null
                if (action != null) {
                    HudInteractions.click(action, currentScreen)
                    false
                } else {
                    true
                }
            }
        }
    }

    private fun registerConnectionHooks() {
        val hasModApi = FabricLoader.getInstance().isModLoaded("hypixel-mod-api")
        if (hasModApi) {
            runCatching { HypixelLocationApi.register() }
                .onFailure { logger.error("Could not hook into hypixel-mod-api", it) }
        } else {
            logger.info("hypixel-mod-api is not installed, falling back to the scoreboard")
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            LocationTracker.onDisconnect()
        }
    }

    private fun Screen.isOwnScreen(): Boolean =
        this is HudEditorScreen || this is BiomeSelectScreen || this is MoulConfigScreenComponent

    private fun onTick() {
        queuedScreen?.let {
            queuedScreen = null
            Minecraft.getInstance().setScreen(it)
        }
        if (++tickCounter >= 10) {
            tickCounter = 0
            LocationTracker.tick()
        }
        ConfigManager.tick()
        SafariSession.tick()
        SafariStats.tick()
    }
}
