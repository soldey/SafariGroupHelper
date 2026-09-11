package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.util.tr
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen

/** Small picker for the biome this player farms. Opened by the HUD button or `/sgh biome`. */
class BiomeSelectScreen(private val parent: Screen?) : Screen(tr("screen.biomePicker.title")) {

    override fun isPauseScreen(): Boolean = false

    override fun init() {
        val buttonWidth = 160
        val buttonHeight = 20
        val startY = height / 2 - (CritterBiome.entries.size * (buttonHeight + 4)) / 2
        CritterBiome.entries.forEachIndexed { index, biome ->
            val selected = biome == ConfigManager.config.critterSafari.selectedBiome
            // Counters are a Critter Safari thing; outside of it the last run's numbers would
            // just be stale noise, so only the biome name is shown.
            val label = if (SafariSession.progressVisible) {
                tr("screen.biomePicker.option", biome.coloredName, SafariSession.uniques(biome), biome.total)
            } else {
                tr("screen.biomePicker.optionPlain", biome.coloredName)
            }.apply { if (selected) append(tr("screen.biomePicker.selected")) }
            addRenderableWidget(
                Button.builder(label) {
                    ConfigManager.config.critterSafari.selectedBiome = biome
                    ConfigManager.save()
                    rebuildWidgets()
                }.bounds(width / 2 - buttonWidth / 2, startY + index * (buttonHeight + 4), buttonWidth, buttonHeight)
                    .build(),
            )
        }
        addRenderableWidget(
            Button.builder(tr("screen.done")) { onClose() }
                .bounds(width / 2 - buttonWidth / 2, startY + CritterBiome.entries.size * (buttonHeight + 4) + 8, buttonWidth, buttonHeight)
                .build(),
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF.toInt())
    }

    override fun onClose() {
        minecraft.setScreen(parent)
    }
}
