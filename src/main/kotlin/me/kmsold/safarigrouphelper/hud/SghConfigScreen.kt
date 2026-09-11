package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.util.text
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Settings screen in the style SkyHanni uses: categories down the left, options on the right,
 * each with a short description of what it actually does.
 */
class SghConfigScreen(private val parent: Screen?) : Screen(text("Safari Group Helper")) {

    private sealed class Entry(val name: String, val description: String) {
        class Toggle(
            name: String,
            description: String,
            val get: () -> Boolean,
            val set: (Boolean) -> Unit,
        ) : Entry(name, description)

        class Cycle(
            name: String,
            description: String,
            val value: () -> Component,
            val cycle: () -> Unit,
        ) : Entry(name, description)

        class Action(
            name: String,
            description: String,
            val buttonLabel: String,
            val run: () -> Unit,
        ) : Entry(name, description)
    }

    private class Category(val title: String, val entries: List<Entry>)

    private var selectedCategory = 0
    private var scroll = 0
    private var maxScroll = 0

    /** Control bounds of the currently visible rows, rebuilt every frame. */
    private val hitboxes = ArrayList<Pair<Rect, Entry>>()
    private var doneBounds: Rect? = null

    override fun isPauseScreen(): Boolean = false

    // region layout
    private val sidebarX get() = 12
    private val sidebarWidth get() = 112
    private val contentX get() = sidebarX + sidebarWidth + 10
    private val contentWidth get() = width - contentX - 22
    private val topY get() = 34
    private val bottomY get() = height - 34
    private val controlWidth get() = 104
    private val rowPadding = 5
    // endregion

    private fun categories(): List<Category> {
        val config = ConfigManager.config
        return listOf(
            Category(
                "General",
                listOf(
                    Entry.Toggle(
                        "Mod enabled",
                        "Master switch. Turns off the HUD and all chat parsing.",
                        { config.enabled },
                        { config.enabled = it },
                    ),
                    Entry.Cycle(
                        "My biome",
                        "The biome you cover for the group. Its critter list gets the detailed HUD block, " +
                            "the other three are summarised separately.",
                        {
                            text(config.selectedBiome.displayName, config.selectedBiome.formatting)
                        },
                        {
                            val next = CritterBiome.entries[
                                (config.selectedBiome.ordinal + 1) % CritterBiome.entries.size,
                            ]
                            config.selectedBiome = next
                        },
                    ),
                    Entry.Toggle(
                        "Announce new uniques",
                        "Prints one line in your own chat whenever a critter is caught for the first " +
                            "time this run, with the biome and the running total.",
                        { config.announceNewUniques },
                        { config.announceNewUniques = it },
                    ),
                ),
            ),
            Category(
                "HUD",
                listOf(
                    Entry.Cycle(
                        "Other biomes",
                        "How much of the other three biomes to show: nothing at all, just the counters " +
                            "like 3/9, or the full critter list.",
                        { text(config.otherBiomesMode.label, ChatFormatting.YELLOW) },
                        { config.otherBiomesMode = config.otherBiomesMode.next() },
                    ),
                    Entry.Cycle(
                        "Biome picker shown",
                        "Where the biome name and the [Switch biome] button are allowed to appear. " +
                            "Critter progress itself is always Safari only.",
                        { text(config.biomeSelectVisibility.label, ChatFormatting.YELLOW) },
                        { config.biomeSelectVisibility = config.biomeSelectVisibility.next() },
                    ),
                    Entry.Toggle(
                        "Buttons only in inventory",
                        "Hides [Switch biome] and [Reset run] unless an inventory or chest is open, " +
                            "so you cannot hit them by accident while hunting.",
                        { config.switchButtonOnlyInInventory },
                        { config.switchButtonOnlyInInventory = it },
                    ),
                    Entry.Toggle(
                        "HUD background",
                        "Draws a translucent black box behind every HUD block.",
                        { config.hudBackground },
                        { config.hudBackground = it },
                    ),
                    Entry.Action(
                        "HUD positions",
                        "Drag blocks with the mouse, scroll over one to resize it, right click to hide " +
                            "it, press R to reset every position.",
                        "Edit...",
                        { minecraft.setScreen(HudEditorScreen(this)) },
                    ),
                ),
            ),
            Category(
                "Chat",
                listOf(
                    Entry.Toggle(
                        "Parse chat only in Safari",
                        "Skips all chat parsing outside the Critter Safari. Keep this on unless you " +
                            "are hunting down a parsing problem.",
                        { config.parseOnlyInSafari },
                        { config.parseOnlyInSafari = it },
                    ),
                    Entry.Toggle(
                        "Debug chat parsing",
                        "Writes every chat line seen inside the safari to chat-debug.log and highlights " +
                            "lines that look like a catch but were not understood.",
                        { config.debugChatParsing },
                        { config.debugChatParsing = it },
                    ),
                ),
            ),
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        hitboxes.clear()

        val categories = categories()
        selectedCategory = selectedCategory.coerceIn(0, categories.size - 1)

        graphics.centeredText(font, title, width / 2, 14, WHITE)

        drawSidebar(graphics, categories, mouseX, mouseY)
        drawOptions(graphics, categories[selectedCategory], mouseX, mouseY)
        drawDone(graphics, mouseX, mouseY)
    }

    private fun drawSidebar(
        graphics: GuiGraphicsExtractor,
        categories: List<Category>,
        mouseX: Int,
        mouseY: Int,
    ) {
        graphics.fill(sidebarX, topY, sidebarX + sidebarWidth, bottomY, PANEL)
        var y = topY + 4
        categories.forEachIndexed { index, category ->
            val rect = Rect(sidebarX.toFloat() + 3, y.toFloat(), sidebarWidth.toFloat() - 6, ROW_HEIGHT.toFloat())
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            val selected = index == selectedCategory
            if (selected || hovered) {
                graphics.fill(
                    rect.x.toInt(),
                    rect.y.toInt(),
                    (rect.x + rect.width).toInt(),
                    (rect.y + rect.height).toInt(),
                    if (selected) SELECTED else HOVER,
                )
            }
            graphics.text(
                font,
                text(category.title, if (selected) ChatFormatting.YELLOW else ChatFormatting.GRAY),
                rect.x.toInt() + 6,
                rect.y.toInt() + (ROW_HEIGHT - font.lineHeight) / 2,
                WHITE,
                false,
            )
            y += ROW_HEIGHT + 2
        }
    }

    private fun drawOptions(graphics: GuiGraphicsExtractor, category: Category, mouseX: Int, mouseY: Int) {
        graphics.fill(contentX, topY, contentX + contentWidth, bottomY, PANEL)

        val textWidth = contentWidth - controlWidth - 24
        val heights = category.entries.map { rowHeight(it, textWidth) }
        val total = heights.sum()
        val viewport = bottomY - topY
        maxScroll = (total - viewport + 8).coerceAtLeast(0)
        scroll = scroll.coerceIn(0, maxScroll)

        graphics.enableScissor(contentX, topY, contentX + contentWidth, bottomY)
        var y = topY + 4 - scroll
        category.entries.forEachIndexed { index, entry ->
            val height = heights[index]
            if (y + height >= topY && y <= bottomY) {
                drawRow(graphics, entry, y, height, textWidth, mouseX, mouseY)
            }
            y += height
        }
        graphics.disableScissor()

        if (maxScroll > 0) {
            val barX = contentX + contentWidth + 4
            graphics.fill(barX, topY, barX + 4, bottomY, PANEL)
            val barHeight = (viewport * viewport / (total + 8)).coerceAtLeast(16)
            val barY = topY + (viewport - barHeight) * scroll / maxScroll
            graphics.fill(barX, barY, barX + 4, barY + barHeight, SELECTED)
        }
    }

    private fun rowHeight(entry: Entry, textWidth: Int): Int {
        val descriptionLines = font.split(text(entry.description), textWidth).size
        return rowPadding * 2 + font.lineHeight + 2 + descriptionLines * (font.lineHeight - 1) + 4
    }

    private fun drawRow(
        graphics: GuiGraphicsExtractor,
        entry: Entry,
        y: Int,
        height: Int,
        textWidth: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        val x = contentX + 8
        graphics.fill(contentX + 2, y, contentX + contentWidth - 2, y + height - 2, ROW_BACKGROUND)

        graphics.text(font, text(entry.name, ChatFormatting.WHITE), x, y + rowPadding, WHITE, false)
        var descriptionY = y + rowPadding + font.lineHeight + 2
        for (line in font.split(text(entry.description, ChatFormatting.GRAY), textWidth)) {
            graphics.text(font, line, x, descriptionY, WHITE, false)
            descriptionY += font.lineHeight - 1
        }

        val control = Rect(
            (contentX + contentWidth - controlWidth - 8).toFloat(),
            (y + rowPadding).toFloat(),
            controlWidth.toFloat(),
            CONTROL_HEIGHT.toFloat(),
        )
        val hovered = control.contains(mouseX.toDouble(), mouseY.toDouble())
        graphics.fill(
            control.x.toInt(),
            control.y.toInt(),
            (control.x + control.width).toInt(),
            (control.y + control.height).toInt(),
            if (hovered) CONTROL_HOVER else CONTROL,
        )
        graphics.outline(control.x.toInt(), control.y.toInt(), controlWidth, CONTROL_HEIGHT, OUTLINE)

        val label = when (entry) {
            is Entry.Toggle -> if (entry.get()) text("ON", ChatFormatting.GREEN) else text("OFF", ChatFormatting.RED)
            is Entry.Cycle -> entry.value()
            is Entry.Action -> text(entry.buttonLabel, ChatFormatting.YELLOW)
        }
        graphics.centeredText(
            font,
            label,
            (control.x + control.width / 2).toInt(),
            control.y.toInt() + (CONTROL_HEIGHT - font.lineHeight) / 2 + 1,
            WHITE,
        )
        // Rows scrolled half out of view must not stay clickable outside the panel.
        if (control.y >= topY && control.y + control.height <= bottomY) {
            hitboxes += control to entry
        }
    }

    private fun drawDone(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val rect = Rect((width / 2 - 60).toFloat(), (height - 26).toFloat(), 120f, 20f)
        doneBounds = rect
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        graphics.fill(
            rect.x.toInt(),
            rect.y.toInt(),
            (rect.x + rect.width).toInt(),
            (rect.y + rect.height).toInt(),
            if (hovered) CONTROL_HOVER else CONTROL,
        )
        graphics.outline(rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt(), OUTLINE)
        graphics.centeredText(
            font,
            text("Done"),
            (rect.x + rect.width / 2).toInt(),
            rect.y.toInt() + (20 - font.lineHeight) / 2 + 1,
            WHITE,
        )
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick)
        val mouseX = event.x()
        val mouseY = event.y()

        doneBounds?.let { if (it.contains(mouseX, mouseY)) { onClose(); return true } }

        val categories = categories()
        var y = topY + 4
        for (index in categories.indices) {
            val rect = Rect(sidebarX.toFloat() + 3, y.toFloat(), sidebarWidth.toFloat() - 6, ROW_HEIGHT.toFloat())
            if (rect.contains(mouseX, mouseY)) {
                selectedCategory = index
                scroll = 0
                return true
            }
            y += ROW_HEIGHT + 2
        }

        val hit = hitboxes.firstOrNull { it.first.contains(mouseX, mouseY) }?.second
        if (hit != null) {
            when (hit) {
                is Entry.Toggle -> hit.set(!hit.get())
                is Entry.Cycle -> hit.cycle()
                is Entry.Action -> hit.run()
            }
            ConfigManager.save()
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (mouseX < contentX || mouseX > contentX + contentWidth) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        }
        scroll = (scroll - (scrollY * 14).toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun onClose() {
        ConfigManager.save()
        minecraft.setScreen(parent)
    }

    private companion object {
        const val ROW_HEIGHT = 18
        const val CONTROL_HEIGHT = 16

        const val WHITE = 0xFFFFFFFF.toInt()
        const val PANEL = 0x70000000
        const val ROW_BACKGROUND = 0x30000000
        const val SELECTED = 0x60FFFF55
        const val HOVER = 0x30FFFFFF
        const val CONTROL = 0x80000000.toInt()
        const val CONTROL_HOVER = 0x80555555.toInt()
        const val OUTLINE = 0x60FFFFFF
    }
}
