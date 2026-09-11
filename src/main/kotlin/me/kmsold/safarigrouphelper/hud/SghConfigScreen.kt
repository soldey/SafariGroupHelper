package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.util.text
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** The settings screen, reachable from ModMenu and from `/sgh settings`. */
class SghConfigScreen(private val parent: Screen?) : Screen(text("Safari Group Helper")) {

    private data class Row(val label: () -> Component, val onClick: () -> Unit)

    override fun isPauseScreen(): Boolean = false

    private fun rows(): List<Row> {
        val config = ConfigManager.config
        return listOf(
            Row({ toggle("Mod enabled", config.enabled) }) { config.enabled = !config.enabled },
            Row({
                value("My biome", config.selectedBiome.displayName, config.selectedBiome.formatting)
            }) {
                val next = CritterBiome.entries[(config.selectedBiome.ordinal + 1) % CritterBiome.entries.size]
                config.selectedBiome = next
            },
            Row({ value("Other biomes", config.otherBiomesMode.label) }) {
                config.otherBiomesMode = config.otherBiomesMode.next()
            },
            Row({ value("Biome picker shown", config.biomeSelectVisibility.label) }) {
                config.biomeSelectVisibility = config.biomeSelectVisibility.next()
            },
            Row({ toggle("Picker only in inventory", config.switchButtonOnlyInInventory) }) {
                config.switchButtonOnlyInInventory = !config.switchButtonOnlyInInventory
            },
            Row({ toggle("Parse chat only in Safari", config.parseOnlyInSafari) }) {
                config.parseOnlyInSafari = !config.parseOnlyInSafari
            },
            Row({ toggle("Announce new uniques", config.announceNewUniques) }) {
                config.announceNewUniques = !config.announceNewUniques
            },
            Row({ toggle("HUD background", config.hudBackground) }) {
                config.hudBackground = !config.hudBackground
            },
            Row({ toggle("Debug chat parsing", config.debugChatParsing) }) {
                config.debugChatParsing = !config.debugChatParsing
            },
            Row({ text("Edit HUD positions...", ChatFormatting.YELLOW) }) {
                minecraft.setScreen(HudEditorScreen(this))
            },
            Row({
                text("")
                    .append(text("Reset current run ", ChatFormatting.RED))
                    .append(
                        text(
                            "(${SafariSession.uniqueTotal}/${CritterBiome.totalCritterCount})",
                            ChatFormatting.GRAY,
                        ),
                    )
            }) {
                SafariSession.reset()
                SafariSession.save()
            },
        )
    }

    override fun init() {
        val rows = rows()
        val columnWidth = 200
        val rowHeight = 22
        val columns = 2
        val perColumn = (rows.size + columns - 1) / columns
        val totalWidth = columns * columnWidth + (columns - 1) * 8
        val startX = width / 2 - totalWidth / 2
        val startY = 48

        rows.forEachIndexed { index, row ->
            val column = index / perColumn
            val rowIndex = index % perColumn
            addRenderableWidget(
                Button.builder(row.label()) {
                    row.onClick()
                    ConfigManager.save()
                    rebuildWidgets()
                }.bounds(
                    startX + column * (columnWidth + 8),
                    startY + rowIndex * rowHeight,
                    columnWidth,
                    20,
                ).build(),
            )
        }

        addRenderableWidget(
            Button.builder(text("Done")) { onClose() }
                .bounds(width / 2 - 100, height - 30, 200, 20)
                .build(),
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, 18, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        ConfigManager.save()
        minecraft.setScreen(parent)
    }

    private fun toggle(label: String, enabled: Boolean): Component = text("")
        .append(text("$label: ", ChatFormatting.GRAY))
        .append(
            if (enabled) text("ON", ChatFormatting.GREEN) else text("OFF", ChatFormatting.RED),
        )

    private fun value(label: String, value: String, formatting: ChatFormatting = ChatFormatting.YELLOW): Component =
        text("")
            .append(text("$label: ", ChatFormatting.GRAY))
            .append(text(value, formatting))
}
