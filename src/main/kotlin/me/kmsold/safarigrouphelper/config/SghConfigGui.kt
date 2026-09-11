package me.kmsold.safarigrouphelper.config

import io.github.notenoughupdates.moulconfig.gui.CloseEventListener
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * The MoulConfig powered settings screen - the same library SkyHanni's config is built on, so the
 * categories, search bar and per-option descriptions look and behave the same way.
 */
object SghConfigGui {

    private var editor: MoulConfigEditor<SghConfig>? = null

    /** Built lazily, and thrown away whenever the config object is replaced. */
    private fun editorInstance(): MoulConfigEditor<SghConfig> = editor ?: build().also { editor = it }

    private fun build(): MoulConfigEditor<SghConfig> {
        val processor = LocalizedConfigProcessor(ConfigManager.config)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(ConfigManager.config)
        return MoulConfigEditor(processor)
    }

    fun invalidate() {
        editor = null
    }

    fun createScreen(parent: Screen? = null): Screen = MoulConfigScreenComponent(
        Component.empty(),
        GuiContext(EditorRoot(editorInstance())),
        parent,
    )

    /** Fills the whole screen with the editor and forwards input and close events to it. */
    private class EditorRoot(private val editor: MoulConfigEditor<*>) : GuiComponent(), CloseEventListener {

        override fun getWidth(): Int = Minecraft.getInstance().window.guiScaledWidth

        override fun getHeight(): Int = Minecraft.getInstance().window.guiScaledHeight

        override fun render(context: GuiImmediateContext) = editor.render()

        override fun mouseEvent(mouseEvent: MouseEvent, context: GuiImmediateContext): Boolean =
            editor.mouseInput(context.mouseX, context.mouseY, mouseEvent)

        override fun keyboardEvent(event: KeyboardEvent, context: GuiImmediateContext): Boolean =
            editor.keyboardInput(event)

        override fun onBeforeClose(): CloseEventListener.CloseAction = editor.onBeforeClose()

        override fun onAfterClose() {
            editor.onAfterClose()
            ConfigManager.save()
        }
    }
}
